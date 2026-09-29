package com.inventories.dto.products;

import jakarta.validation.constraints.NotBlank;

/**
 * "barcode" con contenido reemplaza el codigo guardado; vacio o null lo
 * conserva. Para quitarlo hay que pedirlo explicitamente con clearBarcode=true
 * (solo aplica si barcode viene vacio).
 */
public record UpdateProductPresentationDTO(@NotBlank String description, String barcode, Boolean clearBarcode) {
}
