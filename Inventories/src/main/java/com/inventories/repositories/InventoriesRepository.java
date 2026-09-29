package com.inventories.repositories;

import com.inventories.dto.inventories.InventoriesDTO;
import com.inventories.models.InventoriesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoriesRepository extends JpaRepository<InventoriesEntity, Long> {
    @Query("""
            SELECT i FROM InventoriesEntity i where i.presentation = :type
            """)
    List<InventoriesDTO> getAllByType(@Param("type") String type);
}
