package com.inventories.dto.productsCount;

public record CountsdifferenceDTO(Long id,
                                  Long idInventory,
                                  String description,
                                  String idProduct,
                                  float stock,
                                  Double sum,
                                  Double difference
                                  ) {
}
