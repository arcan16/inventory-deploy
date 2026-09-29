package com.inventories.models;

import com.inventories.models.enums.ProductPresentation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "ProductPresentationsEntity")
@Table(name = "product_presentations")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductPresentationsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_product")
    private ProductsEntity idProduct;

    @Convert(converter = ProductPresentation.JpaConverter.class)
    private ProductPresentation presentation;

    private String barcode;
}
