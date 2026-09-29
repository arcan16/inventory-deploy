package com.inventories.infr.services;

import com.inventories.dto.inventories.InventoryUploadResultDTO;
import com.inventories.models.InventoriesEntity;
import com.inventories.models.ProductPresentationsEntity;
import com.inventories.models.ProductsEntity;
import com.inventories.models.StockEntity;
import com.inventories.models.UserEntity;
import com.inventories.models.enums.ProductPresentation;
import com.inventories.repositories.InventoriesRepository;
import com.inventories.repositories.ProductPresentationsRepository;
import com.inventories.repositories.ProductsRepository;
import com.inventories.repositories.StockRepository;
import com.inventories.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Puerto a Java de CargaDeArchivos/modules/db_connect.py: crea el inventario,
 * da de alta los productos nuevos (ignorando los que ya existen), registra el
 * stock leido del archivo y la presentacion de cada producto con su codigo de
 * barras (product_presentations). Usa los repositorios JPA del proyecto, que ya
 * obtienen la conexion a la base de datos desde las variables de entorno
 * declaradas en application.properties (no credenciales hardcodeadas).
 */
@Service
public class InventoryCsvLoaderService {

    @Autowired
    private InventoryCsvParser inventoryCsvParser;

    @Autowired
    private InventoriesRepository inventoriesRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ProductPresentationsRepository productPresentationsRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public InventoryUploadResultDTO loadInventory(MultipartFile file) throws IOException {
        InventoryCsvParser.ParsedInventoryFile parsed = inventoryCsvParser.parse(file);

        UserEntity actingUser = resolveActingUser();

        InventoriesEntity inventory = new InventoriesEntity();
        inventory.setInventoryDate(new Date(System.currentTimeMillis()));
        inventory.setPresentation(parsed.presentation());
        inventory.setCreatedBy(actingUser);
        inventory.setCreatedBySnapshot(actingUser != null ? actingUser.getUsuario() : null);
        inventoriesRepository.save(inventory);

        Set<String> incomingProductIds = new HashSet<>();
        Map<String, String> descriptionByProductId = new HashMap<>();
        for (InventoryCsvParser.ProductRow row : parsed.rows()) {
            incomingProductIds.add(row.productId());
            descriptionByProductId.putIfAbsent(row.productId(), row.description());
        }

        Set<String> existingProductIds = new HashSet<>();
        for (ProductsEntity existing : productsRepository.findAllById(incomingProductIds)) {
            existingProductIds.add(existing.getId());
        }

        List<ProductsEntity> newProducts = new ArrayList<>();
        for (String productId : incomingProductIds) {
            if (!existingProductIds.contains(productId)) {
                newProducts.add(new ProductsEntity(productId, descriptionByProductId.get(productId)));
            }
        }
        if (!newProducts.isEmpty()) {
            productsRepository.saveAll(newProducts);
        }

        Map<String, ProductsEntity> productById = new HashMap<>();
        for (ProductsEntity product : productsRepository.findAllById(incomingProductIds)) {
            productById.put(product.getId(), product);
        }

        List<StockEntity> stockRows = new ArrayList<>();
        for (InventoryCsvParser.ProductRow row : parsed.rows()) {
            StockEntity stock = new StockEntity();
            stock.setIdProduct(productById.get(row.productId()));
            stock.setStock(row.stock());
            stock.setIdInventory(inventory);
            stockRows.add(stock);
        }
        stockRepository.saveAll(stockRows);

        PresentationsSummary presentations = registerPresentations(parsed.rows(), productById);

        return new InventoryUploadResultDTO(inventory.getId(), inventory.getPresentation(),
                parsed.rows().size(), newProducts.size(), stockRows.size(),
                presentations.created(), presentations.barcodesAssigned(),
                presentations.skipped(), presentations.barcodeConflicts());
    }

    private record PresentationsSummary(int created, int barcodesAssigned, int skipped, int barcodeConflicts) {
    }

    /**
     * Da de alta en product_presentations la presentacion (UNIDAD) de cada fila
     * y le asigna el COD BARRAS cuando el archivo lo trae:
     * - Si el producto aun no tiene esa presentacion, se crea.
     * - Se compara el codigo almacenado de esa presentacion con el del archivo:
     *   si la presentacion no tiene codigo y la fila si, se actualiza el
     *   registro con el del archivo. Si ya tiene uno, se conserva aunque el
     *   archivo traiga otro distinto.
     * - Si el codigo ya pertenece a otra presentacion no se asigna (la columna
     *   barcode es UNIQUE) y se cuenta como conflicto en lugar de abortar la carga.
     * - Las filas con una UNIDAD que no es una presentacion conocida se omiten
     *   aqui, pero su producto y su stock ya quedaron registrados.
     */
    private PresentationsSummary registerPresentations(List<InventoryCsvParser.ProductRow> rows,
                                                       Map<String, ProductsEntity> productById) {
        Map<String, ProductPresentationsEntity> presentationByKey = new HashMap<>();
        for (ProductPresentationsEntity existing : productPresentationsRepository.findByProductIds(productById.keySet())) {
            presentationByKey.put(presentationKey(existing.getIdProduct().getId(), existing.getPresentation()), existing);
        }

        Set<String> incomingBarcodes = new HashSet<>();
        for (InventoryCsvParser.ProductRow row : rows) {
            if (row.barcode() != null) {
                incomingBarcodes.add(row.barcode());
            }
        }
        Map<String, ProductPresentationsEntity> ownerByBarcode = new HashMap<>();
        if (!incomingBarcodes.isEmpty()) {
            for (ProductPresentationsEntity owner : productPresentationsRepository.findByBarcodes(incomingBarcodes)) {
                ownerByBarcode.put(owner.getBarcode(), owner);
            }
        }

        // Por identidad: @Data calcula hashCode con campos que aqui se modifican (barcode).
        Set<ProductPresentationsEntity> toSave = Collections.newSetFromMap(new IdentityHashMap<>());
        int created = 0;
        int barcodesAssigned = 0;
        int skipped = 0;
        int barcodeConflicts = 0;

        for (InventoryCsvParser.ProductRow row : rows) {
            Optional<ProductPresentation> presentation = ProductPresentation.findByLabel(row.unit());
            if (presentation.isEmpty()) {
                skipped++;
                continue;
            }

            String key = presentationKey(row.productId(), presentation.get());
            ProductPresentationsEntity productPresentation = presentationByKey.get(key);
            if (productPresentation == null) {
                productPresentation = new ProductPresentationsEntity(null, productById.get(row.productId()),
                        presentation.get(), null);
                presentationByKey.put(key, productPresentation);
                toSave.add(productPresentation);
                created++;
            }

            // Solo se completa el codigo de las presentaciones que aun no tienen uno;
            // un codigo ya almacenado nunca se sobreescribe desde el archivo.
            String barcode = row.barcode();
            if (barcode == null || productPresentation.getBarcode() != null) {
                continue;
            }
            ProductPresentationsEntity owner = ownerByBarcode.get(barcode);
            if (owner != null) {
                barcodeConflicts++;
                continue;
            }

            productPresentation.setBarcode(barcode);
            ownerByBarcode.put(barcode, productPresentation);
            toSave.add(productPresentation);
            barcodesAssigned++;
        }

        if (!toSave.isEmpty()) {
            productPresentationsRepository.saveAll(toSave);
        }
        return new PresentationsSummary(created, barcodesAssigned, skipped, barcodeConflicts);
    }

    private static String presentationKey(String productId, ProductPresentation presentation) {
        return productId + "|" + presentation.name();
    }

    private UserEntity resolveActingUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        return userRepository.findByUsuario(authentication.getName()).orElse(null);
    }
}
