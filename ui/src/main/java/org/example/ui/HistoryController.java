package org.example.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for viewing analysis history.
 * Stores and displays timestamped records of past analyses.
 */
public class HistoryController {
  private ListView<String> historyListView;
  private TextArea detailsArea;
  private final File historyFile = new File(System.getProperty("user.home"), ".whoami/analysis_history.txt");
  private final List<AnalysisRecord> records = new ArrayList<>();
  private Label titleLabel;
  private Label detailsLabel;
  private Label pastAnalysesLabel;
  private Button clearButton;
  private Button refreshButton;

  public Node getView() {
    VBox mainLayout = new VBox(10);
    mainLayout.setPadding(new Insets(15));

    titleLabel = new Label(LanguageManager.getInstance().get("analysis_history"));
    titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

    HBox controlBox = new HBox(10);
    clearButton = new Button(LanguageManager.getInstance().get("clear_history"));
    clearButton.setPrefWidth(120);
    clearButton.setStyle("-fx-font-size: 11; -fx-padding: 8;");
    clearButton.setOnAction(e -> clearHistory());

    refreshButton = new Button(LanguageManager.getInstance().get("refresh"));
    refreshButton.setPrefWidth(100);
    refreshButton.setStyle("-fx-font-size: 11; -fx-padding: 8;");
    refreshButton.setOnAction(e -> loadHistory());

    controlBox.getChildren().addAll(refreshButton, clearButton);

    // History list
    historyListView = new ListView<>();
    historyListView.setPrefHeight(300);
    historyListView.setOnMouseClicked(e -> {
      if (historyListView.getSelectionModel().getSelectedItem() != null) {
        showDetails();
      }
    });

    detailsLabel = new Label(LanguageManager.getInstance().get("details"));
    detailsLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold;");

    detailsArea = new TextArea();
    detailsArea.setPrefHeight(200);
    detailsArea.setWrapText(true);
    detailsArea.setEditable(false);

    mainLayout.getChildren().addAll(
        titleLabel,
        controlBox,
        (pastAnalysesLabel = new Label(LanguageManager.getInstance().get("past_analyses"))),
        historyListView,
        detailsLabel,
        detailsArea);

    VBox.setVgrow(historyListView, Priority.ALWAYS);
    VBox.setVgrow(detailsArea, Priority.SOMETIMES);

    loadHistory();

    // Listen for language changes
    LanguageManager.getInstance().addLanguageChangeListener(lang -> updateLanguageTexts());
    return new ScrollPane(mainLayout);
  }

  private void loadHistory() {
    records.clear();
    historyListView.getItems().clear();

    try {
      if (historyFile.exists()) {
        List<String> lines = Files.readAllLines(historyFile.toPath());
        for (String line : lines) {
          if (line.startsWith("ANALYSIS|")) {
            String[] parts = line.substring(9).split("\\|");
            if (parts.length >= 2) {
              AnalysisRecord record = new AnalysisRecord(parts[0], parts[1]);
              records.add(record);
              historyListView.getItems().add(parts[0] + " — " + parts[1]);
            }
          }
        }
      }
    } catch (IOException ignored) {
    }
  }

  private void showDetails() {
    int idx = historyListView.getSelectionModel().getSelectedIndex();
    if (idx >= 0 && idx < records.size()) {
      detailsArea.setText(records.get(idx).details);
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
        historyListView.getItems().clear();
        detailsArea.clear();
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
    String timestamp;
    String details;

    AnalysisRecord(String ts, String det) {
      this.timestamp = ts;
      this.details = det;
    }
  }

  private void updateLanguageTexts() {
    LanguageManager lm = LanguageManager.getInstance();
    if (titleLabel != null)
      titleLabel.setText(lm.get("analysis_history"));
    if (detailsLabel != null)
      detailsLabel.setText(lm.get("details"));
    if (pastAnalysesLabel != null)
      pastAnalysesLabel.setText(lm.get("past_analyses"));
    if (clearButton != null)
      clearButton.setText(lm.get("clear_history"));
    if (refreshButton != null)
      refreshButton.setText(lm.get("refresh"));
  }
}
