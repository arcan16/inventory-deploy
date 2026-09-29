package com.inventories.dto.productsCount;

import com.inventories.models.ProductCountsEntity;
import com.inventories.models.enums.ProductLocation;

public record ListProductCountsDTO(Long id,
                                   Long idInventory,
                                   String idProduct,
                                   float quantity,
                                   ProductLocation place) {
    public ListProductCountsDTO(ProductCountsEntity productCounts){
        this(productCounts.getId(), productCounts.getIdInventory().getId(),
                productCounts.getIdProduct().getId(),productCounts.getQuantity(),
                productCounts.getPlace());
    }
}
