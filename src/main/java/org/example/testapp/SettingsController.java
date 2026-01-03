package org.example.testapp;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Controller for application settings and configuration.
 */
public class SettingsController {
  private final Properties props = new Properties();
  private final File settingsFile = new File(System.getProperty("user.home"), ".whoami/settings.properties");
  private Slider neighborWeightSlider;
  private Slider occupancyWeightSlider;
  private Slider consensusWeightSlider;
  private Slider presentThresholdSlider;
  private Slider absentThresholdSlider;
  private Label configTitleLabel;
  private Label weightsTitleLabel;
  private Label thresholdsTitleLabel;
  private Button applyButton;
  private Button resetButton;
  private ProgressBar totalWeightProgress;
  private Label totalWeightLabel;

  public Node getView() {
    VBox mainLayout = new VBox(20);
    mainLayout.setPadding(new Insets(20));
    mainLayout.getStyleClass().add("card");

    // Header Section with Icon
    HBox headerBox = new HBox(15);
    headerBox.setAlignment(Pos.CENTER_LEFT);
    
    Circle iconCircle = new Circle(25);
    iconCircle.setFill(Color.web("#3498DB"));
    Label iconLabel = new Label("⚙");
    iconLabel.setStyle("-fx-font-size: 28px; -fx-text-fill: white;");
    iconLabel.setTranslateX(-25);
    iconLabel.setTranslateY(-25);
    
    configTitleLabel = new Label(LanguageManager.getInstance().get("configuration_settings"));
    configTitleLabel.getStyleClass().add("label-header");
    
    headerBox.getChildren().addAll(iconCircle, iconLabel, configTitleLabel);

    // Strategy Weights Section
    VBox weightsSection = createWeightsSection();

    // Confidence Thresholds Section
    VBox thresholdsSection = createThresholdsSection();

    // Buttons
    HBox buttonBox = createButtonBox();

    mainLayout.getChildren().addAll(
        headerBox,
        weightsSection,
        thresholdsSection,
        buttonBox);

    loadSettings();

    // Listen for language changes
    LanguageManager.getInstance().addLanguageChangeListener(lang -> updateLanguageTexts());
    
    ScrollPane scrollPane = new ScrollPane(mainLayout);
    scrollPane.setFitToWidth(true);
    return scrollPane;
  }

  private VBox createWeightsSection() {
    VBox section = new VBox(15);
    section.setPadding(new Insets(20));
    section.getStyleClass().addAll("card", "settings-weights-section");

    HBox titleBox = new HBox(10);
    titleBox.setAlignment(Pos.CENTER_LEFT);
    
    Label iconLabel = new Label("⚖");
    iconLabel.setStyle("-fx-font-size: 20px;");
    
    weightsTitleLabel = new Label(LanguageManager.getInstance().get("strategy_weights"));
    weightsTitleLabel.getStyleClass().add("label-title");
    
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    
    totalWeightLabel = new Label("Total: 1.00");
    totalWeightLabel.getStyleClass().add("label-secondary");
    
    titleBox.getChildren().addAll(iconLabel, weightsTitleLabel, spacer, totalWeightLabel);

    // Total weight progress bar
    totalWeightProgress = new ProgressBar(1.0);
    totalWeightProgress.setPrefWidth(Double.MAX_VALUE);
    totalWeightProgress.setPrefHeight(8);
    
    // Neighbor Verification Weight
    VBox neighborBox = createWeightControl(
        "🤝 Neighbor Verification Weight",
        "#3498DB",
        0.4,
        (value) -> updateWeightProgress());
    neighborWeightSlider = (Slider) neighborBox.lookup(".slider");

    // Seat Occupancy Weight
    VBox occupancyBox = createWeightControl(
        "💺 Seat Occupancy Weight",
        "#9B59B6",
        0.35,
        (value) -> updateWeightProgress());
    occupancyWeightSlider = (Slider) occupancyBox.lookup(".slider");

    // Consensus Score Weight
    VBox consensusBox = createWeightControl(
        "🎯 Consensus Scoring Weight",
        "#E67E22",
        0.25,
        (value) -> updateWeightProgress());
    consensusWeightSlider = (Slider) consensusBox.lookup(".slider");

    section.getChildren().addAll(titleBox, totalWeightProgress, neighborBox, occupancyBox, consensusBox);

    return section;
  }

  private VBox createWeightControl(String label, String color, double initialValue,
      javafx.event.EventHandler<javafx.scene.input.MouseEvent> onChange) {
    VBox box = new VBox(8);
    box.setPadding(new Insets(10));
    box.getStyleClass().add("settings-control-box");

    HBox headerBox = new HBox(10);
    headerBox.setAlignment(Pos.CENTER_LEFT);
    
    Label nameLabel = new Label(label);
    nameLabel.getStyleClass().add("label-title");
    nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 500;");
    
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    
    Label valueLabel = new Label(String.format("%.0f%%", initialValue * 100));
    valueLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    valueLabel.setPrefWidth(60);
    valueLabel.setAlignment(Pos.CENTER_RIGHT);

    headerBox.getChildren().addAll(nameLabel, spacer, valueLabel);

    Slider slider = new Slider(0, 1, initialValue);
    slider.getStyleClass().add("slider");
    slider.setPrefWidth(Double.MAX_VALUE);
    slider.setShowTickMarks(true);
    slider.setMajorTickUnit(0.2);
    slider.setMinorTickCount(1);
    slider.setBlockIncrement(0.05);

    slider.valueProperty().addListener((obs, oldVal, newVal) -> {
      valueLabel.setText(String.format("%.0f%%", newVal.doubleValue() * 100));
    });

    ProgressBar progressBar = new ProgressBar(initialValue);
    progressBar.setPrefWidth(Double.MAX_VALUE);
    progressBar.setPrefHeight(6);
    progressBar.setStyle("-fx-accent: " + color + ";");
    
    slider.valueProperty().addListener((obs, oldVal, newVal) -> {
      progressBar.setProgress(newVal.doubleValue());
    });

    box.getChildren().addAll(headerBox, slider, progressBar);
    return box;
  }

  private VBox createThresholdsSection() {
    VBox section = new VBox(15);
    section.setPadding(new Insets(20));
    section.getStyleClass().addAll("card", "settings-thresholds-section");

    HBox titleBox = new HBox(10);
    titleBox.setAlignment(Pos.CENTER_LEFT);
    
    Label iconLabel = new Label("📊");
    iconLabel.setStyle("-fx-font-size: 20px;");
    
    thresholdsTitleLabel = new Label(LanguageManager.getInstance().get("confidence_thresholds"));
    thresholdsTitleLabel.getStyleClass().add("label-title");
    
    titleBox.getChildren().addAll(iconLabel, thresholdsTitleLabel);

    // Present Threshold
    VBox presentBox = createThresholdControl(
        "✅ Confidence Threshold for PRESENT Status (≥)",
        "#27AE60",
        0.65);
    presentThresholdSlider = (Slider) presentBox.lookup(".slider");

    // Absent Threshold
    VBox absentBox = createThresholdControl(
        "❌ Confidence Threshold for ABSENT Status (≤)",
        "#E74C3C",
        0.35);
    absentThresholdSlider = (Slider) absentBox.lookup(".slider");

    // Info box
    HBox infoBox = new HBox(10);
    infoBox.setPadding(new Insets(10));
    infoBox.getStyleClass().add("settings-info-box");
    
    Label infoIcon = new Label("ℹ");
    infoIcon.setStyle("-fx-font-size: 16px;");
    
    Label infoLabel = new Label("Scores between thresholds are marked as UNCERTAIN");
    infoLabel.setStyle("-fx-font-size: 11px; -fx-font-style: italic;");
    infoLabel.setWrapText(true);
    
    infoBox.getChildren().addAll(infoIcon, infoLabel);

    section.getChildren().addAll(titleBox, presentBox, absentBox, infoBox);

    return section;
  }

  private VBox createThresholdControl(String label, String color, double initialValue) {
    VBox box = new VBox(8);
    box.setPadding(new Insets(10));
    box.getStyleClass().add("settings-control-box");

    HBox headerBox = new HBox(10);
    headerBox.setAlignment(Pos.CENTER_LEFT);
    
    Label nameLabel = new Label(label);
    nameLabel.getStyleClass().add("label-title");
    nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 500;");
    
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    
    Label valueLabel = new Label(String.format("%.0f%%", initialValue * 100));
    valueLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    valueLabel.setPrefWidth(60);
    valueLabel.setAlignment(Pos.CENTER_RIGHT);

    headerBox.getChildren().addAll(nameLabel, spacer, valueLabel);

    Slider slider = new Slider(0, 1, initialValue);
    slider.getStyleClass().add("slider");
    slider.setPrefWidth(Double.MAX_VALUE);
    slider.setShowTickMarks(true);
    slider.setMajorTickUnit(0.2);
    slider.setMinorTickCount(1);
    slider.setBlockIncrement(0.05);

    slider.valueProperty().addListener((obs, oldVal, newVal) -> {
      valueLabel.setText(String.format("%.0f%%", newVal.doubleValue() * 100));
    });

    ProgressBar progressBar = new ProgressBar(initialValue);
    progressBar.setPrefWidth(Double.MAX_VALUE);
    progressBar.setPrefHeight(6);
    progressBar.setStyle("-fx-accent: " + color + ";");
    
    slider.valueProperty().addListener((obs, oldVal, newVal) -> {
      progressBar.setProgress(newVal.doubleValue());
    });

    box.getChildren().addAll(headerBox, slider, progressBar);
    return box;
  }

  private HBox createButtonBox() {
    HBox box = new HBox(15);
    box.setPadding(new Insets(10));
    box.setAlignment(Pos.CENTER);

    applyButton = new Button(LanguageManager.getInstance().get("apply_settings"));
    applyButton.getStyleClass().add("button-success");
    applyButton.setPrefWidth(150);
    applyButton.setOnAction(e -> applySettings());

    resetButton = new Button(LanguageManager.getInstance().get("reset_defaults"));
    resetButton.getStyleClass().add("button-secondary");
    resetButton.setPrefWidth(150);
    resetButton.setOnAction(e -> resetToDefaults());

    box.getChildren().addAll(applyButton, resetButton);

    return box;
  }

  private void updateWeightProgress() {
    double total = neighborWeightSlider.getValue() + 
                   occupancyWeightSlider.getValue() + 
                   consensusWeightSlider.getValue();
    totalWeightProgress.setProgress(total);
    totalWeightLabel.setText(String.format("Total: %.2f", total));
    
    // Change color based on total
    if (Math.abs(total - 1.0) < 0.01) {
      totalWeightProgress.setStyle("-fx-accent: #27AE60;"); // Green for perfect
    } else if (total > 0) {
      totalWeightProgress.setStyle("-fx-accent: #F39C12;"); // Orange for non-zero
    } else {
      totalWeightProgress.setStyle("-fx-accent: #E74C3C;"); // Red for zero
    }
  }

  private void updateWeights() {
    // Weights are updated live as sliders move
  }

  private void applySettings() {
    double neighborWeight = neighborWeightSlider.getValue();
    double occupancyWeight = occupancyWeightSlider.getValue();
    double consensusWeight = consensusWeightSlider.getValue();
    double presentThreshold = presentThresholdSlider.getValue();
    double absentThreshold = absentThresholdSlider.getValue();

    // Validation checks
    StringBuilder warnings = new StringBuilder();

    if (absentThreshold >= presentThreshold) {
      warnings.append("⚠ Absent threshold must be less than Present threshold\n");
    }

    double total = neighborWeight + occupancyWeight + consensusWeight;
    if (total == 0) {
      showError("Weights must sum to a value greater than zero");
      return;
    }

    if (neighborWeight == 0 || occupancyWeight == 0 || consensusWeight == 0) {
      warnings.append("⚠ At least one weight is zero - this strategy will be disabled\n");
    }

    if (presentThreshold < 0.5) {
      warnings.append("⚠ Present threshold < 0.5 may mark too many as present\n");
    }

    if (absentThreshold > 0.5) {
      warnings.append("⚠ Absent threshold > 0.5 may mark too many as absent\n");
    }

    if (warnings.length() > 0) {
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.setTitle(LanguageManager.getInstance().get("settings_warnings_title"));
      alert.setHeaderText(LanguageManager.getInstance().get("configuration_issues"));
      alert.setContentText(warnings.toString());

      ButtonType continueBtn = new ButtonType(LanguageManager.getInstance().get("continue"));
      ButtonType cancelBtn = new ButtonType(LanguageManager.getInstance().get("cancel"));
      alert.getButtonTypes().setAll(continueBtn, cancelBtn);

      if (alert.showAndWait().get() == cancelBtn) {
        return;
      }
    }

    neighborWeight /= total;
    occupancyWeight /= total;
    consensusWeight /= total;

    saveSettings(neighborWeight, occupancyWeight, consensusWeight, presentThreshold, absentThreshold);
    AuditLogger.log("SETTINGS_APPLIED",
        String.format("Neighbor: %.2f, Occupancy: %.2f, Consensus: %.2f, Present: %.2f, Absent: %.2f",
            neighborWeight, occupancyWeight, consensusWeight, presentThreshold, absentThreshold));

    showInfo(String.format(
        "Settings applied and saved:\n\n" +
            "Neighbor Weight: %.2f\n" +
            "Occupancy Weight: %.2f\n" +
            "Consensus Weight: %.2f\n\n" +
            "Present Threshold: %.2f\n" +
            "Absent Threshold: %.2f",
        neighborWeight, occupancyWeight, consensusWeight, presentThreshold, absentThreshold));
  }

  private void resetToDefaults() {
    neighborWeightSlider.setValue(0.4);
    occupancyWeightSlider.setValue(0.35);
    consensusWeightSlider.setValue(0.25);
    presentThresholdSlider.setValue(0.65);
    absentThresholdSlider.setValue(0.35);

    applySettings();
    showInfo("Settings reset to default values");
  }

  private void loadSettings() {
    try {
      if (settingsFile.exists()) {
        try (FileInputStream in = new FileInputStream(settingsFile)) {
          props.load(in);
        }

        neighborWeightSlider = neighborWeightSlider == null ? null : neighborWeightSlider;
        occupancyWeightSlider = occupancyWeightSlider == null ? null : occupancyWeightSlider;
        consensusWeightSlider = consensusWeightSlider == null ? null : consensusWeightSlider;

        double neighbor = Double.parseDouble(props.getProperty("weight.neighbor", "0.4"));
        double occupancy = Double.parseDouble(props.getProperty("weight.occupancy", "0.35"));
        double consensus = Double.parseDouble(props.getProperty("weight.consensus", "0.25"));
        double present = Double.parseDouble(props.getProperty("threshold.present", "0.65"));
        double absent = Double.parseDouble(props.getProperty("threshold.absent", "0.35"));

        if (neighborWeightSlider != null)
          neighborWeightSlider.setValue(neighbor);
        if (occupancyWeightSlider != null)
          occupancyWeightSlider.setValue(occupancy);
        if (consensusWeightSlider != null)
          consensusWeightSlider.setValue(consensus);
        if (presentThresholdSlider != null)
          presentThresholdSlider.setValue(present);
        if (absentThresholdSlider != null)
          absentThresholdSlider.setValue(absent);
      }
    } catch (Exception ignored) {
    }
  }

  private void saveSettings(double neighbor, double occupancy, double consensus, double present, double absent) {
    try {
      settingsFile.getParentFile().mkdirs();
      props.setProperty("weight.neighbor", String.format("%.3f", neighbor));
      props.setProperty("weight.occupancy", String.format("%.3f", occupancy));
      props.setProperty("weight.consensus", String.format("%.3f", consensus));
      props.setProperty("threshold.present", String.format("%.3f", present));
      props.setProperty("threshold.absent", String.format("%.3f", absent));
      try (FileOutputStream out = new FileOutputStream(settingsFile)) {
        props.store(out, "Smart Attendance Settings");
      }
    } catch (IOException ignored) {
    }
  }

  private void showError(String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(LanguageManager.getInstance().get("error"));
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  private void showInfo(String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(LanguageManager.getInstance().get("settings"));
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  private void updateLanguageTexts() {
    LanguageManager lm = LanguageManager.getInstance();
    if (configTitleLabel != null)
      configTitleLabel.setText(lm.get("configuration_settings"));
    if (weightsTitleLabel != null)
      weightsTitleLabel.setText(lm.get("strategy_weights"));
    if (thresholdsTitleLabel != null)
      thresholdsTitleLabel.setText(lm.get("confidence_thresholds"));
    if (applyButton != null)
      applyButton.setText(lm.get("apply_settings"));
    if (resetButton != null)
      resetButton.setText(lm.get("reset_defaults"));
  }
}
