package org.example.testapp;

import org.example.testapp.entities.Classroom;
import org.example.testapp.entities.Student;
import org.example.testapp.verification.AttendanceConflict;
import org.example.testapp.verification.ConflictDetector;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * Controller for analyzing and displaying attendance conflicts.
 * Shows which students are making false claims or lying about their neighbors.
 */
public class ConflictAnalysisController {
  private ClassroomController classroomController;
  private TableView<AttendanceConflict> conflictTable;
  private TableView<Student> suspiciousTable;
  private Label summaryLabel;
  private Label titleLabel;
  private Button analyzeButton;
  private Label conflictsLabel;
  private Label suspiciousLabel;

  public ConflictAnalysisController(ClassroomController classroomController) {
    this.classroomController = classroomController;
  }

  public Node getView() {
    VBox mainLayout = new VBox(15);
    mainLayout.setPadding(new Insets(15));
    mainLayout.getStyleClass().add("card");

    // Title
    titleLabel = new Label(LanguageManager.getInstance().get("conflict_title"));
    titleLabel.getStyleClass().add("label-header");

    // Analyze button
    analyzeButton = new Button(LanguageManager.getInstance().get("analyze_conflicts"));
    analyzeButton.getStyleClass().add("button-success");
    analyzeButton.setOnAction(e -> analyzeConflicts());

    // Summary
    summaryLabel = new Label(LanguageManager.getInstance().get("conflict_prompt"));
    summaryLabel.setWrapText(true);

    // Conflicts Table
    conflictsLabel = new Label(LanguageManager.getInstance().get("conflict_detailed"));
    conflictsLabel.getStyleClass().add("label-title");
    
    conflictTable = new TableView<>();
    conflictTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    
    TableColumn<AttendanceConflict, String> studentCol = new TableColumn<>(
        LanguageManager.getInstance().get("student_name"));
    studentCol.setCellValueFactory(cellData -> 
        new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStudent().getName()));
    studentCol.setMinWidth(120);
    
    TableColumn<AttendanceConflict, String> involvedCol = new TableColumn<>("Involved Student");
    involvedCol.setCellValueFactory(cellData -> 
        new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().getInvolvedStudent() != null ? 
            cellData.getValue().getInvolvedStudent().getName() : "N/A"));
    involvedCol.setMinWidth(120);
    
    TableColumn<AttendanceConflict, String> typeCol = new TableColumn<>("Conflict Type");
    typeCol.setCellValueFactory(cellData -> 
        new javafx.beans.property.SimpleStringProperty(cellData.getValue().getType().toString()));
    typeCol.setMinWidth(150);
    
    TableColumn<AttendanceConflict, String> descCol = new TableColumn<>("Description");
    descCol.setCellValueFactory(cellData -> 
        new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescription()));
    descCol.setMinWidth(250);
    descCol.setPrefWidth(300);
    
    conflictTable.getColumns().addAll(studentCol, involvedCol, typeCol, descCol);
    conflictTable.setPlaceholder(new Label("No conflicts detected"));

    // Suspicious Students Table
    suspiciousLabel = new Label(LanguageManager.getInstance().get("conflict_suspicious"));
    suspiciousLabel.getStyleClass().add("label-title");
    
    suspiciousTable = new TableView<>();
    suspiciousTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    suspiciousTable.setPrefHeight(200);
    
    TableColumn<Student, String> suspNameCol = new TableColumn<>("Student Name");
    suspNameCol.setCellValueFactory(cellData -> 
        new javafx.beans.property.SimpleStringProperty(cellData.getValue().getName()));
    
    TableColumn<Student, String> suspIdCol = new TableColumn<>("Student ID");
    suspIdCol.setCellValueFactory(cellData -> 
        new javafx.beans.property.SimpleStringProperty(cellData.getValue().getId()));
    
    suspiciousTable.getColumns().addAll(suspNameCol, suspIdCol);
    suspiciousTable.setPlaceholder(new Label("No suspicious students detected"));

    mainLayout.getChildren().addAll(
        titleLabel, 
        analyzeButton, 
        summaryLabel,
        conflictsLabel,
        conflictTable,
        suspiciousLabel,
        suspiciousTable
    );
    
    VBox.setVgrow(conflictTable, javafx.scene.layout.Priority.ALWAYS);

    // Listen for language changes
    LanguageManager.getInstance().addLanguageChangeListener(lang -> updateLanguageTexts());
    
    ScrollPane scrollPane = new ScrollPane(mainLayout);
    scrollPane.setFitToWidth(true);
    return scrollPane;
  }

  private void analyzeConflicts() {
    try {
      Classroom classroom = classroomController.getClassroom();
      if (classroom == null || classroom.allPlacements().isEmpty()) {
        showAlert(Alert.AlertType.WARNING, LanguageManager.getInstance().get("no_data"),
            LanguageManager.getInstance().get("no_classroom_or_students"));
        return;
      }

      ConflictDetector detector = new ConflictDetector(classroom);
      var allConflicts = detector.detectAllConflicts();
      var suspiciousStudents = detector.findSuspiciousStudents(0.5); // 50% reciprocity threshold

      if (allConflicts.isEmpty() && suspiciousStudents.isEmpty()) {
        summaryLabel.setText(LanguageManager.getInstance().get("no_conflicts"));
        conflictTable.setItems(FXCollections.observableArrayList());
        suspiciousTable.setItems(FXCollections.observableArrayList());
      } else {
        summaryLabel.setText(LanguageManager.getInstance().get("conflict_detected_summary", allConflicts.size())
            + " - " + LanguageManager.getInstance().get("conflict_total_students", classroom.allPlacements().size())
            + (allConflicts.isEmpty() ? "" : " - " + LanguageManager.getInstance().get("conflict_rate",
                (allConflicts.size() * 100.0 / classroom.allPlacements().size()))));

        // Populate conflicts table
        ObservableList<AttendanceConflict> conflictData = FXCollections.observableArrayList(allConflicts);
        conflictTable.setItems(conflictData);

        // Populate suspicious students table
        ObservableList<Student> suspiciousData = FXCollections.observableArrayList(suspiciousStudents);
        suspiciousTable.setItems(suspiciousData);
      }
    } catch (Exception e) {
      showAlert(Alert.AlertType.ERROR, LanguageManager.getInstance().get("analysis_failed"),
          "Error: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void showAlert(Alert.AlertType type, String title, String message) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  private void updateLanguageTexts() {
    LanguageManager lm = LanguageManager.getInstance();
    if (titleLabel != null)
      titleLabel.setText(lm.get("conflict_title"));
    if (analyzeButton != null)
      analyzeButton.setText(lm.get("analyze_conflicts"));
    if (summaryLabel != null)
      summaryLabel.setText(lm.get("conflict_prompt"));
    if (conflictsLabel != null)
      conflictsLabel.setText(lm.get("conflict_detailed"));
    if (suspiciousLabel != null)
      suspiciousLabel.setText(lm.get("conflict_suspicious"));
  }
}
