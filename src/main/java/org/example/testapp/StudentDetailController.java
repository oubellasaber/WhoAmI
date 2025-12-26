package org.example.testapp;

import org.example.testapp.entities.LocatedStudent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Controller for viewing per-student details.
 * Shows position, claims, and verification score.
 */
public class StudentDetailController {
  private TextArea detailsArea;
  private Label titleLabel;

  public Node getView() {
    VBox mainLayout = new VBox(10);
    mainLayout.setPadding(new Insets(15));

    titleLabel = new Label(LanguageManager.getInstance().get("student_details"));
    titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

    detailsArea = new TextArea();
    detailsArea.setPrefHeight(500);
    detailsArea.setWrapText(true);
    detailsArea.setEditable(false);
    detailsArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11;");

    mainLayout.getChildren().addAll(titleLabel, detailsArea);
    VBox.setVgrow(detailsArea, Priority.ALWAYS);

    // Listen for language changes
    LanguageManager.getInstance().addLanguageChangeListener(lang -> updateLanguageTexts());

    return new ScrollPane(mainLayout);
  }

  public void displayStudent(LocatedStudent student) {
    if (detailsArea == null) {
      System.out.println("[DEBUG] detailsArea is null in displayStudent()!");
      return;
    }

    if (student == null) {
      detailsArea.clear();
      return;
    }

    LanguageManager lm = LanguageManager.getInstance();
    StringBuilder sb = new StringBuilder();
    sb.append("=== ").append(lm.get("student_details")).append(" ===\n\n");
    sb.append(lm.get("name")).append(": ").append(student.getStudent().getName()).append("\n");
    sb.append(lm.get("id")).append(": ").append(student.getStudent().getId()).append("\n");

    if (student.getPosition() != null) {
      sb.append(lm.get("position")).append(": ").append(lm.get("row")).append(" ")
          .append(student.getPosition().row())
          .append(", ").append(lm.get("col")).append(" ").append(student.getPosition().col()).append("\n");
    } else {
      sb.append(lm.get("position")).append(": ").append(lm.get("absent")).append("\n");
    }

    sb.append("\n=== ").append(lm.get("claims")).append(" ===\n");
    if (student.getClaims().isEmpty()) {
      sb.append(lm.get("no_claims_recorded")).append("\n");
    } else {
      student.getClaims().forEach(claim -> {
        String directionKey = "direction_" + claim.getDirection().name().toLowerCase();
        sb.append("• ").append(lm.get("direction")).append(": ").append(lm.get(directionKey))
            .append(", ").append(lm.get("target")).append(": ")
            .append(claim.getTarget() != null ? claim.getTarget().getName() : lm.get("empty"))
            .append("\n");
      });
    }

    sb.append("\n=== ").append(lm.get("verification_score")).append(" ===\n");
    sb.append(lm.get("claims_count")).append(": ").append(student.getClaims().size()).append("\n");

    detailsArea.setText(sb.toString());
    System.out.println("[DEBUG] Student details displayed for: " + student.getStudent().getName());
  }

  public void clear() {
    detailsArea.clear();
  }

  private void updateLanguageTexts() {
    if (titleLabel != null) {
      titleLabel.setText(LanguageManager.getInstance().get("student_details"));
    }
  }
}
