package org.example.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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

  public Node getView() {
    VBox mainLayout = new VBox(15);
    mainLayout.setPadding(new Insets(20));

    // Strategy Weights Section
    VBox weightsSection = createWeightsSection();

    // Confidence Thresholds Section
    VBox thresholdsSection = createThresholdsSection();

    // Buttons
    HBox buttonBox = createButtonBox();

    mainLayout.getChildren().addAll(
        (configTitleLabel = new Label(LanguageManager.getInstance().get("configuration_settings"))),
        weightsSection,
        new Separator(),
        thresholdsSection,
        new Separator(),
        buttonBox);

    Label titleLabel = (Label) mainLayout.getChildren().get(0);
    titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

    loadSettings();

    // Listen for language changes
    LanguageManager.getInstance().addLanguageChangeListener(lang -> updateLanguageTexts());
    return new ScrollPane(mainLayout);
  }

  private VBox createWeightsSection() {
    VBox section = new VBox(15);
    section.setPadding(new Insets(15));
    section.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 5;");

    weightsTitleLabel = new Label(LanguageManager.getInstance().get("strategy_weights"));
    weightsTitleLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");

    // Neighbor Verification Weight
    VBox neighborBox = createWeightControl(
        "Neighbor Verification Weight",
        0.4,
        (value) -> updateWeights());
    neighborWeightSlider = (Slider) neighborBox.lookup(".slider");

    // Seat Occupancy Weight
    VBox occupancyBox = createWeightControl(
        "Seat Occupancy Weight",
        0.35,
        (value) -> updateWeights());
    occupancyWeightSlider = (Slider) occupancyBox.lookup(".slider");

    // Consensus Score Weight
    VBox consensusBox = createWeightControl(
        "Consensus Scoring Weight",
        0.25,
        (value) -> updateWeights());
    consensusWeightSlider = (Slider) consensusBox.lookup(".slider");

    section.getChildren().addAll(weightsTitleLabel, neighborBox, occupancyBox, consensusBox);

    return section;
  }

  private VBox createWeightControl(String label, double initialValue,
      javafx.event.EventHandler<javafx.scene.input.MouseEvent> onChange) {
    VBox box = new VBox(5);

    Label nameLabel = new Label(label);
    nameLabel.setStyle("-fx-font-size: 11;");

    HBox controlBox = new HBox(10);
    Slider slider = new Slider(0, 1, initialValue);
    slider.setStyle(".slider");
    slider.setPrefWidth(200);
    slider.setShowTickLabels(true);
    slider.setShowTickMarks(true);
    slider.setMajorTickUnit(0.1);

    Label valueLabel = new Label(String.format("%.2f", initialValue));
    valueLabel.setPrefWidth(50);
    valueLabel.setStyle("-fx-font-size: 11; -fx-text-alignment: center;");

    slider.valueProperty().addListener((obs, oldVal, newVal) -> {
      valueLabel.setText(String.format("%.2f", newVal.doubleValue()));
    });

    controlBox.getChildren().addAll(slider, valueLabel);

    box.getChildren().addAll(nameLabel, controlBox);
    return box;
  }

  private VBox createThresholdsSection() {
    VBox section = new VBox(15);
    section.setPadding(new Insets(15));
    section.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 5;");

    thresholdsTitleLabel = new Label(LanguageManager.getInstance().get("confidence_thresholds"));
    thresholdsTitleLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");

    // Present Threshold
    VBox presentBox = createThresholdControl(
        "Confidence Threshold for PRESENT Status (≥)",
        0.65);
    presentThresholdSlider = (Slider) presentBox.lookup(".slider");

    // Absent Threshold
    VBox absentBox = createThresholdControl(
        "Confidence Threshold for ABSENT Status (≤)",
        0.35);
    absentThresholdSlider = (Slider) absentBox.lookup(".slider");

    Label infoLabel = new Label("Scores between thresholds are marked as UNCERTAIN");
    infoLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #666666; -fx-font-style: italic;");

    section.getChildren().addAll(thresholdsTitleLabel, presentBox, absentBox, infoLabel);

    return section;
  }

  private VBox createThresholdControl(String label, double initialValue) {
    VBox box = new VBox(5);

    Label nameLabel = new Label(label);
    nameLabel.setStyle("-fx-font-size: 11;");

    HBox controlBox = new HBox(10);
    Slider slider = new Slider(0, 1, initialValue);
    slider.setStyle(".slider");
    slider.setPrefWidth(200);
    slider.setShowTickLabels(true);
    slider.setShowTickMarks(true);
    slider.setMajorTickUnit(0.1);

    Label valueLabel = new Label(String.format("%.2f", initialValue));
    valueLabel.setPrefWidth(50);
    valueLabel.setStyle("-fx-font-size: 11; -fx-text-alignment: center;");

    slider.valueProperty().addListener((obs, oldVal, newVal) -> {
      valueLabel.setText(String.format("%.2f", newVal.doubleValue()));
    });

    controlBox.getChildren().addAll(slider, valueLabel);

    box.getChildren().addAll(nameLabel, controlBox);
    return box;
  }

  private HBox createButtonBox() {
    HBox box = new HBox(10);
    box.setPadding(new Insets(10));

    applyButton = new Button(LanguageManager.getInstance().get("apply_settings"));
    applyButton.setPrefWidth(120);
    applyButton.setStyle("-fx-font-size: 11; -fx-padding: 8;");
    applyButton.setOnAction(e -> applySettings());

    resetButton = new Button(LanguageManager.getInstance().get("reset_defaults"));
    resetButton.setPrefWidth(120);
    resetButton.setStyle("-fx-font-size: 11; -fx-padding: 8;");
    resetButton.setOnAction(e -> resetToDefaults());

    box.getChildren().addAll(applyButton, resetButton);

    return box;
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
