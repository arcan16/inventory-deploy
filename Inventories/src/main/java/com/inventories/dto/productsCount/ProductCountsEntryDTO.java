package com.inventories.dto.productsCount;

import com.inventories.models.ProductCountsEntity;
import com.inventories.models.enums.ProductLocation;

public record ProductCountsEntryDTO(Long id,
                                    float quantity,
                                    ProductLocation place,
                                    ProductRefDTO idProduct) {
    public ProductCountsEntryDTO(ProductCountsEntity productCounts) {
        this(productCounts.getId(), productCounts.getQuantity(), productCounts.getPlace(),
                new ProductRefDTO(productCounts.getIdProduct().getId(), productCounts.getIdProduct().getDescription()));
    }

    public record ProductRefDTO(String id, String description) {
    }
}
