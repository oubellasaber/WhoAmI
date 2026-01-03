package org.example.testapp;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for viewing analysis history.
 * Stores and displays timestamped records of past analyses.
 */
public class HistoryController {
  private TableView<AnalysisRecord> historyTable;
  private final File historyFile = new File(System.getProperty("user.home"), ".whoami/analysis_history.txt");
  private final ObservableList<AnalysisRecord> records = FXCollections.observableArrayList();
  private Label titleLabel;
  private Button clearButton;
  private Button refreshButton;

  public Node getView() {
    VBox mainLayout = new VBox(15);
    mainLayout.setPadding(new Insets(15));
    mainLayout.getStyleClass().add("card");

    titleLabel = new Label(LanguageManager.getInstance().get("analysis_history"));
    titleLabel.getStyleClass().add("label-header");

    HBox controlBox = new HBox(10);
    
    refreshButton = new Button(LanguageManager.getInstance().get("refresh"));
    refreshButton.getStyleClass().add("button-success");
    refreshButton.setOnAction(e -> loadHistory());

    clearButton = new Button(LanguageManager.getInstance().get("clear_history"));
    clearButton.getStyleClass().add("button-danger");
    clearButton.setOnAction(e -> clearHistory());

    controlBox.getChildren().addAll(refreshButton, clearButton);

    // History table
    historyTable = new TableView<>();
    historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    historyTable.setItems(records);

    TableColumn<AnalysisRecord, String> timestampCol = new TableColumn<>("Date & Time");
    timestampCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getTimestamp()));
    timestampCol.setMinWidth(150);
    timestampCol.setPrefWidth(180);

    TableColumn<AnalysisRecord, String> detailsCol = new TableColumn<>("Analysis Summary");
    detailsCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getDetails()));
    detailsCol.setMinWidth(300);
    
    // Enable text wrapping in details column
    detailsCol.setCellFactory(tc -> {
      TableCell<AnalysisRecord, String> cell = new TableCell<>();
      Label label = new Label();
      label.setWrapText(true);
      label.setMaxWidth(Double.MAX_VALUE);
      cell.setGraphic(label);
      cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
      label.textProperty().bind(cell.itemProperty());
      return cell;
    });

    historyTable.getColumns().addAll(timestampCol, detailsCol);
    historyTable.setPlaceholder(new Label("No analysis history available"));

    mainLayout.getChildren().addAll(titleLabel, controlBox, historyTable);
    VBox.setVgrow(historyTable, Priority.ALWAYS);

    loadHistory();

    // Listen for language changes
    LanguageManager.getInstance().addLanguageChangeListener(lang -> updateLanguageTexts());
    
    ScrollPane scrollPane = new ScrollPane(mainLayout);
    scrollPane.setFitToWidth(true);
    return scrollPane;
  }

  private void loadHistory() {
    records.clear();

    try {
      if (historyFile.exists()) {
        List<String> lines = Files.readAllLines(historyFile.toPath());
        for (String line : lines) {
          if (line.startsWith("ANALYSIS|")) {
            String[] parts = line.substring(9).split("\\|", 2);
            if (parts.length >= 2) {
              AnalysisRecord record = new AnalysisRecord(parts[0], parts[1]);
              records.add(record);
            }
          }
        }
      }
    } catch (IOException ignored) {
    }
  }

  private void clearHistory() {
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle(LanguageManager.getInstance().get("clear_history"));
    confirm.setHeaderText(null);
    confirm.setContentText(LanguageManager.getInstance().get("clear_history_confirm"));
    Optional<ButtonType> result = confirm.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
      try {
        historyFile.delete();
        records.clear();
      } catch (Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(LanguageManager.getInstance().get("error"));
        alert.setContentText(LanguageManager.getInstance().get("failed_clear_history") + " " + e.getMessage());
        alert.showAndWait();
      }
    }
  }

  public void recordAnalysis(String summary) {
    try {
      historyFile.getParentFile().mkdirs();
      DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      String timestamp = LocalDateTime.now().format(fmt);
      String details = String.format("Recorded at: %s\n%s", timestamp, summary);
      try (FileWriter fw = new FileWriter(historyFile, true)) {
        fw.write("ANALYSIS|" + timestamp + "|" + summary + "\n");
      }
      loadHistory();
    } catch (IOException ignored) {
    }
  }

  private static class AnalysisRecord {
    private final String timestamp;
    private final String details;

    AnalysisRecord(String ts, String det) {
      this.timestamp = ts;
      this.details = det;
    }

    public String getTimestamp() {
      return timestamp;
    }

    public String getDetails() {
      return details;
    }
  }

  private void updateLanguageTexts() {
    LanguageManager lm = LanguageManager.getInstance();
    if (titleLabel != null)
      titleLabel.setText(lm.get("analysis_history"));
    if (clearButton != null)
      clearButton.setText(lm.get("clear_history"));
    if (refreshButton != null)
      refreshButton.setText(lm.get("refresh"));
  }
}
