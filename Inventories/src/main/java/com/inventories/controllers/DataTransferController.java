package com.inventories.controllers;

import com.inventories.dto.backup.BackupDTO;
import com.inventories.dto.backup.ImportResultDTO;
import com.inventories.infr.services.DataTransferService;
import com.inventories.models.UserEntity;
import com.inventories.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Respaldo de todos los datos (catalogo, presentaciones, inventarios, stock y
 * conteos) en el formato que tambien entiende la app local ComexBeta. Ver
 * DataTransferService para las reglas de importacion (combina, nunca borra).
 */
@RestController
@RequestMapping("/data")
public class DataTransferController {

    @Autowired
    private DataTransferService dataTransferService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/export")
    public ResponseEntity<BackupDTO> exportData() {
        return ResponseEntity.ok(dataTransferService.exportBackup());
    }

    @PostMapping("/import")
    public ResponseEntity<?> importData(@RequestBody BackupDTO backup, Authentication authentication) {
        UserEntity actingUser = authentication == null ? null
                : userRepository.findByUsuario(authentication.getName()).orElse(null);
        try {
            ImportResultDTO result = dataTransferService.importBackup(backup, actingUser);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("{\"err\": \"" + e.getMessage() + "\"}");
        }
    }
}
