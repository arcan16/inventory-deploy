package com.inventories.repositories;

import com.inventories.dto.productsCount.CountsdifferenceDTO;
import com.inventories.dto.productsCount.ReportsDTO;
import com.inventories.models.ProductCountsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductCountsRepository extends JpaRepository<ProductCountsEntity, Long> {
    @Query("""
            SELECT pc FROM ProductCountsEntity pc
            JOIN FETCH pc.idInventory
            JOIN FETCH pc.idProduct
            WHERE pc.idInventory.id =:idInventory
            """)
    List<ProductCountsEntity> getInventoryCounts(Long idInventory);

    @Query("""
            SELECT pc FROM ProductCountsEntity pc WHERE pc.idInventory.id = :idInventory
            """)
    Optional<ProductCountsEntity> findByIdInventory(Long idInventory);

    @Query("""
            select New com.inventories.dto.productsCount.CountsdifferenceDTO(
                s.id,
                s.idInventory.id,
                p.description,
                s.idProduct.id,
                s.stock,
                coalesce(sum(pc.quantity),0) as sum,
                (coalesce(sum(pc.quantity),0) - s.stock) as difference
            )
            from StockEntity s
            left join ProductCountsEntity pc on pc.idProduct.id = s.idProduct.id and pc.idInventory.id = s.idInventory.id
            inner join ProductsEntity p on p.id = s.idProduct.id
            group by s.id, s.idInventory.id, s.idProduct.id, s.stock
            having s.idInventory.id = :idInventory
            order by s.id
            """)
    List<CountsdifferenceDTO> getCountDifference(Long idInventory);

    /**
     * Productos contados en el inventario que no vienen en su stock (p. ej.
     * registrados durante el conteo). Cada fila: [id producto, descripcion,
     * total contado]. getCountDifference no los incluye porque parte del stock.
     */
    @Query("""
            select p.id, p.description, sum(pc.quantity)
            from ProductCountsEntity pc
            join pc.idProduct p
            where pc.idInventory.id = :idInventory
            and not exists (
                select s.id from StockEntity s
                where s.idInventory.id = :idInventory and s.idProduct.id = p.id
            )
            group by p.id, p.description
            order by p.id
            """)
    List<Object[]> getCountedWithoutStock(Long idInventory);

    @Query("""
            SELECT NEW com.inventories.dto.productsCount.ReportsDTO(
                                pc.idInventory.id,
                                i.inventoryDate,
                                i.presentation
                        )
                        FROM ProductCountsEntity pc
                        INNER JOIN InventoriesEntity i on pc.idInventory.id = i.id
                        GROUP BY pc.idInventory.id, i.inventoryDate, i.presentation
            """)
    List<ReportsDTO> getReports();
}

