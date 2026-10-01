package com.inventories.dto.backup;

/**
 * Resultado de importar un respaldo. Importar combina: nunca borra nada.
 * rowsSkipped: renglones que no se pudieron importar (producto inexistente,
 * presentacion o lugar desconocido, valores fuera de rango...).
 * barcodeConflicts: codigos de barras que ya pertenecen a otra presentacion.
 */
public record ImportResultDTO(int productsCreated,
                              int productsUpdated,
                              int presentationsCreated,
                              int barcodesAssigned,
                              int barcodeConflicts,
                              int inventoriesCreated,
                              int stockRowsCreated,
                              int countsCreated,
                              int rowsSkipped) {
}
