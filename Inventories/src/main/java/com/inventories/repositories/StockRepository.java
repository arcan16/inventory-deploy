package com.inventories.repositories;

import com.inventories.dto.stock.StockListDTO;
import com.inventories.models.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<StockEntity, Long> {
    @Query("""
            SELECT s FROM StockEntity s WHERE s.idInventory.id = :idInventory
            """)
    List<StockEntity> findByIdInventory(Long idInventory);

    @Query("""
            SELECT NEW com.inventories.dto.stock.StockListDTO(
                s.id,
                s.idProduct.id,
                p.description,
                s.stock,
                s.idInventory.id)
            FROM StockEntity s inner join ProductsEntity p on p.id = s.idProduct.id
            where s.idInventory.id = :idInventory
            """)
    List<StockListDTO> getByIdInventory(Long idInventory);
}
