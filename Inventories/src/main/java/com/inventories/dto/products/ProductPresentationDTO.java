package com.inventories.dto.products;

import com.inventories.models.ProductPresentationsEntity;

/**
 * Una presentacion de un producto con su codigo de barras. Incluye la
 * descripcion del producto para que la app pueda mostrar el registro completo
 * sin otra consulta. "presentation" es el label ("1 LT", "4 LTS"...).
 */
public record ProductPresentationDTO(Long id,
                                     String productId,
                                     String description,
                                     String presentation,
                                     String barcode) {
    public ProductPresentationDTO(ProductPresentationsEntity entity) {
        this(entity.getId(),
                entity.getIdProduct().getId(),
                entity.getIdProduct().getDescription(),
                entity.getPresentation().getLabel(),
                entity.getBarcode());
    }
}
