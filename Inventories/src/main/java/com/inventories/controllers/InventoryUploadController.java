package com.inventories.controllers;

import com.inventories.dto.inventories.InventoryUploadResultDTO;
import com.inventories.infr.services.InventoryCsvLoaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Reemplaza el flujo manual de la app de escritorio CargaDeArchivos: sube el
 * reporte de existencias (csv) del proveedor y realiza el mismo proceso que
 * antes hacia converter.py + db_connect.py (limpiar el archivo, crear el
 * inventario, dar de alta productos nuevos y registrar el stock).
 */
@RestController
@RequestMapping("/inventories/upload")
public class InventoryUploadController {

    @Autowired
    private InventoryCsvLoaderService inventoryCsvLoaderService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadInventory(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"err\": \"El archivo esta vacio\"}");
        }

        try {
            InventoryUploadResultDTO result = inventoryCsvLoaderService.loadInventory(file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("{\"err\": \"" + e.getMessage() + "\"}");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("{\"err\": \"No fue posible leer el archivo\"}");
        }
    }
}
