package com.inventories.dto.products;

import jakarta.validation.constraints.NotBlank;

public record UpdateProductDTO(@NotBlank String description) {
}
