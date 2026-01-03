package org.example.testapp;

import org.example.testapp.attendance.AttendanceReport;
import org.example.testapp.entities.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class StatsController {
  private VBox root;
  private Label titleLabel;
  private Label totalStudentsLabel;
  private Label presentLabel;
  private Label absentLabel;
  private Label attendanceRateLabel;
  private PieChart attendanceChart;
  private Label emptyStateLabel;

  public StatsController() {
    buildUI();
    // Listen for language changes
    LanguageManager.getInstance().addLanguageChangeListener(lang -> updateLabels());
  }

  private void buildUI() {
    root = new VBox(15);
    root.getStyleClass().add("card");
    root.setPrefWidth(800);
    root.setPrefHeight(600);
    root.setMinHeight(600);

    // Title
    titleLabel = new Label(LanguageManager.getInstance().get("attendance_statistics"));
    titleLabel.getStyleClass().add("label-header");

    // Stats summary box
    HBox statsBox = new HBox(30);
    statsBox.getStyleClass().add("card");
    statsBox.setAlignment(Pos.CENTER_LEFT);
    statsBox.setPrefHeight(100);

    totalStudentsLabel = new Label("Total Students: 0");
    totalStudentsLabel.getStyleClass().add("label-title");

    presentLabel = new Label("Present: 0");
    presentLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2ecc71;");

    absentLabel = new Label("Absent: 0");
    absentLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");

    attendanceRateLabel = new Label("Attendance Rate: 0%");
    attendanceRateLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #3498db;");

    statsBox.getChildren().addAll(totalStudentsLabel, presentLabel, absentLabel, attendanceRateLabel);

    // Empty state message shown when no analysis is available
    emptyStateLabel = new Label(LanguageManager.getInstance().get("no_stats"));
    emptyStateLabel.getStyleClass().add("label-secondary");

    // Pie chart
    attendanceChart = new PieChart();
    attendanceChart.setTitle(LanguageManager.getInstance().get("chart_title"));
    attendanceChart.setMinHeight(300);
    attendanceChart.setPrefHeight(400);

    // Create chart container
    VBox chartContainer = new VBox();
    chartContainer.getStyleClass().add("card");
    chartContainer.getChildren().add(attendanceChart);
    VBox.setVgrow(attendanceChart, Priority.ALWAYS);

    Label chartLabel = new Label("Chart:");
    chartLabel.getStyleClass().add("label-title");

    root.getChildren().addAll(
        titleLabel,
        statsBox,
        emptyStateLabel,
        chartLabel,
        chartContainer);
    VBox.setVgrow(chartContainer, Priority.ALWAYS);
  }

  public void updateStats(Classroom classroom, AttendanceService attendanceService) {
    System.out.println("[DEBUG] StatsController.updateStats() called with classroom=" + (classroom != null)
        + ", service=" + (attendanceService != null));

    if (classroom == null || classroom.allPlacements().isEmpty()) {
      System.out.println("[DEBUG] Classroom is null or empty, resetting stats");
      totalStudentsLabel.setText("Total Students: 0");
      presentLabel.setText("Present: 0");
      absentLabel.setText("Absent: 0");
      attendanceRateLabel.setText("Attendance Rate: 0%");
      attendanceChart.getData().clear();
      emptyStateLabel.setVisible(true);
      return;
    }

    List<LocatedStudent> students = new ArrayList<>(classroom.allPlacements().values());
    int total = students.size();
    int present = 0;
    int absent = 0;

    AttendanceService.AttendanceAnalysisResult result = attendanceService.getLastResult();
    System.out.println("[DEBUG] Analysis result: " + (result != null) + ", total students: " + total);

    if (result != null) {
      for (AttendanceReport report : result.reports) {
        if (report.getStatus() == AttendanceReport.AttendanceStatus.PRESENT)
          present++;
        else if (report.getStatus() == AttendanceReport.AttendanceStatus.ABSENT)
          absent++;
      }
    }

    System.out.println("[DEBUG] Stats: total=" + total + ", present=" + present + ", absent=" + absent);
    int rate = total > 0 ? (present * 100) / total : 0;

    totalStudentsLabel.setText("Total Students: " + total);
    presentLabel.setText("Present: " + present);
    absentLabel.setText("Absent: " + absent);
    attendanceRateLabel.setText("Attendance Rate: " + rate + "%");
    boolean hasData = (result != null) && (present > 0 || absent > 0);
    emptyStateLabel.setText(hasData ? "" : "No statistics yet. Run analysis to populate.");
    emptyStateLabel.setVisible(!hasData);

    // Update pie chart - only add data if present + absent > 0
    attendanceChart.getData().clear();
    if (present > 0 || absent > 0) {
      System.out.println("[DEBUG] Creating pie chart with present=" + present + ", absent=" + absent);
      ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();
      if (present > 0) {
        chartData.add(new PieChart.Data("Present", present));
      }
      if (absent > 0) {
        chartData.add(new PieChart.Data("Absent", absent));
      }

      attendanceChart.setData(chartData);

      // Color the pie slices after nodes are created
      javafx.application.Platform.runLater(() -> {
        for (PieChart.Data data : chartData) {
          if (data.getName().equalsIgnoreCase("Present") && data.getNode() != null) {
            data.getNode().setStyle("-fx-pie-color: #2ecc71;");
          } else if (data.getName().equalsIgnoreCase("Absent") && data.getNode() != null) {
            data.getNode().setStyle("-fx-pie-color: #e74c3c;");
          }
        }
      });
    } else {
      System.out.println("[DEBUG] No present/absent data to display");
    }
  }

  public VBox getRoot() {
    return root;
  }

  public ScrollPane getScrollableRoot() {
    ScrollPane scrollPane = new ScrollPane(root);
    scrollPane.setFitToWidth(true);
    return scrollPane;
  }

  private void updateLabels() {
    LanguageManager lm = LanguageManager.getInstance();
    // Update chart title
    attendanceChart.setTitle(lm.get("chart_title"));
    // Update other static labels
    titleLabel.setText(lm.get("attendance_statistics"));
    emptyStateLabel.setText(lm.get("no_stats"));
  }
}
