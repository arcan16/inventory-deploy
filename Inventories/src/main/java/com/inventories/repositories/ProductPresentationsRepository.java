package com.inventories.repositories;

import com.inventories.models.ProductPresentationsEntity;
import com.inventories.models.enums.ProductPresentation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductPresentationsRepository extends JpaRepository<ProductPresentationsEntity, Long> {

    @Query("""
            SELECT pp FROM ProductPresentationsEntity pp WHERE pp.idProduct.id IN :productIds
            """)
    List<ProductPresentationsEntity> findByProductIds(@Param("productIds") Collection<String> productIds);

    @Query("""
            SELECT pp FROM ProductPresentationsEntity pp WHERE pp.barcode IN :barcodes
            """)
    List<ProductPresentationsEntity> findByBarcodes(@Param("barcodes") Collection<String> barcodes);

    /** Presentaciones de un producto con el producto ya cargado (para armar el DTO). */
    @Query("""
            SELECT pp FROM ProductPresentationsEntity pp JOIN FETCH pp.idProduct p WHERE p.id = :productId
            """)
    List<ProductPresentationsEntity> findByProductIdWithProduct(@Param("productId") String productId);

    /** Todas las presentaciones que ya tienen codigo de barras (para buscar por codigo en el conteo). */
    @Query("""
            SELECT pp FROM ProductPresentationsEntity pp JOIN FETCH pp.idProduct WHERE pp.barcode IS NOT NULL
            """)
    List<ProductPresentationsEntity> findAllWithBarcode();

    @Query("""
            SELECT pp FROM ProductPresentationsEntity pp JOIN FETCH pp.idProduct p
            WHERE p.id = :productId AND pp.presentation = :presentation
            """)
    Optional<ProductPresentationsEntity> findByProductIdAndPresentation(@Param("productId") String productId,
                                                                        @Param("presentation") ProductPresentation presentation);

    @Query("""
            SELECT pp FROM ProductPresentationsEntity pp JOIN FETCH pp.idProduct WHERE pp.barcode = :barcode
            """)
    Optional<ProductPresentationsEntity> findByBarcode(@Param("barcode") String barcode);
}
