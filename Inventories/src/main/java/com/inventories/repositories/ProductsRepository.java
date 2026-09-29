package com.inventories.repositories;

import com.inventories.models.ProductsEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductsRepository extends JpaRepository<ProductsEntity, String> {

    @Query("""
            SELECT p FROM ProductsEntity p WHERE p.id = :id
            """)
    ProductsEntity findByStringId(String id);

    @Query("""
            SELECT p FROM ProductsEntity p
            WHERE LOWER(p.id) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<ProductsEntity> search(@Param("query") String query, Pageable pageable);
}
