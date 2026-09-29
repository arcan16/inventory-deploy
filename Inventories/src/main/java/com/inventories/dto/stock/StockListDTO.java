package com.inventories.dto.stock;

import com.inventories.models.StockEntity;

public record StockListDTO(Long id,
                           String idProduct,
                           String description,
                           float stock,
                           Long idInventory) {
//    public StockListDTO(StockEntity stock){
//        this(stock.getId(), stock.getIdProduct().getId(),
//                stock.getStock(), stock.getIdInventory().getId());
//    }
}
