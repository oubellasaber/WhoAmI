package org.example.testapp;

import org.example.testapp.attendance.AttendanceReport;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for displaying attendance analysis results.
 */
public class ResultsController {
  private TableView<AttendanceResultRow> resultsTable;
  private Label summaryLabel;
  private Label titleLabel;
  private Label searchLabel;
  private Label statusLabel;
  private Button refreshButton;
  private Button clearButton;
  private Button exportJsonButton;
  private Button exportCsvButton;
  private Button exportPdfButton;
  private Button exportExcelButton;
  private Button exportImageButton;
  private ClassroomController classroomController;
  private TextField searchField;
  private ComboBox<String> statusFilter;
  private java.util.List<AttendanceResultRow> allResults;
  // Dynamic box elements for language updates
  private Label summaryTitleLabel;
  private Label filterStatusLabel;
  private TableColumn<AttendanceResultRow, String> nameCol;
  private TableColumn<AttendanceResultRow, String> statusCol;
  private TableColumn<AttendanceResultRow, String> confidenceCol;
  private TableColumn<AttendanceResultRow, String> neighborCol;
  private TableColumn<AttendanceResultRow, String> occupancyCol;
  private TableColumn<AttendanceResultRow, String> consensusCol;

  public ResultsController(ClassroomController classroomController) {
    this.classroomController = classroomController;
    this.allResults = new java.util.ArrayList<>();
  }

  public Node getView() {
    VBox mainLayout = new VBox(15);
    mainLayout.setPadding(new Insets(20));
    mainLayout.setStyle("-fx-background-color: -fx-background;");

    // Title
    titleLabel = new Label(LanguageManager.getInstance().get("attendance_analysis_results"));
    titleLabel.getStyleClass().add("label-title");

    // Filter controls
    HBox filterBox = createFilterBox();

    // Results table
    resultsTable = createResultsTable();

    // Refresh button
    refreshButton = new Button(LanguageManager.getInstance().get("refresh_results"));
    refreshButton.getStyleClass().add("button-outline");
    refreshButton.setPrefWidth(150);
    refreshButton.setOnAction(e -> loadLatestResults());

    // Summary statistics
    VBox summaryBox = createSummaryBox();

    mainLayout.getChildren().addAll(titleLabel, filterBox, refreshButton, resultsTable, summaryBox);
    VBox.setVgrow(resultsTable, javafx.scene.layout.Priority.ALWAYS);

    // Load initial results if any
    loadLatestResults();

    // Listen for language changes to update all labels/buttons/headers live
    LanguageManager.getInstance().addLanguageChangeListener(lang -> updateLanguageTexts());
    return new ScrollPane(mainLayout);
  }

  private HBox createFilterBox() {
    HBox filterBox = new HBox(15);
    filterBox.setPadding(new Insets(15));
    filterBox.getStyleClass().add("card");
    filterBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

    searchLabel = new Label(LanguageManager.getInstance().get("search_name"));
    searchLabel.getStyleClass().add("label-header");
    
    searchField = new TextField();
    searchField.setPromptText(LanguageManager.getInstance().get("enter_student_name"));
    searchField.setPrefWidth(220);
    searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

    filterStatusLabel = new Label(LanguageManager.getInstance().get("filter_status"));
    filterStatusLabel.getStyleClass().add("label-header");
    
    statusFilter = new ComboBox<>();
    statusFilter.getItems().addAll(LanguageManager.getInstance().get("all"), "PRESENT", "ABSENT", "UNCERTAIN");
    statusFilter.setValue(LanguageManager.getInstance().get("all"));
    statusFilter.setPrefWidth(140);
    statusFilter.setOnAction(e -> applyFilters());

    clearButton = new Button(LanguageManager.getInstance().get("clear_filters"));
    clearButton.getStyleClass().add("button-secondary");
    clearButton.setOnAction(e -> {
      searchField.clear();
      statusFilter.setValue(LanguageManager.getInstance().get("all"));
    });

    filterBox.getChildren().addAll(searchLabel, searchField, filterStatusLabel, statusFilter, clearButton);
    return filterBox;
  }

  private void applyFilters() {
    String searchText = searchField.getText().toLowerCase().trim();
    String statusText = statusFilter.getValue();

    java.util.List<AttendanceResultRow> filteredResults = allResults.stream()
        .filter(row -> {
          boolean nameMatch = searchText.isEmpty() || row.studentName.toLowerCase().contains(searchText);
          String allLabel = LanguageManager.getInstance().get("all");
          boolean statusMatch = statusText.equals(allLabel) || row.status.equals(statusText);
          return nameMatch && statusMatch;
        })
        .toList();

    resultsTable.getItems().setAll(filteredResults);
  }

  private void loadLatestResults() {
    try {
      if (classroomController != null && classroomController.getAttendanceService() != null) {
        AttendanceService service = classroomController.getAttendanceService();
        AttendanceService.AttendanceAnalysisResult result = service.getLastResult();

        if (result == null) {
          summaryLabel.setText(LanguageManager.getInstance().get("no_analysis_results"));
          resultsTable.getItems().clear();
          return;
        }

        // Convert reports to table data
        java.util.List<AttendanceResultRow> rows = new java.util.ArrayList<>();
        for (AttendanceReport report : result.reports) {
          String status = report.getStatus().name();
          // Map report status to UI status
          String uiStatus = switch (report.getStatus()) {
            case PRESENT -> "PRESENT";
            case ABSENT -> "ABSENT";
            default -> "UNCERTAIN";
          };

          // Parse individual strategy scores from reason string
          double neighborScore = extractScore(report.getReason(), "NeighborVerification");
          double occupancyScore = extractScore(report.getReason(), "SeatOccupancy");
          double consensusScore = extractScore(report.getReason(), "ConsensusScore");

          rows.add(new AttendanceResultRow(
              report.getStudent().getName(),
              uiStatus,
              report.getConfidenceScore(),
              neighborScore,
              occupancyScore,
              consensusScore));
        }

        // Store all results and apply filters
        allResults = rows;
        applyFilters();
        updateSummary(result.reports);
      }
    } catch (Exception e) {
      summaryLabel.setText(LanguageManager.getInstance().get("no_analysis_results"));
    }
  }

  private TableView<AttendanceResultRow> createResultsTable() {
    resultsTable = new TableView<>();
    resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    resultsTable.setPrefHeight(400);

    // Name column
    nameCol = new TableColumn<>(LanguageManager.getInstance().get("student_name_col"));
    nameCol.setCellValueFactory(cellData -> javafx.beans.binding.Bindings.createStringBinding(
        () -> cellData.getValue().studentName));
    nameCol.setPrefWidth(120);

    // Status column
    statusCol = new TableColumn<>(LanguageManager.getInstance().get("status_col"));
    statusCol.setCellValueFactory(cellData -> javafx.beans.binding.Bindings.createStringBinding(
        () -> cellData.getValue().status));
    statusCol.setPrefWidth(100);
    statusCol.setCellFactory(column -> new StatusCell());

    // Confidence column
    confidenceCol = new TableColumn<>(LanguageManager.getInstance().get("confidence_col"));
    confidenceCol.setCellValueFactory(cellData -> javafx.beans.binding.Bindings.createStringBinding(
        () -> String.format("%.2f%%", cellData.getValue().confidence * 100)));
    confidenceCol.setPrefWidth(100);

    // Neighbor Verification column
    neighborCol = new TableColumn<>(LanguageManager.getInstance().get("neighbor_col"));
    neighborCol.setCellValueFactory(cellData -> javafx.beans.binding.Bindings.createStringBinding(
        () -> String.format("%.2f", cellData.getValue().neighborScore)));
    neighborCol.setPrefWidth(80);

    // Seat Occupancy column
    occupancyCol = new TableColumn<>(LanguageManager.getInstance().get("occupancy_col"));
    occupancyCol.setCellValueFactory(cellData -> javafx.beans.binding.Bindings.createStringBinding(
        () -> String.format("%.2f", cellData.getValue().occupancyScore)));
    occupancyCol.setPrefWidth(80);

    // Consensus column
    consensusCol = new TableColumn<>(LanguageManager.getInstance().get("consensus_col"));
    consensusCol.setCellValueFactory(cellData -> javafx.beans.binding.Bindings.createStringBinding(
        () -> String.format("%.2f", cellData.getValue().consensusScore)));
    consensusCol.setPrefWidth(80);

    resultsTable.getColumns().addAll(nameCol, statusCol, confidenceCol, neighborCol, occupancyCol, consensusCol);

    return resultsTable;
  }

  private VBox createSummaryBox() {
    VBox summaryBox = new VBox(12);
    summaryBox.setPadding(new Insets(15));
    summaryBox.getStyleClass().add("card");

    summaryTitleLabel = new Label(LanguageManager.getInstance().get("summary_statistics"));
    summaryTitleLabel.getStyleClass().add("label-subtitle");

    summaryLabel = new Label(LanguageManager.getInstance().get("no_results_yet"));
    summaryLabel.getStyleClass().add("label-secondary");
    summaryLabel.setWrapText(true);

    Label exportLabel = new Label("Export Options:");
    exportLabel.getStyleClass().add("label-header");
    
    HBox exportBox = new HBox(10);
    exportBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    exportBox.setPadding(new Insets(10, 0, 0, 0));

    exportJsonButton = new Button(LanguageManager.getInstance().get("export_json"));
    exportJsonButton.getStyleClass().add("button-success");
    exportJsonButton.setPrefWidth(110);
    exportJsonButton.setOnAction(e -> exportResults("json"));

    exportCsvButton = new Button(LanguageManager.getInstance().get("export_csv"));
    exportCsvButton.getStyleClass().add("button-success");
    exportCsvButton.setPrefWidth(110);
    exportCsvButton.setOnAction(e -> exportResults("csv"));

    exportPdfButton = new Button(LanguageManager.getInstance().get("export_pdf"));
    exportPdfButton.getStyleClass().add("button-success");
    exportPdfButton.setPrefWidth(110);
    exportPdfButton.setOnAction(e -> exportResults("pdf"));

    exportExcelButton = new Button(LanguageManager.getInstance().get("export_excel"));
    exportExcelButton.getStyleClass().add("button-success");
    exportExcelButton.setPrefWidth(110);
    exportExcelButton.setOnAction(e -> exportResults("xlsx"));

    exportImageButton = new Button(LanguageManager.getInstance().get("export_image"));
    exportImageButton.getStyleClass().add("button-success");
    exportImageButton.setPrefWidth(110);
    exportImageButton.setOnAction(e -> exportResults("png"));

    exportBox.getChildren().addAll(exportJsonButton, exportCsvButton, exportPdfButton, exportExcelButton,
        exportImageButton);

    summaryBox.getChildren().addAll(summaryTitleLabel, summaryLabel, new Separator(), exportLabel, exportBox);

    return summaryBox;
  }

  private void exportResults(String format) {
    try {
      AttendanceService service = classroomController.getAttendanceService();
      AttendanceService.AttendanceAnalysisResult result = service.getLastResult();

      if (result == null || result.reports.isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "No Data", "No analysis results to export");
        return;
      }

      // Create downloads directory if it doesn't exist
      String userHome = System.getProperty("user.home");
      File downloadsDir = new File(userHome, "Downloads");
      if (!downloadsDir.exists()) {
        downloadsDir.mkdirs();
      }

      // Generate filename with timestamp
      LocalDateTime now = LocalDateTime.now();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
      String extension = format.equals("csv") ? "csv"
          : (format.equals("pdf") ? "pdf" : (format.equals("xlsx") ? "xlsx" : (format.equals("png") ? "png" : "json")));
      String filename = "attendance_results_" + now.format(formatter) + "." + extension;
      File outputFile = new File(downloadsDir, filename);

      if (format.equals("csv")) {
        exportToCSV(outputFile, result);
      } else if (format.equals("pdf")) {
        exportToPDF(outputFile, result);
      } else if (format.equals("xlsx")) {
        exportToExcel(outputFile, result);
      } else if (format.equals("png")) {
        exportToImage(outputFile, result);
      } else {
        exportToJSON(outputFile, result);
      }

      showAlert(Alert.AlertType.INFORMATION, "Export Success",
          "Results exported to:\n" + outputFile.getAbsolutePath());
    } catch (IOException e) {
      showAlert(Alert.AlertType.ERROR, "Export Failed", "Error: " + e.getMessage());
    } catch (Exception e) {
      showAlert(Alert.AlertType.ERROR, "Export Failed", "Unexpected error: " + e.getMessage());
    }
  }

  private void exportToCSV(File outputFile, AttendanceService.AttendanceAnalysisResult result) throws IOException {
    try (FileWriter writer = new FileWriter(outputFile)) {
      // Write headers
      writer.write("Student Name,Status,Confidence %,Neighbor Score,Occupancy Score,Consensus Score\n");

      // Write data rows
      for (AttendanceReport report : result.reports) {
        double neighborScore = extractScore(report.getReason(), "NeighborVerification");
        double occupancyScore = extractScore(report.getReason(), "SeatOccupancy");
        double consensusScore = extractScore(report.getReason(), "ConsensusScore");

        writer.write(String.format(
            "%s,%s,%.2f%%,%.4f,%.4f,%.4f\n",
            escapeCsv(report.getStudent().getName()),
            report.getStatus().name(),
            report.getConfidenceScore() * 100,
            neighborScore,
            occupancyScore,
            consensusScore));
      }
    }
  }

  private void exportToJSON(File outputFile, AttendanceService.AttendanceAnalysisResult result) throws IOException {
    StringBuilder json = new StringBuilder();
    json.append("{\n");
    json.append("  \"exportTime\": \"").append(LocalDateTime.now()).append("\",\n");
    json.append("  \"classroomDimensions\": {\n");
    json.append("    \"rows\": ").append(classroomController.getClassroom().getRows()).append(",\n");
    json.append("    \"columns\": ").append(classroomController.getClassroom().getCols()).append("\n");
    json.append("  },\n");

    // Summary statistics
    long presentCount = result.reports.stream()
        .filter(r -> r.getStatus().equals(AttendanceReport.AttendanceStatus.PRESENT))
        .count();
    long absentCount = result.reports.stream()
        .filter(r -> r.getStatus().equals(AttendanceReport.AttendanceStatus.ABSENT))
        .count();
    long uncertainCount = result.reports.stream()
        .filter(r -> r.getStatus().equals(AttendanceReport.AttendanceStatus.UNCERTAIN))
        .count();
    double avgConfidence = result.reports.stream()
        .mapToDouble(AttendanceReport::getConfidenceScore)
        .average()
        .orElse(0.0);

    json.append("  \"summary\": {\n");
    json.append("    \"totalStudents\": ").append(result.reports.size()).append(",\n");
    json.append("    \"present\": ").append(presentCount).append(",\n");
    json.append("    \"absent\": ").append(absentCount).append(",\n");
    json.append("    \"uncertain\": ").append(uncertainCount).append(",\n");
    json.append("    \"averageConfidence\": ").append(String.format("%.4f", avgConfidence)).append("\n");
    json.append("  },\n");

    // Student details
    json.append("  \"results\": [\n");
    for (int i = 0; i < result.reports.size(); i++) {
      AttendanceReport report = result.reports.get(i);

      double neighborScore = extractScore(report.getReason(), "NeighborVerification");
      double occupancyScore = extractScore(report.getReason(), "SeatOccupancy");
      double consensusScore = extractScore(report.getReason(), "ConsensusScore");

      json.append("    {\n");
      json.append("      \"name\": \"").append(escapeJson(report.getStudent().getName())).append("\",\n");
      json.append("      \"status\": \"").append(report.getStatus().name()).append("\",\n");
      json.append("      \"confidence\": ").append(String.format("%.4f", report.getConfidenceScore())).append(",\n");
      json.append("      \"neighborScore\": ").append(String.format("%.4f", neighborScore)).append(",\n");
      json.append("      \"occupancyScore\": ").append(String.format("%.4f", occupancyScore)).append(",\n");
      json.append("      \"consensusScore\": ").append(String.format("%.4f", consensusScore)).append("\n");
      json.append("    }");

      if (i < result.reports.size() - 1) {
        json.append(",");
      }
      json.append("\n");
    }
    json.append("  ]\n");
    json.append("}\n");

    // Write to file
    try (FileWriter writer = new FileWriter(outputFile)) {
      writer.write(json.toString());
    }
  }

  private String escapeCsv(String field) {
    if (field == null) {
      return "";
    }
    // Escape CSV by wrapping in quotes if field contains comma, quote, or newline
    if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
      return "\"" + field.replace("\"", "\"\"") + "\"";
    }
    return field;
  }

  private void showAlert(Alert.AlertType type, String title, String message) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  private String escapeJson(String str) {
    if (str == null) {
      return "";
    }
    return str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  private void updateSummary(java.util.List<AttendanceReport> reports) {
    if (reports == null || reports.isEmpty()) {
      summaryLabel.setText("No results available");
      return;
    }

    long presentCount = reports.stream()
        .filter(r -> r.getStatus().equals(AttendanceReport.AttendanceStatus.PRESENT))
        .count();
    long absentCount = reports.stream()
        .filter(r -> r.getStatus().equals(AttendanceReport.AttendanceStatus.ABSENT))
        .count();
    long uncertainCount = reports.stream()
        .filter(r -> r.getStatus().equals(AttendanceReport.AttendanceStatus.UNCERTAIN))
        .count();
    double avgConfidence = reports.stream()
        .mapToDouble(AttendanceReport::getConfidenceScore)
        .average()
        .orElse(0.0);

    int total = reports.size();
    double presentPercent = (presentCount * 100.0) / total;
    double absentPercent = (absentCount * 100.0) / total;
    double uncertainPercent = (uncertainCount * 100.0) / total;

    String summary = String.format(
        "Total Students: %d\n" +
            "Present: %d (%.1f%%)\n" +
            "Absent: %d (%.1f%%)\n" +
            "Uncertain: %d (%.1f%%)\n" +
            "Average Confidence: %.2f",
        total, presentCount, presentPercent,
        absentCount, absentPercent,
        uncertainCount, uncertainPercent,
        avgConfidence);

    summaryLabel.setText(summary);
  }

  private static class StatusCell extends TableCell<AttendanceResultRow, String> {
    @Override
    protected void updateItem(String item, boolean empty) {
      super.updateItem(item, empty);

      if (empty || getTableRow().getItem() == null) {
        setText(null);
        setStyle("");
      } else {
        AttendanceResultRow row = getTableRow().getItem();
        setText(row.status);

        switch (row.status) {
          case "PRESENT" -> setStyle("-fx-text-fill: #00aa00; -fx-font-weight: bold;");
          case "ABSENT" -> setStyle("-fx-text-fill: #cc0000; -fx-font-weight: bold;");
          case "UNCERTAIN" -> setStyle("-fx-text-fill: #ff8800; -fx-font-weight: bold;");
          default -> setStyle("");
        }
      }
    }
  }

  public void updateResults(java.util.List<AttendanceReport> reports) {
    resultsTable.getItems().clear();

    long present = 0;
    long absent = 0;
    long uncertain = 0;
    double totalConfidence = 0;

    for (AttendanceReport report : reports) {
      AttendanceResultRow row = new AttendanceResultRow(
          report.getStudent().getName(),
          report.getStatus().toString(),
          report.getConfidenceScore(),
          0.0, 0.0, 0.0 // These would be extracted from report
      );

      resultsTable.getItems().add(row);

      totalConfidence += report.getConfidenceScore();
      switch (report.getStatus()) {
        case PRESENT -> present++;
        case ABSENT -> absent++;
        case UNCERTAIN -> uncertain++;
      }
    }

    // Update summary
    int total = (int) (present + absent + uncertain);
    double avgConfidence = total > 0 ? totalConfidence / total : 0;

    String summary = String.format(
        "Total: %d | Present: %d (%.1f%%) | Absent: %d (%.1f%%) | Uncertain: %d (%.1f%%) | Avg Confidence: %.2f%%",
        total, present, 100.0 * present / total,
        absent, 100.0 * absent / total,
        uncertain, 100.0 * uncertain / total,
        avgConfidence * 100);

    summaryLabel.setText(summary);
  }

  static class AttendanceResultRow {
    String studentName;
    String status;
    double confidence;
    double neighborScore;
    double occupancyScore;
    double consensusScore;

    AttendanceResultRow(String studentName, String status, double confidence,
        double neighborScore, double occupancyScore, double consensusScore) {
      this.studentName = studentName;
      this.status = status;
      this.confidence = confidence;
      this.neighborScore = neighborScore;
      this.occupancyScore = occupancyScore;
      this.consensusScore = consensusScore;
    }
  }

  private void exportToPDF(File outputFile, AttendanceService.AttendanceAnalysisResult result) throws Exception {
    StringBuilder pdfContent = new StringBuilder();
    pdfContent.append("ATTENDANCE ANALYSIS REPORT\n\n");

    // Summary
    long presentCount = result.reports.stream()
        .filter(r -> r.getStatus().equals(AttendanceReport.AttendanceStatus.PRESENT))
        .count();
    long absentCount = result.reports.stream()
        .filter(r -> r.getStatus().equals(AttendanceReport.AttendanceStatus.ABSENT))
        .count();
    long uncertainCount = result.reports.stream()
        .filter(r -> r.getStatus().equals(AttendanceReport.AttendanceStatus.UNCERTAIN))
        .count();

    pdfContent.append("SUMMARY\n");
    pdfContent.append("Total Students: ").append(result.reports.size()).append("\n");
    pdfContent.append("Present: ").append(presentCount).append("\n");
    pdfContent.append("Absent: ").append(absentCount).append("\n");
    pdfContent.append("Uncertain: ").append(uncertainCount).append("\n");
    pdfContent.append("Conflicts Detected: ").append(result.conflicts.size()).append("\n\n");

    // Student details
    pdfContent.append("STUDENT RESULTS\n");
    pdfContent.append("-".repeat(80)).append("\n");
    for (AttendanceReport report : result.reports) {
      pdfContent.append(report.getStudent().getName())
          .append(" - ").append(report.getStatus())
          .append(" (").append(String.format("%.2f%%", report.getConfidenceScore() * 100)).append(")\n");
    }

    PdfExporter.exportReport("Attendance Analysis Report", pdfContent.toString(), outputFile);
  }

  /**
   * Extract individual strategy score from reason string.
   * Example reason: "NeighborVerification: 0.50 | SeatOccupancy: 0.95 |
   * ConsensusScore: 0.50"
   */
  private double extractScore(String reason, String strategyName) {
    if (reason == null || reason.isEmpty()) {
      return 0.0;
    }

    int startIdx = reason.indexOf(strategyName + ": ");
    if (startIdx == -1) {
      return 0.0;
    }

    startIdx += strategyName.length() + 2; // Move past ": "
    int endIdx = reason.indexOf(" |", startIdx);
    if (endIdx == -1) {
      endIdx = reason.length();
    }

    try {
      String scoreStr = reason.substring(startIdx, endIdx).trim();
      return Double.parseDouble(scoreStr);
    } catch (NumberFormatException e) {
      return 0.0;
    }
  }

  private void exportToExcel(File outputFile, AttendanceService.AttendanceAnalysisResult result) throws Exception {
    java.util.List<String> data = new java.util.ArrayList<>();
    data.add("Student Name | Student ID | Status | Reason");

    for (AttendanceReport report : result.reports) {
      String line = report.getStudent().getName() + " | " + report.getStudent().getId() + " | " +
          report.getStatus() + " | " +
          report.getReason();
      data.add(line);
    }

    ExcelExporter.exportReport(outputFile.getAbsolutePath(), "ATTENDANCE ANALYSIS REPORT", data);
  }

  private void exportToImage(File outputFile, AttendanceService.AttendanceAnalysisResult result) throws Exception {
    if (resultsTable == null || resultsTable.getScene() == null) {
      throw new Exception("Results table not visible");
    }

    javafx.scene.image.WritableImage image = resultsTable.snapshot(new javafx.scene.SnapshotParameters(), null);

    // Convert WritableImage to BufferedImage properly
    int width = (int) image.getWidth();
    int height = (int) image.getHeight();
    java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(
        width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);

    javafx.scene.image.PixelReader pixelReader = image.getPixelReader();
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        bufferedImage.setRGB(x, y, pixelReader.getArgb(x, y));
      }
    }

    javax.imageio.ImageIO.write(bufferedImage, "png", outputFile);
  }

  private void updateLanguageTexts() {
    LanguageManager lm = LanguageManager.getInstance();
    // Title
    if (titleLabel != null)
      titleLabel.setText(lm.get("attendance_analysis_results"));
    // Filter box labels
    if (searchLabel != null)
      searchLabel.setText(lm.get("search_name"));
    if (searchField != null)
      searchField.setPromptText(lm.get("enter_student_name"));
    if (filterStatusLabel != null)
      filterStatusLabel.setText(lm.get("filter_status"));
    if (clearButton != null)
      clearButton.setText(lm.get("clear_filters"));
    // Refresh button
    if (refreshButton != null)
      refreshButton.setText(lm.get("refresh_results"));
    // Export buttons
    if (exportJsonButton != null)
      exportJsonButton.setText(lm.get("export_json"));
    if (exportCsvButton != null)
      exportCsvButton.setText(lm.get("export_csv"));
    if (exportPdfButton != null)
      exportPdfButton.setText(lm.get("export_pdf"));
    if (exportExcelButton != null)
      exportExcelButton.setText(lm.get("export_excel"));
    if (exportImageButton != null)
      exportImageButton.setText(lm.get("export_image"));
    // Summary section
    if (summaryTitleLabel != null)
      summaryTitleLabel.setText(lm.get("summary_statistics"));
    // Table column headers
    if (nameCol != null)
      nameCol.setText(lm.get("student_name_col"));
    if (statusCol != null)
      statusCol.setText(lm.get("status_col"));
    if (confidenceCol != null)
      confidenceCol.setText(lm.get("confidence_col"));
    if (neighborCol != null)
      neighborCol.setText(lm.get("neighbor_col"));
    if (occupancyCol != null)
      occupancyCol.setText(lm.get("occupancy_col"));
    if (consensusCol != null)
      consensusCol.setText(lm.get("consensus_col"));
    // Update status filter items with localized "All"
    String allLabel = lm.get("all");
    if (statusFilter != null && statusFilter.getItems() != null && !statusFilter.getItems().isEmpty()) {
      statusFilter.getItems().set(0, allLabel);
      if (statusFilter.getValue().equals(allLabel) || statusFilter.getValue().equals("All")) {
        statusFilter.setValue(allLabel);
      }
    }
  }
}
