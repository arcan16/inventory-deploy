package com.inventories.dto.productsCount;


import com.inventories.models.enums.ProductLocation;

import javax.validation.constraints.NotNull;

public record NewProductCountDTO(@NotNull Long idInventory,
                                 @NotNull String idProduct,
                                 @NotNull Float quantity,
                                 @NotNull ProductLocation place) {
}
