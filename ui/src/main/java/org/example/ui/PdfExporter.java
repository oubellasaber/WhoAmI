package org.example.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility for generating PDF reports.
 * Simple PDF export for attendance analysis.
 */
public class PdfExporter {

  public static void exportReport(String title, String content, File outputFile) throws Exception {
    PdfWriter writer = new PdfWriter(outputFile);
    Document document = new Document(new com.itextpdf.kernel.pdf.PdfDocument(writer));

    document.add(new Paragraph(title)
        .setFontSize(18)
        .setBold());

    document.add(new Paragraph(" "));

    document.add(new Paragraph("Generated: " + LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        .setFontSize(10));

    document.add(new Paragraph(" "));
    document.add(new Paragraph(" "));

    for (String line : content.split("\n")) {
      document.add(new Paragraph(line).setFontSize(11));
    }

    document.close();
  }
}
