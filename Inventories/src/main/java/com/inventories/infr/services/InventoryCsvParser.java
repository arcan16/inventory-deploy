package com.inventories.infr.services;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Puerto a Java de la limpieza que antes hacia a mano
 * CargaDeArchivos/modules/converter.py sobre el reporte de existencias que
 * entrega el proveedor. El archivo trae 4 filas de encabezado/metadatos
 * antes del encabezado real (fila 5), varias columnas vacias intercaladas,
 * una columna "NO SHOW" que no se usa, y 2 filas de pie de reporte al final.
 * Los reportes mas recientes agregan la columna opcional "COD BARRAS".
 */
@Component
public class InventoryCsvParser {

    private static final String HEADER_PRODUCT = "PRODUCTO";
    private static final String HEADER_UNIT = "UNIDAD";
    private static final String HEADER_STOCK = "EXISTENCIA";
    private static final String HEADER_DESCRIPTION = "DESCRIPCION DEL PRODUCTO";
    private static final String HEADER_BARCODE = "COD BARRAS";

    // Indice (0-based) de la fila que trae el encabezado real dentro del archivo (fila 5 del csv).
    private static final int HEADER_ROW_INDEX = 4;
    // A partir de que fila (0-based) comienzan los datos (fila 6 del csv).
    private static final int DATA_START_INDEX = 5;
    // Filas de pie de reporte al final del archivo que deben descartarse.
    private static final int TRAILING_ROWS_TO_DISCARD = 2;

    // El sistema del proveedor exporta en Windows-1252 (p. ej. la Ñ es el byte 0xD1).
    private static final Charset FALLBACK_CHARSET = Charset.forName("windows-1252");

    /** barcode es null cuando el archivo no trae la columna o la celda viene vacia. */
    public record ProductRow(String productId, String unit, String description, float stock, String barcode) {
    }

    public record ParsedInventoryFile(String presentation, List<ProductRow> rows) {
    }

    public ParsedInventoryFile parse(MultipartFile file) throws IOException {
        List<CSVRecord> allLines;
        try (StringReader reader = new StringReader(decode(file.getBytes()));
             CSVParser parser = CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(true).build().parse(reader)) {
            allLines = parser.getRecords();
        }

        if (allLines.size() <= DATA_START_INDEX) {
            throw new IllegalArgumentException("El archivo no tiene el formato esperado del reporte de existencias");
        }

        Map<String, Integer> columnIndexByName = resolveColumns(allLines.get(HEADER_ROW_INDEX));

        List<CSVRecord> dataRows = allLines.subList(DATA_START_INDEX, allLines.size());
        if (dataRows.size() <= TRAILING_ROWS_TO_DISCARD) {
            throw new IllegalArgumentException("El archivo no contiene productos para cargar");
        }
        dataRows = dataRows.subList(0, dataRows.size() - TRAILING_ROWS_TO_DISCARD);

        int productIdx = columnIndexByName.get(HEADER_PRODUCT);
        int unitIdx = columnIndexByName.get(HEADER_UNIT);
        int stockIdx = columnIndexByName.get(HEADER_STOCK);
        int descriptionIdx = columnIndexByName.get(HEADER_DESCRIPTION);
        Integer barcodeIdx = columnIndexByName.get(HEADER_BARCODE);

        List<ProductRow> rows = new ArrayList<>();
        for (CSVRecord record : dataRows) {
            String productId = record.get(productIdx).trim();
            if (productId.isEmpty()) {
                throw new IllegalArgumentException("La fila " + record.getRecordNumber() + " del archivo no tiene un id de producto valido");
            }

            String stockRaw = record.get(stockIdx).trim();
            float stock;
            try {
                stock = Float.parseFloat(stockRaw);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("La fila " + record.getRecordNumber() + " tiene un valor de existencia invalido: '" + stockRaw + "'");
            }

            String unit = record.get(unitIdx).trim();
            String description = record.get(descriptionIdx).trim();
            String barcode = barcodeIdx != null ? record.get(barcodeIdx).trim() : "";
            rows.add(new ProductRow(productId, unit, description, stock, barcode.isEmpty() ? null : barcode));
        }

        String presentation = rows.get(0).unit();
        return new ParsedInventoryFile(presentation, rows);
    }

    /** Lee el archivo como UTF-8 si es valido; si no, como Windows-1252. */
    private String decode(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, FALLBACK_CHARSET);
        }
    }

    /**
     * Ubica cada columna por su nombre en el encabezado (primera aparicion),
     * ignorando las columnas sin nombre y las que no se usan ("NO SHOW",
     * "P UNIT", "IMPORTE"). "COD BARRAS" es opcional para seguir aceptando
     * los reportes anteriores que no la traen.
     */
    private Map<String, Integer> resolveColumns(CSVRecord headerRow) {
        Map<String, Integer> columnIndexByName = new HashMap<>();
        for (int i = 0; i < headerRow.size(); i++) {
            String name = headerRow.get(i).trim();
            if (!name.isEmpty()) {
                columnIndexByName.putIfAbsent(name, i);
            }
        }

        for (String required : List.of(HEADER_PRODUCT, HEADER_UNIT, HEADER_STOCK, HEADER_DESCRIPTION)) {
            if (!columnIndexByName.containsKey(required)) {
                throw new IllegalArgumentException("El archivo no contiene la columna esperada '" + required + "'");
            }
        }

        return columnIndexByName;
    }
}
