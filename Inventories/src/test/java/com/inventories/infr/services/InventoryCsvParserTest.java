package com.inventories.infr.services;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Valida el parser contra un reporte real de existencias del proveedor
 * (formato con filas/columnas basura), tal como lo procesaba antes
 * CargaDeArchivos/modules/converter.py.
 */
class InventoryCsvParserTest {

    private final InventoryCsvParser parser = new InventoryCsvParser();

    @Test
    void parsesRealSupplierReport() throws IOException {
        MockMultipartFile file = loadSample("csv/GALONES.csv");

        InventoryCsvParser.ParsedInventoryFile result = parser.parse(file);

        assertEquals("4 LTS", result.presentation());
        assertEquals(333, result.rows().size());

        InventoryCsvParser.ProductRow first = result.rows().get(0);
        assertEquals("10292", first.productId());
        assertEquals("THINNER E", first.description());
        assertEquals(1.200f, first.stock());

        InventoryCsvParser.ProductRow last = result.rows().get(result.rows().size() - 1);
        assertEquals("HP-10-YEL", last.productId());
        assertEquals("YELLOW TINT", last.description());
        assertEquals(1.000f, last.stock());

        // Este formato no trae la columna COD BARRAS.
        assertNull(first.barcode());
        assertEquals("4 LTS", first.unit());
    }

    @Test
    void parsesReportWithBarcodeColumn() throws IOException {
        MockMultipartFile file = loadSample("csv/cubetas_barcode.csv");

        InventoryCsvParser.ParsedInventoryFile result = parser.parse(file);

        assertEquals("1 LT", result.presentation());
        assertEquals(399, result.rows().size());

        InventoryCsvParser.ProductRow first = result.rows().get(0);
        assertEquals("100", first.productId());
        assertEquals("1 LT", first.unit());
        assertEquals("COMEX 100 BLANCO", first.description());
        assertEquals(2.000f, first.stock());
        assertEquals("7500025010471", first.barcode());

        InventoryCsvParser.ProductRow withoutBarcode = result.rows().get(1);
        assertEquals("1000013", withoutBarcode.productId());
        assertNull(withoutBarcode.barcode());

        // El reporte viene en Windows-1252: la Ñ debe decodificarse correctamente.
        assertTrue(result.rows().stream()
                .anyMatch(row -> row.description().equals("VIA COLOR SEÑALAMIENTO HIGH TECH AMARILLO")));

        // Se conservan los ceros a la izquierda del codigo.
        assertTrue(result.rows().stream().anyMatch(row -> "0762281207607".equals(row.barcode())));

        InventoryCsvParser.ProductRow last = result.rows().get(result.rows().size() - 1);
        assertEquals("H024352", last.productId());
        assertEquals("GEL ANTIBACTERIAL 900 ML", last.description());
    }

    @Test
    void rejectsFileWithoutExpectedShape() {
        MockMultipartFile file = new MockMultipartFile("file", "bad.csv", "text/csv",
                "a,b,c\n1,2,3\n".getBytes());

        assertThrows(IllegalArgumentException.class, () -> parser.parse(file));
    }

    private MockMultipartFile loadSample(String classpathLocation) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(classpathLocation)) {
            if (inputStream == null) {
                throw new IllegalStateException("No se encontro el archivo de prueba " + classpathLocation);
            }
            return new MockMultipartFile("file", "GALONES.csv", "text/csv", inputStream.readAllBytes());
        }
    }
}
