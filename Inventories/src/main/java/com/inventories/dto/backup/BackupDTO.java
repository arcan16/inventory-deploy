package com.inventories.dto.backup;

import java.util.List;

/**
 * Archivo de respaldo (JSON) que comparten el backend, la app cliente y la app
 * local (ComexBeta). Contiene todo: catalogo, presentaciones con su codigo de
 * barras, e inventarios con su stock y sus conteos. Las cantidades van como
 * numero y las fechas como "yyyy-MM-dd".
 *
 * Al cambiar la forma del archivo hay que subir VERSION y seguir aceptando las
 * versiones anteriores en DataTransferService.importBackup (y en la app local).
 */
public record BackupDTO(String format,
                        int version,
                        String exportedAt,
                        List<Product> products,
                        List<Presentation> productPresentations,
                        List<Inventory> inventories) {

    public static final String FORMAT = "comex-inventory-backup";
    public static final int VERSION = 1;

    public record Product(String id, String description) {
    }

    /** presentation es el label ("1 LT", "4 LTS"...); barcode puede ser null. */
    public record Presentation(String productId, String presentation, String barcode) {
    }

    /** id es el del sistema que exporto, solo como referencia: al importar se crea un inventario nuevo. */
    public record Inventory(Long id,
                            String date,
                            String presentation,
                            String status,
                            List<StockRow> stock,
                            List<CountRow> counts) {
    }

    public record StockRow(String productId, Float stock) {
    }

    public record CountRow(String productId, Float quantity, String place) {
    }
}
