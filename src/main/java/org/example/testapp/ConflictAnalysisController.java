package org.example.testapp;

import org.example.testapp.entities.Classroom;
import org.example.testapp.verification.ConflictDetector;
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
  private TextArea conflictTextArea;
  private Label summaryLabel;
  private Label titleLabel;
  private Button analyzeButton;
  private Label detailsLabel;

  public ConflictAnalysisController(ClassroomController classroomController) {
    this.classroomController = classroomController;
  }

  public Node getView() {
    VBox mainLayout = new VBox(10);
    mainLayout.setPadding(new Insets(15));

    // Title
    titleLabel = new Label(LanguageManager.getInstance().get("conflict_title"));
    titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

    // Analyze button
    analyzeButton = new Button(LanguageManager.getInstance().get("analyze_conflicts"));
    analyzeButton.setPrefWidth(150);
    analyzeButton.setOnAction(e -> analyzeConflicts());

    // Summary
    summaryLabel = new Label(LanguageManager.getInstance().get("conflict_prompt"));
    summaryLabel.setStyle("-fx-font-size: 11;");
    summaryLabel.setWrapText(true);

    // Conflict details text area
    conflictTextArea = new TextArea();
    conflictTextArea.setWrapText(true);
    conflictTextArea.setEditable(false);
    conflictTextArea.setPrefRowCount(20);
    conflictTextArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11;");

    detailsLabel = new Label(LanguageManager.getInstance().get("conflict_details"));
    mainLayout.getChildren().addAll(titleLabel, analyzeButton, summaryLabel,
        detailsLabel, conflictTextArea);
    VBox.setVgrow(conflictTextArea, javafx.scene.layout.Priority.ALWAYS);

    // Listen for language changes
    LanguageManager.getInstance().addLanguageChangeListener(lang -> updateLanguageTexts());
    return new ScrollPane(mainLayout);
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
        conflictTextArea.setText(LanguageManager.getInstance().get("conflict_truthful_summary",
            classroom.allPlacements().size()));
      } else {
        summaryLabel.setText(LanguageManager.getInstance().get("conflict_detected_summary", allConflicts.size()));

        StringBuilder details = new StringBuilder();
        details.append(LanguageManager.getInstance().get("conflict_report_header")).append("\n");
        details.append(LanguageManager.getInstance().get("conflict_total_conflicts", allConflicts.size())).append("\n");
        details.append(LanguageManager.getInstance().get("conflict_total_students", classroom.allPlacements().size()))
            .append("\n");
        if (!allConflicts.isEmpty()) {
          details.append(LanguageManager.getInstance().get("conflict_rate",
              (allConflicts.size() * 100.0 / classroom.allPlacements().size()))).append("\n");
        }
        details.append("\n");

        if (!allConflicts.isEmpty()) {
          details.append(LanguageManager.getInstance().get("conflict_detailed")).append("\n\n");
          for (var conflict : allConflicts) {
            details.append(LanguageManager.getInstance().get("conflict_claimer", conflict.getStudent().getName()))
                .append("\n");
            details.append(LanguageManager.getInstance().get("conflict_type", conflict.getType())).append("\n");
            details.append(LanguageManager.getInstance().get("conflict_detail_line", conflict.getDescription()))
                .append("\n");
            details.append(LanguageManager.getInstance().get("conflict_separator")).append("\n\n");
          }
        }

        if (!suspiciousStudents.isEmpty()) {
          details.append(LanguageManager.getInstance().get("conflict_suspicious")).append("\n\n");
          for (var student : suspiciousStudents) {
            details.append(LanguageManager.getInstance().get("conflict_suspicious_entry",
                student.getName(), student.getId())).append("\n");
          }
          details.append("\n");
        }

        details.append(LanguageManager.getInstance().get("analysis_complete")).append("\n");
        conflictTextArea.setText(details.toString());
      }
    } catch (Exception e) {
      showAlert(Alert.AlertType.ERROR, LanguageManager.getInstance().get("analysis_failed"),
          "Error: " + e.getMessage());
      conflictTextArea.setText(LanguageManager.getInstance().get("conflict_error", e.getMessage()));
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
    if (detailsLabel != null)
      detailsLabel.setText(lm.get("conflict_details"));
    if (conflictTextArea != null && (conflictTextArea.getText() == null || conflictTextArea.getText().isEmpty())) {
      conflictTextArea.setText(lm.get("conflict_prompt"));
    }
  }
}
