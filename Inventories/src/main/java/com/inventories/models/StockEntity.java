package com.inventories.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "StockEntity")
@Table(name = "stock")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_product")
    private ProductsEntity idProduct;
    private Float stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_inventory")
    private InventoriesEntity idInventory;
}
