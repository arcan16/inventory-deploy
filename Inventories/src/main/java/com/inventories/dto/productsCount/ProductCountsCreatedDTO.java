package com.inventories.dto.productsCount;

import com.inventories.models.ProductCountsEntity;
import com.inventories.models.enums.ProductLocation;

public record ProductCountsCreatedDTO(Long id,
                                      String idProduct,
                                      String description,
                                      float quantity,
                                      ProductLocation place) {
    public ProductCountsCreatedDTO(ProductCountsEntity productCounts) {
        this(productCounts.getId(), productCounts.getIdProduct().getId(),
                productCounts.getIdProduct().getDescription(), productCounts.getQuantity(),
                productCounts.getPlace());
    }
}
