package com.inventories.dto.inventories;

/**
 * presentationsSkipped: filas cuya UNIDAD no es una presentacion conocida (no
 * se registran en product_presentations, pero si en products y stock).
 * barcodeConflicts: codigos de barras que ya pertenecen a otra
 * presentacion/producto y por eso no se asignaron.
 */
public record InventoryUploadResultDTO(Long inventoryId,
                                       String presentation,
                                       int rowsProcessed,
                                       int productsCreated,
                                       int stockRowsCreated,
                                       int presentationsCreated,
                                       int barcodesAssigned,
                                       int presentationsSkipped,
                                       int barcodeConflicts) {
}
