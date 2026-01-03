package org.example.testapp;

import org.example.testapp.entities.Claim;
import org.example.testapp.entities.LocatedStudent;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Controller for viewing per-student details.
 * Shows position, claims, and verification score.
 */
public class StudentDetailController {
  private TableView<InfoRow> infoTable;
  private TableView<Claim> claimsTable;
  private Label titleLabel;
  private Label claimsLabel;
  private LocatedStudent currentStudent;

  // Helper class for student information rows
  public static class InfoRow {
    private final String property;
    private final String value;

    public InfoRow(String property, String value) {
      this.property = property;
      this.value = value;
    }

    public String getProperty() {
      return property;
    }

    public String getValue() {
      return value;
    }
  }

  public Node getView() {
    VBox mainLayout = new VBox(15);
    mainLayout.setPadding(new Insets(15));
    mainLayout.getStyleClass().add("card");

    titleLabel = new Label(LanguageManager.getInstance().get("student_details"));
    titleLabel.getStyleClass().add("label-header");

    // Student Information Table
    infoTable = new TableView<>();
    infoTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    infoTable.setPrefHeight(200);

    TableColumn<InfoRow, String> propertyCol = new TableColumn<>("Property");
    propertyCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getProperty()));
    propertyCol.setMinWidth(150);

    TableColumn<InfoRow, String> valueCol = new TableColumn<>("Value");
    valueCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getValue()));
    valueCol.setMinWidth(200);

    infoTable.getColumns().addAll(propertyCol, valueCol);
    infoTable.setPlaceholder(new Label("Select a student to view details"));

    // Claims Table
    claimsLabel = new Label(LanguageManager.getInstance().get("claims"));
    claimsLabel.getStyleClass().add("label-title");

    claimsTable = new TableView<>();
    claimsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    claimsTable.setPrefHeight(300);

    TableColumn<Claim, String> directionCol = new TableColumn<>("Direction");
    directionCol.setCellValueFactory(cellData -> {
      String directionKey = "direction_" + cellData.getValue().getDirection().name().toLowerCase();
      return new SimpleStringProperty(LanguageManager.getInstance().get(directionKey));
    });
    directionCol.setMinWidth(120);

    TableColumn<Claim, String> targetCol = new TableColumn<>("Target Student");
    targetCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getTarget() != null ?
            cellData.getValue().getTarget().getName() :
            LanguageManager.getInstance().get("empty")));
    targetCol.setMinWidth(150);

    TableColumn<Claim, String> statusCol = new TableColumn<>("Status");
    statusCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().isAbsentClaim() ?
            LanguageManager.getInstance().get("absent") : "Present"));
    statusCol.setMinWidth(100);

    claimsTable.getColumns().addAll(directionCol, targetCol, statusCol);
    claimsTable.setPlaceholder(new Label(LanguageManager.getInstance().get("no_claims_recorded")));

    mainLayout.getChildren().addAll(titleLabel, infoTable, claimsLabel, claimsTable);
    VBox.setVgrow(claimsTable, Priority.ALWAYS);

    // Listen for language changes
    LanguageManager.getInstance().addLanguageChangeListener(lang -> updateLanguageTexts());

    ScrollPane scrollPane = new ScrollPane(mainLayout);
    scrollPane.setFitToWidth(true);
    return scrollPane;
  }

  public void displayStudent(LocatedStudent student) {
    if (infoTable == null || claimsTable == null) {
      System.out.println("[DEBUG] Tables are null in displayStudent()!");
      return;
    }

    if (student == null) {
      clear();
      return;
    }

    currentStudent = student;
    LanguageManager lm = LanguageManager.getInstance();

    // Populate student info table
    ObservableList<InfoRow> infoData = FXCollections.observableArrayList();
    infoData.add(new InfoRow(lm.get("name"), student.getStudent().getName()));
    infoData.add(new InfoRow(lm.get("id"), student.getStudent().getId()));

    if (student.getPosition() != null) {
      String positionStr = lm.get("row") + " " + student.getPosition().row() +
          ", " + lm.get("col") + " " + student.getPosition().col();
      infoData.add(new InfoRow(lm.get("position"), positionStr));
    } else {
      infoData.add(new InfoRow(lm.get("position"), lm.get("absent")));
    }

    infoData.add(new InfoRow(lm.get("status"), student.getStatus().toString()));
    infoData.add(new InfoRow(lm.get("claims_count"), String.valueOf(student.getClaims().size())));

    infoTable.setItems(infoData);

    // Populate claims table
    ObservableList<Claim> claimsData = FXCollections.observableArrayList(student.getClaims());
    claimsTable.setItems(claimsData);

    System.out.println("[DEBUG] Student details displayed for: " + student.getStudent().getName());
  }

  public void clear() {
    if (infoTable != null) {
      infoTable.setItems(FXCollections.observableArrayList());
    }
    if (claimsTable != null) {
      claimsTable.setItems(FXCollections.observableArrayList());
    }
  }

  private void updateLanguageTexts() {
    if (titleLabel != null) {
      titleLabel.setText(LanguageManager.getInstance().get("student_details"));
    }
    if (claimsLabel != null) {
      claimsLabel.setText(LanguageManager.getInstance().get("claims"));
    }
    // Refresh current student data if available
    if (currentStudent != null) {
      displayStudent(currentStudent);
    }
  }
}
