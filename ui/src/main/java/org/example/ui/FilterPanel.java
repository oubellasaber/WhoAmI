package org.example.ui;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class FilterPanel extends VBox {
  private ComboBox<String> statusFilter;
  private ComboBox<Integer> rowFilter;
  private ComboBox<Integer> colFilter;
  private ComboBox<String> verificationFilter;
  private Button clearButton;
  private Runnable onFilterChanged;

  public FilterPanel() {
    setPrefWidth(200);
    setSpacing(10);
    setStyle("-fx-padding: 10; -fx-border-color: #cccccc; -fx-border-width: 1;");

    // Status filter
    VBox statusBox = new VBox(5);
    statusBox.getChildren().addAll(
        new Label("Status:"),
        statusFilter = new ComboBox<>(FXCollections.observableArrayList(
            "All", "Present", "Absent", "Conflicted")));
    statusFilter.setValue("All");
    statusFilter.setOnAction(e -> fireFilterChanged());

    // Row filter
    VBox rowBox = new VBox(5);
    ObservableList<Integer> rows = FXCollections.observableArrayList();
    rows.add(null);
    for (int i = 1; i <= 10; i++)
      rows.add(i);
    rowFilter = new ComboBox<>(rows);
    rowFilter.setValue(null);
    rowFilter.setPromptText("All rows");
    rowBox.getChildren().addAll(new Label("Row:"), rowFilter);
    rowFilter.setOnAction(e -> fireFilterChanged());

    // Column filter
    VBox colBox = new VBox(5);
    ObservableList<Integer> cols = FXCollections.observableArrayList();
    cols.add(null);
    for (int i = 1; i <= 10; i++)
      cols.add(i);
    colFilter = new ComboBox<>(cols);
    colFilter.setValue(null);
    colFilter.setPromptText("All columns");
    colBox.getChildren().addAll(new Label("Column:"), colFilter);
    colFilter.setOnAction(e -> fireFilterChanged());

    // Verification filter
    VBox verBox = new VBox(5);
    verificationFilter = new ComboBox<>(FXCollections.observableArrayList(
        "All", "Verified", "Not Verified", "Conflicted"));
    verificationFilter.setValue("All");
    verBox.getChildren().addAll(new Label("Verification:"), verificationFilter);
    verificationFilter.setOnAction(e -> fireFilterChanged());

    // Clear button
    clearButton = new Button("Clear Filters");
    clearButton.setPrefWidth(150);
    clearButton.setOnAction(e -> clearFilters());

    getChildren().addAll(statusBox, rowBox, colBox, verBox, clearButton);
  }

  public String getStatusFilter() {
    return statusFilter.getValue();
  }

  public Integer getRowFilter() {
    return rowFilter.getValue();
  }

  public Integer getColFilter() {
    return colFilter.getValue();
  }

  public String getVerificationFilter() {
    return verificationFilter.getValue();
  }

  public void clearFilters() {
    statusFilter.setValue("All");
    rowFilter.setValue(null);
    colFilter.setValue(null);
    verificationFilter.setValue("All");
    fireFilterChanged();
  }

  public void setOnFilterChanged(Runnable callback) {
    this.onFilterChanged = callback;
  }

  private void fireFilterChanged() {
    if (onFilterChanged != null) {
      onFilterChanged.run();
    }
  }
}
