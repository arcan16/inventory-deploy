package com.inventories.models;

import com.inventories.dto.productsCount.CreateProductCountDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "ProductsEntity")
@Table(name = "products")
@Data
@NoArgsConstructor
public class ProductsEntity {

    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;
    private String description;

    public ProductsEntity(String id, String description) {
        this.id=id;
        this.description = description;
    }
}
