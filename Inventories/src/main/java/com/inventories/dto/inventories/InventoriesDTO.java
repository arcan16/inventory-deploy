package com.inventories.dto.inventories;

import com.inventories.models.InventoriesEntity;
import com.inventories.models.enums.InventoryStatus;

import java.sql.Date;
import java.sql.Timestamp;

public record InventoriesDTO(Long id,
                             Date date,
                             String presentation,
                             Long createdBy,
                             String createdBySnapshot,
                             InventoryStatus status,
                             Long lockedBy,
                             Timestamp lockedAt,
                             String lockedByUsername,
                             String lockedDevice) {
    public InventoriesDTO(InventoriesEntity inventories){
        this(inventories.getId(),
                inventories.getInventoryDate(),
                inventories.getPresentation(),
                inventories.getCreatedBy() != null ? inventories.getCreatedBy().getId() : null,
                inventories.getCreatedBySnapshot(),
                inventories.getStatus(),
                inventories.getLockedBy() != null ? inventories.getLockedBy().getId() : null,
                inventories.getLockedAt(),
                // Quien lo esta usando: la app lo muestra y permite reentrar a su propio usuario.
                inventories.getLockedBy() != null ? inventories.getLockedBy().getUsuario() : null,
                // Dispositivo que lo tiene: la app solo deja reentrar al mismo dispositivo.
                inventories.getLockedDevice());
    }
}
