package com.inventories.infr.services;

import com.inventories.dto.productsCount.CountsdifferenceDTO;
import com.inventories.models.InventoriesEntity;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


@Service
public class PdfCreator {
    Document document;
    FileOutputStream fileOutputStream;

    Font fontTitle = FontFactory.getFont(FontFactory.TIMES, 16);
    Font fontParagraph = FontFactory.getFont(FontFactory.HELVETICA, 12);
    Font fontTableHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    Font fontSectionTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

    public void createPdf(Long idInventory) throws DocumentException, IOException {
        document = new Document(PageSize.A4, 35, 30, 50, 50);
        String pdfPath = ("src/main/resources/files/");
        Path path = Paths.get("src/main/resources/files/");
        Files.createDirectories(path.getParent());
        fileOutputStream = new FileOutputStream(pdfPath + "summary_"+idInventory+".pdf" );

        PdfWriter.getInstance(document, fileOutputStream);
    }

    public void openPdf(){
        document.open();

    }

    public void addTitle(String title) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        PdfPCell cell = new PdfPCell(new Phrase(title, fontTitle));
        cell.setColspan(5);
        cell.setBorderColor(BaseColor.WHITE);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        document.add(table);
    }

    public void addParagraph(String myText) throws DocumentException {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Phrase(myText, fontParagraph));
        document.add(paragraph);
    }

    public void addLineBreaks() throws DocumentException {
        Paragraph lineBreaks = new Paragraph();
        lineBreaks.add(new Phrase(Chunk.NEWLINE));
        lineBreaks.add(new Phrase(Chunk.NEWLINE));
        document.add(lineBreaks);
    }

    /** Datos generales del reporte: inventario al que pertenece y fecha en la que se genero. */
    public void addReportMetadata(Long idInventory, InventoriesEntity inventory) throws DocumentException {
        Paragraph metadata = new Paragraph();

        String inventoryLine = "Inventario: #" + idInventory;
        if (inventory != null && inventory.getPresentation() != null) {
            inventoryLine += " - " + inventory.getPresentation();
        }
        metadata.add(new Phrase(inventoryLine, fontParagraph));
        metadata.add(Chunk.NEWLINE);

        if (inventory != null && inventory.getInventoryDate() != null) {
            SimpleDateFormat inventoryDateFormat = new SimpleDateFormat("dd/MM/yyyy");
            metadata.add(new Phrase("Fecha del inventario: " + inventoryDateFormat.format(inventory.getInventoryDate()), fontParagraph));
            metadata.add(Chunk.NEWLINE);
        }

        SimpleDateFormat generatedAtFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        metadata.add(new Phrase("Fecha de generacion: " + generatedAtFormat.format(new Date()), fontParagraph));

        document.add(metadata);
    }

    public void addCountTable(List<CountsdifferenceDTO> report) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        float[] columnWidths = {1f, 3f, 1f, 1f, 1f}; // Ancho relativo de cada columna
        table.setWidths(columnWidths);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        addHeaderCell(table, "Id");
        addHeaderCell(table, "Descripcion");
        addHeaderCell(table, "Stock");
        addHeaderCell(table, "Fisico");
        addHeaderCell(table, "Diferencia");

        for(CountsdifferenceDTO counts : report){
           if(counts.difference()==null || counts.difference()!=0){
               table.addCell(dataCell(String.valueOf(counts.idProduct()), Element.ALIGN_LEFT));
               table.addCell(dataCell(String.valueOf(counts.description()), Element.ALIGN_LEFT));
               table.addCell(dataCell(formatNumber(counts.stock()), Element.ALIGN_RIGHT));
               table.addCell(dataCell(formatNumber(counts.sum()), Element.ALIGN_RIGHT));
               table.addCell(dataCell(formatNumber(counts.difference()), Element.ALIGN_RIGHT));
           }
        }

        document.add(table);
    }

    /** Totales de sobrantes, faltantes y el balance neto del conteo fisico. */
    public void addBalanceSummary(List<CountsdifferenceDTO> report) throws DocumentException {
        double totalSobrante = 0;
        double totalFaltante = 0;
        int productosConSobrante = 0;
        int productosConFaltante = 0;
        int productosExactos = 0;

        for (CountsdifferenceDTO counts : report) {
            Double difference = counts.difference();
            if (difference == null) {
                continue;
            }
            if (difference > 0) {
                totalSobrante += difference;
                productosConSobrante++;
            } else if (difference < 0) {
                totalFaltante += -difference;
                productosConFaltante++;
            } else {
                productosExactos++;
            }
        }
        double balanceNeto = totalSobrante - totalFaltante;

        addLineBreaks();

        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(60);
        summaryTable.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell titleCell = new PdfPCell(new Phrase("Balance del conteo fisico", fontSectionTitle));
        titleCell.setColspan(2);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPaddingBottom(6f);
        summaryTable.addCell(titleCell);

        addSummaryRow(summaryTable, "Productos con sobrante", productosConSobrante + "  (+" + formatNumber(totalSobrante) + ")");
        addSummaryRow(summaryTable, "Productos con faltante", productosConFaltante + "  (-" + formatNumber(totalFaltante) + ")");
        addSummaryRow(summaryTable, "Productos exactos", String.valueOf(productosExactos));
        addSummaryRow(summaryTable, "Balance neto", (balanceNeto >= 0 ? "+" : "") + formatNumber(balanceNeto));

        document.add(summaryTable);
    }

    private void addSummaryRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, fontParagraph));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(4f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, fontBold));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPaddingBottom(4f);
        table.addCell(valueCell);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, fontTableHeader));
        cell.setBackgroundColor(new BaseColor(230, 230, 230));
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private PdfPCell dataCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, fontParagraph));
        cell.setPadding(4f);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    private String formatNumber(float value) {
        return String.format(Locale.US, "%.3f", value);
    }

    private String formatNumber(Double value) {
        return value == null ? "-" : String.format(Locale.US, "%.3f", value);
    }

    private String formatNumber(double value) {
        return String.format(Locale.US, "%.3f", value);
    }

    public void closeDocument(){
        try {
            if (document != null) {
                document.close();
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
