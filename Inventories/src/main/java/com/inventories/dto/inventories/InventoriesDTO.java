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
                             Timestamp lockedAt) {
    public InventoriesDTO(InventoriesEntity inventories){
        this(inventories.getId(),
                inventories.getInventoryDate(),
                inventories.getPresentation(),
                inventories.getCreatedBy() != null ? inventories.getCreatedBy().getId() : null,
                inventories.getCreatedBySnapshot(),
                inventories.getStatus(),
                inventories.getLockedBy() != null ? inventories.getLockedBy().getId() : null,
                inventories.getLockedAt());
    }
}
