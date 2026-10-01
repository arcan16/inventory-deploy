package com.inventories.infr.services;

import com.inventories.dto.backup.BackupDTO;
import com.inventories.dto.backup.ImportResultDTO;
import com.inventories.models.InventoriesEntity;
import com.inventories.models.ProductCountsEntity;
import com.inventories.models.ProductPresentationsEntity;
import com.inventories.models.ProductsEntity;
import com.inventories.models.StockEntity;
import com.inventories.models.UserEntity;
import com.inventories.models.enums.InventoryStatus;
import com.inventories.models.enums.ProductLocation;
import com.inventories.models.enums.ProductPresentation;
import com.inventories.repositories.InventoriesRepository;
import com.inventories.repositories.ProductCountsRepository;
import com.inventories.repositories.ProductPresentationsRepository;
import com.inventories.repositories.ProductsRepository;
import com.inventories.repositories.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exporta e importa el archivo de respaldo (BackupDTO) que comparten el
 * backend, la app cliente y la app local ComexBeta.
 *
 * Importar combina con lo que ya existe y nunca borra:
 * - Productos: se crean los que faltan; en los existentes se actualiza la descripcion.
 * - Presentaciones: se crean las que faltan; el codigo de barras del archivo
 *   reemplaza al guardado, salvo que ya pertenezca a otra presentacion (conflicto).
 * - Inventarios: siempre se crean nuevos, con su stock y sus conteos (importar
 *   dos veces el mismo archivo los duplica).
 * Los renglones que no caben en las columnas de MySQL (p. ej. id de producto de
 * mas de 10 caracteres, cantidades de 1000 o mas) se omiten en lugar de abortar.
 */
@Service
public class DataTransferService {

    // Limites de las columnas (ver db/migration/V1__Create_Tables.sql).
    private static final int MAX_PRODUCT_ID = 10;
    private static final int MAX_DESCRIPTION = 50;
    private static final int MAX_BARCODE = 64;
    private static final int MAX_INVENTORY_PRESENTATION = 10;
    /** decimal(6,3): hasta 999.999. */
    private static final float MAX_QUANTITY = 999.999f;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private ProductPresentationsRepository productPresentationsRepository;

    @Autowired
    private InventoriesRepository inventoriesRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ProductCountsRepository productCountsRepository;

    @Transactional(readOnly = true)
    public BackupDTO exportBackup() {
        List<BackupDTO.Product> products = productsRepository.findAll(Sort.by("id")).stream()
                .map(p -> new BackupDTO.Product(p.getId(), p.getDescription()))
                .toList();

        List<BackupDTO.Presentation> presentations = productPresentationsRepository.findAll().stream()
                .sorted(Comparator.comparing((ProductPresentationsEntity pp) -> pp.getIdProduct().getId())
                        .thenComparing(ProductPresentationsEntity::getPresentation))
                .map(pp -> new BackupDTO.Presentation(pp.getIdProduct().getId(), pp.getPresentation().getLabel(), pp.getBarcode()))
                .toList();

        List<BackupDTO.Inventory> inventories = new ArrayList<>();
        for (InventoriesEntity inventory : inventoriesRepository.findAll(Sort.by("id"))) {
            List<BackupDTO.StockRow> stock = stockRepository.findByIdInventory(inventory.getId()).stream()
                    .filter(s -> s.getIdProduct() != null)
                    .sorted(Comparator.comparing(StockEntity::getId))
                    .map(s -> new BackupDTO.StockRow(s.getIdProduct().getId(), s.getStock()))
                    .toList();
            List<BackupDTO.CountRow> counts = productCountsRepository.getInventoryCounts(inventory.getId()).stream()
                    .sorted(Comparator.comparing(ProductCountsEntity::getId))
                    .map(c -> new BackupDTO.CountRow(c.getIdProduct().getId(), c.getQuantity(), c.getPlace().name()))
                    .toList();
            inventories.add(new BackupDTO.Inventory(inventory.getId(), inventory.getInventoryDate().toString(),
                    inventory.getPresentation(), inventory.getStatus().name(), stock, counts));
        }

        String exportedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return new BackupDTO(BackupDTO.FORMAT, BackupDTO.VERSION, exportedAt, products, presentations, inventories);
    }

    /** IllegalArgumentException si el archivo no es un respaldo valido. */
    @Transactional
    public ImportResultDTO importBackup(BackupDTO backup, UserEntity actingUser) {
        if (backup == null || !BackupDTO.FORMAT.equals(backup.format())) {
            throw new IllegalArgumentException("El archivo no es un respaldo de inventarios");
        }
        if (backup.version() < 1 || backup.version() > BackupDTO.VERSION) {
            throw new IllegalArgumentException("Version de respaldo no soportada: " + backup.version());
        }

        Counters counters = new Counters();
        Map<String, ProductsEntity> productById = importProducts(nullSafe(backup.products()), counters);
        importPresentations(nullSafe(backup.productPresentations()), productById, counters);
        for (BackupDTO.Inventory inventory : nullSafe(backup.inventories())) {
            importInventory(inventory, productById, actingUser, counters);
        }

        return new ImportResultDTO(counters.productsCreated, counters.productsUpdated, counters.presentationsCreated,
                counters.barcodesAssigned, counters.barcodeConflicts, counters.inventoriesCreated,
                counters.stockRowsCreated, counters.countsCreated, counters.rowsSkipped);
    }

    private static final class Counters {
        int productsCreated;
        int productsUpdated;
        int presentationsCreated;
        int barcodesAssigned;
        int barcodeConflicts;
        int inventoriesCreated;
        int stockRowsCreated;
        int countsCreated;
        int rowsSkipped;
    }

    /** Regresa todos los productos que existen al terminar (los del archivo y los que ya estaban). */
    private Map<String, ProductsEntity> importProducts(List<BackupDTO.Product> products, Counters counters) {
        Map<String, ProductsEntity> productById = new HashMap<>();
        for (ProductsEntity existing : productsRepository.findAll()) {
            productById.put(existing.getId(), existing);
        }

        List<ProductsEntity> toSave = new ArrayList<>();
        for (BackupDTO.Product product : products) {
            String id = trim(product.id());
            String description = trim(product.description());
            if (id == null || description == null || id.length() > MAX_PRODUCT_ID || description.length() > MAX_DESCRIPTION) {
                counters.rowsSkipped++;
                continue;
            }
            ProductsEntity existing = productById.get(id);
            if (existing == null) {
                ProductsEntity created = new ProductsEntity(id, description);
                productById.put(id, created);
                toSave.add(created);
                counters.productsCreated++;
            } else if (!description.equals(existing.getDescription())) {
                existing.setDescription(description);
                toSave.add(existing);
                counters.productsUpdated++;
            }
        }
        productsRepository.saveAll(toSave);
        return productById;
    }

    private void importPresentations(List<BackupDTO.Presentation> presentations, Map<String, ProductsEntity> productById,
                                     Counters counters) {
        Map<String, ProductPresentationsEntity> byKey = new HashMap<>();
        Map<String, ProductPresentationsEntity> byBarcode = new HashMap<>();
        for (ProductPresentationsEntity existing : productPresentationsRepository.findAll()) {
            byKey.put(key(existing.getIdProduct().getId(), existing.getPresentation()), existing);
            if (existing.getBarcode() != null) {
                byBarcode.put(existing.getBarcode(), existing);
            }
        }

        for (BackupDTO.Presentation row : presentations) {
            ProductsEntity product = productById.get(trim(row.productId()));
            ProductPresentation type = ProductPresentation.findByLabel(row.presentation()).orElse(null);
            String barcode = trim(row.barcode());
            if (product == null || type == null || (barcode != null && barcode.length() > MAX_BARCODE)) {
                counters.rowsSkipped++;
                continue;
            }

            String key = key(product.getId(), type);
            ProductPresentationsEntity presentation = byKey.get(key);
            boolean isNew = presentation == null;
            if (isNew) {
                presentation = new ProductPresentationsEntity(null, product, type, null);
                byKey.put(key, presentation);
                counters.presentationsCreated++;
            }

            boolean barcodeChanged = false;
            if (barcode != null && !barcode.equals(presentation.getBarcode())) {
                ProductPresentationsEntity owner = byBarcode.get(barcode);
                if (owner != null && owner != presentation) {
                    counters.barcodeConflicts++;
                } else {
                    if (presentation.getBarcode() != null) {
                        byBarcode.remove(presentation.getBarcode());
                    }
                    presentation.setBarcode(barcode);
                    byBarcode.put(barcode, presentation);
                    counters.barcodesAssigned++;
                    barcodeChanged = true;
                }
            }

            if (isNew) {
                productPresentationsRepository.save(presentation);
            } else if (barcodeChanged) {
                // Se aplica de inmediato: Hibernate ejecuta los INSERT antes que los UPDATE pendientes, y
                // un codigo liberado aqui podria tomarlo una presentacion nueva (barcode es UNIQUE).
                productPresentationsRepository.saveAndFlush(presentation);
            }
        }
    }

    private void importInventory(BackupDTO.Inventory row, Map<String, ProductsEntity> productById,
                                 UserEntity actingUser, Counters counters) {
        String presentation = trim(row.presentation());
        Date date = parseDate(row.date());
        if (presentation == null || presentation.length() > MAX_INVENTORY_PRESENTATION || date == null) {
            counters.rowsSkipped++;
            return;
        }

        InventoriesEntity inventory = new InventoriesEntity();
        inventory.setInventoryDate(date);
        inventory.setPresentation(presentation);
        inventory.setStatus(importedStatus(row.status()));
        inventory.setCreatedBy(actingUser);
        inventory.setCreatedBySnapshot(actingUser != null ? actingUser.getUsuario() : null);
        inventoriesRepository.save(inventory);
        counters.inventoriesCreated++;

        List<StockEntity> stockRows = new ArrayList<>();
        for (BackupDTO.StockRow stockRow : nullSafe(row.stock())) {
            ProductsEntity product = productById.get(trim(stockRow.productId()));
            if (product == null || !isValidQuantity(stockRow.stock())) {
                counters.rowsSkipped++;
                continue;
            }
            stockRows.add(new StockEntity(null, product, stockRow.stock(), inventory));
        }
        stockRepository.saveAll(stockRows);
        counters.stockRowsCreated += stockRows.size();

        List<ProductCountsEntity> counts = new ArrayList<>();
        for (BackupDTO.CountRow countRow : nullSafe(row.counts())) {
            ProductsEntity product = productById.get(trim(countRow.productId()));
            ProductLocation place = parsePlace(countRow.place());
            if (product == null || place == null || !isValidQuantity(countRow.quantity())) {
                counters.rowsSkipped++;
                continue;
            }
            counts.add(new ProductCountsEntity(null, inventory, product, countRow.quantity(), place));
        }
        productCountsRepository.saveAll(counts);
        counters.countsCreated += counts.size();
    }

    /** LOCKED no se importa: el bloqueo era de un dispositivo del sistema que exporto. */
    private static InventoryStatus importedStatus(String status) {
        if ("CLOSED".equals(status)) {
            return InventoryStatus.CLOSED;
        }
        if ("PENDING".equals(status)) {
            return InventoryStatus.PENDING;
        }
        return InventoryStatus.OPENED;
    }

    private static ProductLocation parsePlace(String place) {
        try {
            return place == null ? null : ProductLocation.valueOf(place);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Date parseDate(String date) {
        try {
            return date == null ? null : Date.valueOf(date.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isValidQuantity(Float value) {
        return value != null && !value.isNaN() && Math.abs(value) <= MAX_QUANTITY;
    }

    private static String key(String productId, ProductPresentation presentation) {
        return productId + "|" + presentation.name();
    }

    /** null o texto recortado; un texto vacio cuenta como null. */
    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list != null ? list : List.of();
    }
}
