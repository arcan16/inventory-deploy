package com.inventories.repositories;

import com.inventories.dto.inventories.InventoriesDTO;
import com.inventories.models.InventoriesEntity;
import com.inventories.models.enums.InventoryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface InventoriesRepository extends JpaRepository<InventoriesEntity, Long> {
    @Query("""
            SELECT i FROM InventoriesEntity i where i.presentation = :type
            """)
    List<InventoriesDTO> getAllByType(@Param("type") String type);

    /**
     * Lee el inventario bloqueando su fila hasta el fin de la transaccion, para
     * que dos dispositivos que entran al mismo tiempo no lo tomen ambos.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i FROM InventoriesEntity i LEFT JOIN FETCH i.lockedBy WHERE i.id = :id
            """)
    Optional<InventoriesEntity> findByIdForUpdate(@Param("id") Long id);

    /**
     * Libera los bloqueos que no se renovaron desde "limit" (la app se cerro de
     * golpe o perdio la red sin avisar que salio del conteo).
     */
    @Modifying
    @Query("""
            UPDATE InventoriesEntity i SET i.status = :opened, i.lockedBy = null, i.lockedAt = null, i.lockedDevice = null
            WHERE i.status = :locked AND (i.lockedAt IS NULL OR i.lockedAt < :limit)
            """)
    int releaseStaleLocks(@Param("opened") InventoryStatus opened,
                          @Param("locked") InventoryStatus locked,
                          @Param("limit") Timestamp limit);
}
