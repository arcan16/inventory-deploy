package com.inventories.models;

import com.inventories.models.enums.InventoryStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.sql.Timestamp;

@Entity(name = "InventoriesEntity")
@Table(name = "inventories")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoriesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    private String createdBySnapshot;

    private Date inventoryDate = new Date(System.currentTimeMillis());
    private String presentation;

    @Enumerated(EnumType.STRING)
    private InventoryStatus status = InventoryStatus.OPENED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_by")
    private UserEntity lockedBy;

    private Timestamp lockedAt;
}
