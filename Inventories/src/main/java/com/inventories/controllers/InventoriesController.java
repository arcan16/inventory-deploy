package com.inventories.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.inventories.dto.inventories.InventoriesDTO;
import com.inventories.dto.products.ProductPresentationDTO;
import com.inventories.dto.products.ProductsDTO;
import com.inventories.dto.productsCount.ProductCountsEntryDTO;
import com.inventories.dto.stock.StockListDTO;
import com.inventories.infr.services.InventoryLockService;
import com.inventories.infr.services.InventoryService;
import com.inventories.models.InventoriesEntity;
import com.inventories.models.ProductCountsEntity;
import com.inventories.models.UserEntity;
import com.inventories.models.enums.InventoryStatus;
import com.inventories.repositories.InventoriesRepository;
import com.inventories.repositories.ProductCountsRepository;
import com.inventories.repositories.ProductPresentationsRepository;
import com.inventories.repositories.ProductsRepository;
import com.inventories.repositories.StockRepository;
import com.inventories.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inventories")
public class InventoriesController {

    /** Identificador de la instalacion de la app; el bloqueo de inventario es por dispositivo. */
    private static final String DEVICE_HEADER = "X-Device-Id";

    @Autowired
    private InventoriesRepository inventoriesRepository;

    @Autowired
    private ProductCountsRepository productCountsRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ProductPresentationsRepository productPresentationsRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryLockService inventoryLockService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getAllInventories(@PageableDefault(size = 10)Pageable pageable){
        List<InventoriesEntity> inventories = inventoriesRepository.findAll();
        return ResponseEntity.ok(inventoriesRepository.findAll(pageable).map(InventoriesDTO::new));
    }

    /**
     * Crea una lista con todos los elementos que incluyan el tipo de dato recibido
     * @param type String con el valor de la presentacion que sera buscada
     * @return Lista de datos
     */
    @GetMapping("/allByType/{type}")
    public ResponseEntity<?> getAllInventoriesByType(@PathVariable String type){
        List<InventoriesDTO> inventoriesList = inventoryService.getByType(type);
        if(inventoriesList.isEmpty())
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok().body(inventoriesList);
    }

    @GetMapping("/normal/{idInventory}")
    public ResponseEntity<?> getNormalInventory(@PathVariable Long idInventory){
        List<ProductCountsEntity> productCountsList = productCountsRepository.getInventoryCounts(idInventory);
        List<ProductCountsEntryDTO> productCounts = productCountsList.stream().map(ProductCountsEntryDTO::new).toList();
        List<ProductsDTO> products = productsRepository.findAll().stream().map(ProductsDTO::new).toList();
        List<StockListDTO> stockList = stockRepository.getByIdInventory(idInventory);

        ObjectMapper objectMapper =new ObjectMapper();
        ObjectNode jsonResponse = objectMapper.createObjectNode();

        ArrayNode productsNode = objectMapper.valueToTree(products);
        jsonResponse.set("products", productsNode);

        ArrayNode productCountsNode = objectMapper.valueToTree(productCounts);
        jsonResponse.set("productsCount", productCountsNode);

        ArrayNode stockNode = objectMapper.valueToTree(stockList);
        jsonResponse.set("stock", stockNode);

        // Presentaciones con codigo de barras: la app busca aqui primero al escanear, sin ir a la red.
        List<ProductPresentationDTO> barcodes = productPresentationsRepository.findAllWithBarcode().stream()
                .map(ProductPresentationDTO::new).toList();
        jsonResponse.set("barcodes", objectMapper.valueToTree(barcodes));

        return ResponseEntity.ok().body(jsonResponse);
    }


    /**
     * Cierra el inventario indicado, marcandolo como CLOSED para que ya no reciba
     * mas conteos
     * @param idInventory Parametro recibido en la url de la peticion
     * @return El inventario actualizado
     */
    @PutMapping("/{idInventory}/close")
    public ResponseEntity<?> closeInventory(@PathVariable Long idInventory){
        Optional<InventoriesEntity> inventoryOpt = inventoriesRepository.findById(idInventory);
        if(inventoryOpt.isEmpty())
            return ResponseEntity.badRequest().body("{\"err\": \" El id no existe \"}");

        InventoriesEntity inventory = inventoryOpt.get();
        if(inventory.getStatus() == InventoryStatus.CLOSED)
            return ResponseEntity.badRequest().body("{\"err\": \" El inventario ya esta cerrado \"}");

        // Al cerrar se suelta el bloqueo de quien lo estaba contando (Finalizar conteo).
        InventoryLockService.clearLock(inventory, InventoryStatus.CLOSED);
        inventoriesRepository.save(inventory);
        return ResponseEntity.ok(new InventoriesDTO(inventory));
    }

    /**
     * Reabre un inventario previamente cerrado, regresandolo al estado OPENED
     * @param idInventory Parametro recibido en la url de la peticion
     * @return El inventario actualizado
     */
    @PutMapping("/{idInventory}/reopen")
    public ResponseEntity<?> reopenInventory(@PathVariable Long idInventory){
        Optional<InventoriesEntity> inventoryOpt = inventoriesRepository.findById(idInventory);
        if(inventoryOpt.isEmpty())
            return ResponseEntity.badRequest().body("{\"err\": \" El id no existe \"}");

        InventoriesEntity inventory = inventoryOpt.get();
        if(inventory.getStatus() != InventoryStatus.CLOSED)
            return ResponseEntity.badRequest().body("{\"err\": \" El inventario no esta cerrado \"}");

        InventoryLockService.clearLock(inventory, InventoryStatus.OPENED);
        inventoriesRepository.save(inventory);
        return ResponseEntity.ok(new InventoriesDTO(inventory));
    }

    /**
     * Marca el inventario como en uso (LOCKED) por el usuario autenticado al
     * entrar a su conteo. La app lo vuelve a llamar cada minuto para renovar el
     * bloqueo; sin renovacion, se libera solo (ver InventoryLockService).
     * 409 si otro usuario lo esta usando, indicando quien.
     */
    @PutMapping("/{idInventory}/lock")
    public ResponseEntity<?> lockInventory(@PathVariable Long idInventory, Authentication authentication,
                                           @RequestHeader(value = DEVICE_HEADER, required = false) String deviceId){
        InventoryLockService.Result result = inventoryLockService.lock(idInventory, currentUser(authentication), deviceId);
        return switch (result.outcome()) {
            case NOT_FOUND -> ResponseEntity.badRequest().body("{\"err\": \"El inventario no existe\"}");
            case CLOSED -> ResponseEntity.badRequest().body("{\"err\": \"El inventario esta cerrado\"}");
            case LOCKED_BY_OTHER -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("{\"err\": \"" + (result.sameUser()
                            ? "El inventario esta abierto en otro dispositivo con el usuario " + result.holder()
                            : "El inventario lo esta usando " + result.holder()) + "\"}");
            default -> ResponseEntity.ok(new InventoriesDTO(result.inventory()));
        };
    }

    /**
     * Regresa el inventario a OPENED al salir de su conteo (o al mandar la app a
     * segundo plano). Si el bloqueo es de otro dispositivo no cambia nada.
     */
    @PutMapping("/{idInventory}/unlock")
    public ResponseEntity<?> unlockInventory(@PathVariable Long idInventory, Authentication authentication,
                                             @RequestHeader(value = DEVICE_HEADER, required = false) String deviceId){
        InventoryLockService.Result result = inventoryLockService.unlock(idInventory, currentUser(authentication), deviceId);
        if(result.outcome() == InventoryLockService.Outcome.NOT_FOUND)
            return ResponseEntity.badRequest().body("{\"err\": \"El inventario no existe\"}");
        return ResponseEntity.ok(new InventoriesDTO(result.inventory()));
    }

    private UserEntity currentUser(Authentication authentication){
        return authentication == null ? null : userRepository.findByUsuario(authentication.getName()).orElse(null);
    }

    /**
     * Elimina el inventario indicado a traves del parametro recibido en la url
     * @param idInventory Parametro recibido en la url de la peticion
     * @return Regresa un mensaje de confirmacion
     */
    @DeleteMapping("/{idInventory}")
    public ResponseEntity<?> deleteInventory(@PathVariable Long idInventory){
        Optional<InventoriesEntity> inventories = inventoriesRepository.findById(idInventory);
        if(inventories.isEmpty())
            return ResponseEntity.badRequest().body("{\"err\": \" El id no existe \"}");
        inventoriesRepository.deleteById(idInventory);
        return ResponseEntity.ok().body("{\" message \": \" Inventario eliminado con exito \"}");

    }
}
