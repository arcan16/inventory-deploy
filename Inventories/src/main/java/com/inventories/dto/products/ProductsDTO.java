package com.inventories.dto.products;

import com.inventories.models.ProductsEntity;
import org.antlr.v4.runtime.misc.NotNull;

public record ProductsDTO(@NotNull String id,
                          @NotNull String description) {
    public ProductsDTO(ProductsEntity products){
        this(products.getId(), products.getDescription());
    }
}
