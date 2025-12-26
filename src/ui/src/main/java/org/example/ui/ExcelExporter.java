package org.example.ui;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelExporter {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public static void exportReport(String filePath, String title, List<String> data) throws IOException {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Analysis Results");

      // Title
      Row titleRow = sheet.createRow(0);
      Cell titleCell = titleRow.createCell(0);
      titleCell.setCellValue(title);
      CellStyle titleStyle = workbook.createCellStyle();
      Font titleFont = workbook.createFont();
      titleFont.setBold(true);
      titleFont.setFontHeightInPoints((short) 14);
      titleStyle.setFont(titleFont);
      titleCell.setCellStyle(titleStyle);

      // Timestamp
      Row tsRow = sheet.createRow(1);
      Cell tsCell = tsRow.createCell(0);
      tsCell.setCellValue("Generated: " + LocalDateTime.now().format(FORMATTER));
      CellStyle tsStyle = workbook.createCellStyle();
      Font tsFont = workbook.createFont();
      tsFont.setItalic(true);
      tsStyle.setFont(tsFont);
      tsCell.setCellStyle(tsStyle);

      // Data
      int rowNum = 3;
      CellStyle dataStyle = workbook.createCellStyle();
      dataStyle.setWrapText(true);
      dataStyle.setVerticalAlignment(VerticalAlignment.TOP);

      for (String line : data) {
        Row row = sheet.createRow(rowNum++);
        Cell cell = row.createCell(0);
        cell.setCellValue(line);
        cell.setCellStyle(dataStyle);
      }

      // Auto-size column
      sheet.setColumnWidth(0, 50 * 256);

      // Write to file
      try (FileOutputStream fos = new FileOutputStream(filePath)) {
        workbook.write(fos);
      }
    } catch (IOException e) {
      throw new IOException("Failed to export Excel report: " + e.getMessage(), e);
    }
  }
}
