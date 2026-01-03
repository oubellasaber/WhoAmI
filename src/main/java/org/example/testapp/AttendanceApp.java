package org.example.testapp;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.testapp.entities.Classroom;

import java.io.File;

/**
 * Main JavaFX application for the Smart Attendance System.
 * Provides a GUI for managing classroom seating, recording claims, and
 * analyzing attendance.
 */
public class AttendanceApp extends Application {
  private ClassroomController classroomController;
  private Stage primaryStage;
  private Scene scene;
  private HistoryController historyController;
  private StudentDetailController studentDetailController;
  private Label languageStatusLabel;
  // Menus and items to re-label on language change
  private Menu fileMenu;
  private Menu editMenu;
  private Menu viewMenu;
  private Menu helpMenu;
  private Menu languageMenu;
  private MenuItem newItem;
  private MenuItem saveItem;
  private MenuItem loadItem;
  private MenuItem exitItem;
  private MenuItem undoItem;
  private MenuItem redoItem;
  private MenuItem aboutItem;
  private CheckMenuItem darkToggle;
  private CheckMenuItem redactionToggle;
  // Tabs to re-label on language change
  private Tab classroomTab;
  private Tab resultsTab;
  private Tab conflictTab;
  private Tab detailsTab;
  private Tab historyTab;
  private Tab statsTab;
  private Tab settingsTab;
  private Tab auditTab;
  // Audit view controls for live localization
  private Label auditTitleLabel;
  private Button auditRefreshButton;
  private Button auditClearButton;



  @Override
  public void start(Stage stage) throws Exception {
    this.primaryStage = stage;
    primaryStage.setTitle("Smart Attendance System");
    primaryStage.setWidth(1400);
    primaryStage.setHeight(900);

    // Set up keyboard shortcuts
    primaryStage.setOnShowing(e -> setupKeyboardShortcuts());

    // Create main container
    BorderPane mainLayout = new BorderPane();

    // Create menu bar
    mainLayout.setTop(createMenuBar());

    // Create main content area with tabs
    TabPane contentTabs = new TabPane();
    contentTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

    // Create classroom controller first
    classroomController = new ClassroomController();

    // Classroom tab
    classroomTab = new Tab("Classroom", classroomController.getView());
    classroomTab.setClosable(false);
    contentTabs.getTabs().add(classroomTab);

    // Results tab - pass classroom controller for analysis results
    ResultsController resultsController = new ResultsController(classroomController);
    resultsTab = new Tab("Attendance Results", resultsController.getView());
    resultsTab.setClosable(false);
    contentTabs.getTabs().add(resultsTab);

    // Conflict Analysis tab
    ConflictAnalysisController conflictController = new ConflictAnalysisController(classroomController);
    conflictTab = new Tab("Conflict Analysis", conflictController.getView());
    conflictTab.setClosable(false);
    contentTabs.getTabs().add(conflictTab);

    // Student Details tab
    studentDetailController = new StudentDetailController();
    detailsTab = new Tab("Student Details", studentDetailController.getView());
    detailsTab.setClosable(false);
    contentTabs.getTabs().add(detailsTab);
    classroomController.setStudentDetailController(studentDetailController);
    classroomController.setDetailsTab(detailsTab);

    // History tab
    historyController = new HistoryController();
    historyTab = new Tab("Analysis History", historyController.getView());
    historyTab.setClosable(false);
    contentTabs.getTabs().add(historyTab);
    classroomController.setHistoryController(historyController);

    // Statistics tab
    StatsController statsController = new StatsController();
    ScrollPane statsScrollPane = new ScrollPane(statsController.getRoot());
    statsScrollPane.setFitToWidth(true);
    statsTab = new Tab("Statistics", statsScrollPane);
    statsTab.setClosable(false);
    // Refresh stats when user opens the tab
    statsTab.setOnSelectionChanged(e -> {
      if (statsTab.isSelected()) {
        System.out.println("[DEBUG] Statistics tab selected - refreshing stats");
        statsController.updateStats(classroomController.getClassroom(), classroomController.getAttendanceService());
      }
    });
    contentTabs.getTabs().add(statsTab);
    System.out.println("[DEBUG] Setting up stats callback...");
    classroomController.setOnAnalysisComplete(() -> {
      System.out.println("[DEBUG] Analysis complete callback triggered!");
      statsController.updateStats(classroomController.getClassroom(), classroomController.getAttendanceService());
    });

    // Settings tab
    settingsTab = new Tab("Settings", createSettingsView());
    settingsTab.setClosable(false);
    contentTabs.getTabs().add(settingsTab);

    // Audit Log tab
    auditTab = new Tab("Audit Log", createAuditLogView());
    auditTab.setClosable(false);
    contentTabs.getTabs().add(auditTab);

    // Set up analysis callback to refresh results
    classroomController.setOnAnalysisComplete(() -> {
      // Results tab will be updated when clicked
    });

    mainLayout.setCenter(contentTabs);

    // Add tab switching animations
    setupTabAnimations(contentTabs);

    // Add language status label at the bottom
    languageStatusLabel = new Label();
    languageStatusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: #7F8C8D;");
    updateLanguageStatusLabel();
    LanguageManager.getInstance().addLanguageChangeListener(lang -> {
      System.out.println("[DEBUG] AttendanceApp listener triggered! Language changed to: " + lang);
      updateLanguageStatusLabel();
      applyTranslations();
    });

    HBox bottomBar = new HBox(10);
    bottomBar.setPadding(new Insets(10, 15, 10, 15));
    bottomBar.getStyleClass().add("status-bar");
    bottomBar.setAlignment(Pos.CENTER_LEFT);
    
    // Add version label
    Label versionLabel = new Label("v1.0");
    versionLabel.getStyleClass().add("label-secondary");
    versionLabel.setStyle("-fx-font-size: 11px;");
    
    // Spacer
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    
    bottomBar.getChildren().addAll(languageStatusLabel, spacer, versionLabel);
    mainLayout.setBottom(bottomBar);

    // Create scene
    scene = new Scene(mainLayout);
    
    // Load and apply professional stylesheet
    try {
      String css = getClass().getResource("styles.css").toExternalForm();
      scene.getStylesheets().add(css);
      System.out.println("[UI] Professional stylesheet loaded successfully");
    } catch (Exception e) {
      System.err.println("[UI] Warning: Could not load stylesheet: " + e.getMessage());
    }
    
    primaryStage.setScene(scene);
    // Apply translations to tabs now that they're created
    applyTranslations();
    applyTheme(false);
    primaryStage.show();
  }

  private MenuBar createMenuBar() {
    MenuBar menuBar = new MenuBar();

    fileMenu = new Menu("📁 File");
    newItem = new MenuItem("✨ New Session");
    newItem.setOnAction(e -> classroomController.newSession());

    saveItem = new MenuItem("💾 Save Session");
    saveItem.setOnAction(e -> saveSession());

    loadItem = new MenuItem("📂 Load Session");
    loadItem.setOnAction(e -> loadSession());

    exitItem = new MenuItem("🚪 Exit");
    exitItem.setOnAction(e -> primaryStage.close());

    fileMenu.getItems().addAll(newItem, new SeparatorMenuItem(), saveItem, loadItem,
        new SeparatorMenuItem(), exitItem);

    editMenu = new Menu("✏ Edit");
    undoItem = new MenuItem("⟲ Undo");
    undoItem.setOnAction(e -> classroomController.undo());

    redoItem = new MenuItem("⟳ Redo");
    redoItem.setOnAction(e -> classroomController.redo());

    editMenu.getItems().addAll(undoItem, redoItem);

    helpMenu = new Menu("ℹ Help");
    aboutItem = new MenuItem("ℹ About");
    aboutItem.setOnAction(e -> showAboutDialog());
    helpMenu.getItems().add(aboutItem);

    viewMenu = new Menu("👁 View");
    darkToggle = new CheckMenuItem("🌙 Dark Mode");
    darkToggle.setOnAction(e -> applyTheme(darkToggle.isSelected()));

    redactionToggle = new CheckMenuItem("🔒 Anonymize Data in Exports");
    redactionToggle.setOnAction(e -> classroomController.setRedactionEnabled(redactionToggle.isSelected()));

    viewMenu.getItems().addAll(darkToggle, redactionToggle);

    languageMenu = new Menu("🌐 Language");
    LanguageManager langManager = LanguageManager.getInstance();
    for (LanguageManager.Language lang : langManager.getAvailableLanguages()) {
      MenuItem langItem = new MenuItem("🗣 " + lang.getDisplayName());
      langItem.setOnAction(e -> {
        System.out.println("[DEBUG] Language changed to: " + lang.getDisplayName());
        langManager.setLanguage(lang);
        showAlert(Alert.AlertType.INFORMATION, "Language Changed",
            "Language preference saved. UI updated.");
      });
      languageMenu.getItems().add(langItem);
    }

    menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, languageMenu, helpMenu);
    // Apply initial translations
    applyTranslations();
    return menuBar;
  }

  private void applyTranslations() {
    LanguageManager lm = LanguageManager.getInstance();
    // Menus with icons
    fileMenu.setText("📁 " + lm.get("file"));
    editMenu.setText("✏ " + lm.get("edit"));
    viewMenu.setText("👁 " + lm.get("view"));
    helpMenu.setText("ℹ " + lm.get("help"));
    languageMenu.setText("🌐 " + lm.get("language"));
    // File items with icons
    newItem.setText("✨ " + lm.get("new_session"));
    saveItem.setText("💾 " + lm.get("save_session"));
    loadItem.setText("📂 " + lm.get("load_session"));
    exitItem.setText("🚪 " + lm.get("exit"));
    // Edit items with icons
    undoItem.setText("⟲ " + lm.get("undo"));
    redoItem.setText("⟳ " + lm.get("redo"));
    // View items with icons
    darkToggle.setText("🌙 " + lm.get("dark_mode"));
    redactionToggle.setText("🔒 " + lm.get("anonymize_exports"));
    // Help item with icon
    aboutItem.setText("ℹ " + lm.get("about"));
    // Tabs
    if (classroomTab != null)
      classroomTab.setText(lm.get("classroom_layout"));
    if (resultsTab != null)
      resultsTab.setText(lm.get("attendance_results"));
    if (conflictTab != null)
      conflictTab.setText(lm.get("conflict_analysis"));
    if (detailsTab != null)
      detailsTab.setText(lm.get("student_details"));
    if (historyTab != null)
      historyTab.setText(lm.get("analysis_history"));
    if (statsTab != null)
      statsTab.setText(lm.get("statistics"));
    if (settingsTab != null)
      settingsTab.setText(lm.get("settings"));
    if (auditTab != null)
      auditTab.setText(lm.get("audit_log"));
    // Audit view controls
    if (auditTitleLabel != null)
      auditTitleLabel.setText(lm.get("audit_log"));
    if (auditRefreshButton != null)
      auditRefreshButton.setText(lm.get("refresh"));
    if (auditClearButton != null)
      auditClearButton.setText(lm.get("clear_log"));
  }

  private void saveSession() {
    try {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Save Session");
      fileChooser.getExtensionFilters().add(
          new FileChooser.ExtensionFilter("Session Files (*.session)", "*.session"));
      fileChooser.setInitialDirectory(new File(System.getProperty("user.home"), "Documents"));

      File file = fileChooser.showSaveDialog(primaryStage);
      if (file != null) {
        SessionManager.saveSession(classroomController.getClassroom(), file);
        showAlert(Alert.AlertType.INFORMATION, "Success", "Session saved to:\n" + file.getAbsolutePath());
      }
    } catch (Exception e) {
      showAlert(Alert.AlertType.ERROR, "Error", "Failed to save session: " + e.getMessage());
    }
  }

  private void loadSession() {
    try {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Load Session");
      fileChooser.getExtensionFilters().add(
          new FileChooser.ExtensionFilter("Session Files (*.session)", "*.session"));
      fileChooser.setInitialDirectory(new File(System.getProperty("user.home"), "Documents"));

      File file = fileChooser.showOpenDialog(primaryStage);
      if (file != null) {
        Classroom classroom = SessionManager.loadSession(file);
        classroomController.loadClassroom(classroom);
        showAlert(Alert.AlertType.INFORMATION, "Success", "Session loaded from:\n" + file.getAbsolutePath());
      }
    } catch (Exception e) {
      showAlert(Alert.AlertType.ERROR, "Error", "Failed to load session: " + e.getMessage());
    }
  }

  private void showAlert(Alert.AlertType type, String title, String message) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  private Node createClassroomView() {
    classroomController = new ClassroomController();
    return classroomController.getView();
  }

  private Node createResultsView() {
    return new ResultsController(classroomController).getView();
  }

  private Node createSettingsView() {
    return new SettingsController().getView();
  }

  private Node createAuditLogView() {
    VBox mainLayout = new VBox(15);
    mainLayout.setPadding(new Insets(20));
    mainLayout.getStyleClass().add("card");

    // Header with icon
    HBox headerBox = new HBox(15);
    headerBox.setAlignment(Pos.CENTER_LEFT);
    
    Circle iconCircle = new Circle(25);
    iconCircle.setFill(Color.web("#E74C3C"));
    Label iconLabel = new Label("📋");
    iconLabel.setStyle("-fx-font-size: 28px; -fx-text-fill: white;");
    iconLabel.setTranslateX(-25);
    iconLabel.setTranslateY(-25);
    
    auditTitleLabel = new Label(LanguageManager.getInstance().get("audit_log"));
    auditTitleLabel.getStyleClass().add("label-header");
    
    Region spacer = new Region();
    HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
    
    Label entryCountLabel = new Label("0 entries");
    entryCountLabel.getStyleClass().add("label-secondary");
    
    headerBox.getChildren().addAll(iconCircle, iconLabel, auditTitleLabel, spacer, entryCountLabel);

    // Audit Log Table
    TableView<AuditLogEntry> auditTable = new TableView<>();
    auditTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

    TableColumn<AuditLogEntry, String> timestampCol = new TableColumn<>("Date & Time");
    timestampCol.setCellValueFactory(cellData ->
        new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTimestamp()));
    timestampCol.setMinWidth(160);
    timestampCol.setPrefWidth(180);

    TableColumn<AuditLogEntry, String> actionCol = new TableColumn<>("Action");
    actionCol.setCellValueFactory(cellData ->
        new javafx.beans.property.SimpleStringProperty(cellData.getValue().getAction()));
    actionCol.setMinWidth(150);

    TableColumn<AuditLogEntry, String> detailsCol = new TableColumn<>("Details");
    detailsCol.setCellValueFactory(cellData ->
        new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDetails()));
    detailsCol.setMinWidth(350);
    
    // Enable text wrapping in details column
    detailsCol.setCellFactory(tc -> {
      TableCell<AuditLogEntry, String> cell = new TableCell<>();
      Label label = new Label();
      label.setWrapText(true);
      label.setMaxWidth(Double.MAX_VALUE);
      cell.setGraphic(label);
      cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
      label.textProperty().bind(cell.itemProperty());
      return cell;
    });

    auditTable.getColumns().addAll(timestampCol, actionCol, detailsCol);
    auditTable.setPlaceholder(new Label(LanguageManager.getInstance().get("no_data")));

    // Load audit log data
    ObservableList<AuditLogEntry> auditData = FXCollections.observableArrayList();
    loadAuditLogData(auditData);
    auditTable.setItems(auditData);
    entryCountLabel.setText(auditData.size() + " entries");

    VBox.setVgrow(auditTable, javafx.scene.layout.Priority.ALWAYS);

    // Button Box
    HBox buttonBox = new HBox(10);
    buttonBox.setAlignment(Pos.CENTER_LEFT);

    auditRefreshButton = new Button(LanguageManager.getInstance().get("refresh"));
    auditRefreshButton.getStyleClass().add("button-success");
    auditRefreshButton.setOnAction(e -> {
      auditData.clear();
      loadAuditLogData(auditData);
      auditTable.setItems(auditData);
      entryCountLabel.setText(auditData.size() + " entries");
    });

    auditClearButton = new Button(LanguageManager.getInstance().get("clear_log"));
    auditClearButton.getStyleClass().add("button-danger");
    auditClearButton.setOnAction(e -> {
      Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
      confirm.setTitle(LanguageManager.getInstance().get("confirm"));
      confirm.setHeaderText(LanguageManager.getInstance().get("are_you_sure"));
      confirm.setContentText(LanguageManager.getInstance().get("clear_log_confirm"));
      if (confirm.showAndWait().get() == ButtonType.OK) {
        AuditLogger.clearAuditLog();
        auditData.clear();
        auditTable.setItems(auditData);
        entryCountLabel.setText("0 entries");
      }
    });

    buttonBox.getChildren().addAll(auditRefreshButton, auditClearButton);

    mainLayout.getChildren().addAll(headerBox, auditTable, buttonBox);
    
    ScrollPane scrollPane = new ScrollPane(mainLayout);
    scrollPane.setFitToWidth(true);
    return scrollPane;
  }

  private void loadAuditLogData(ObservableList<AuditLogEntry> data) {
    String logContent = AuditLogger.readAuditLog();
    if (logContent == null || logContent.isEmpty() || logContent.contains("No audit log")) {
      return;
    }
    
    String[] lines = logContent.split("\n");
    for (String line : lines) {
      if (line.trim().isEmpty()) continue;
      // Parse format: [yyyy-MM-dd HH:mm:ss] ACTION: details
      if (line.startsWith("[") && line.contains("] ")) {
        int closeIdx = line.indexOf("]");
        String timestamp = line.substring(1, closeIdx);
        
        String rest = line.substring(closeIdx + 2); // Skip "] "
        int colonIdx = rest.indexOf(":");
        if (colonIdx > 0) {
          String action = rest.substring(0, colonIdx);
          String details = rest.substring(colonIdx + 1).trim();
          data.add(new AuditLogEntry(timestamp, action, details));
        }
      }
    }
  }

  // Helper class for audit log entries
  public static class AuditLogEntry {
    private final String timestamp;
    private final String action;
    private final String details;

    public AuditLogEntry(String timestamp, String action, String details) {
      this.timestamp = timestamp;
      this.action = action;
      this.details = details;
    }

    public String getTimestamp() {
      return timestamp;
    }

    public String getAction() {
      return action;
    }

    public String getDetails() {
      return details;
    }
  }

  private void showAboutDialog() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("About Smart Attendance System");
    alert.setHeaderText("Multi-Strategy Smart Attendance System v1.0");
    
    // Create rich content with better formatting
    VBox content = new VBox(15);
    content.setPadding(new Insets(10));
    
    Label descLabel = new Label("A distributed attendance verification system using peer-confirmation networks.");
    descLabel.setWrapText(true);
    descLabel.setStyle("-fx-font-size: 13px;");
    
    Label featuresHeader = new Label("Key Features:");
    featuresHeader.getStyleClass().add("label-header");
    
    VBox features = new VBox(5);
    features.setPadding(new Insets(0, 0, 0, 15));
    String[] featureList = {
      "✓ Neighbor-based verification with directional claims",
      "✓ Intelligent seat occupancy analysis",
      "✓ Peer consensus scoring algorithm",
      "✓ Advanced conflict detection and resolution",
      "✓ Multi-language support (EN, FR, AR)",
      "✓ Export to PDF, Excel, CSV, JSON, and PNG"
    };
    for (String feature : featureList) {
      Label l = new Label(feature);
      l.setStyle("-fx-font-size: 12px;");
      features.getChildren().add(l);
    }
    
    Label shortcutsHeader = new Label("Keyboard Shortcuts:");
    shortcutsHeader.getStyleClass().add("label-header");
    
    VBox shortcuts = new VBox(5);
    shortcuts.setPadding(new Insets(0, 0, 0, 15));
    String[] shortcutList = {
      "Alt+A: Analyze Attendance",
      "Ctrl+S: Save Session",
      "Ctrl+L: Load Session",
      "Ctrl+Z: Undo",
      "Ctrl+Y: Redo"
    };
    for (String shortcut : shortcutList) {
      Label l = new Label(shortcut);
      l.setStyle("-fx-font-size: 12px; -fx-font-family: 'Courier New', monospace;");
      shortcuts.getChildren().add(l);
    }
    
    content.getChildren().addAll(descLabel, featuresHeader, features, shortcutsHeader, shortcuts);
    alert.getDialogPane().setContent(content);
    alert.getDialogPane().setPrefWidth(500);
    alert.showAndWait();
  }

  private void setupKeyboardShortcuts() {
    scene.setOnKeyPressed(event -> {
      if (event.isAltDown() && event.getCode().getName().equals("A")) {
        classroomController.analyzeAttendance();
      } else if (event.isControlDown() && event.getCode().getName().equals("S")) {
        saveSession();
      } else if (event.isControlDown() && event.getCode().getName().equals("L")) {
        loadSession();
      } else if (event.isControlDown() && event.getCode().getName().equals("Z")) {
        classroomController.undo();
      } else if (event.isControlDown() && event.getCode().getName().equals("Y")) {
        classroomController.redo();
      }
    });
  }

  private void applyTheme(boolean dark) {
    if (scene == null) {
      return;
    }

    if (dark) {
      scene.getRoot().getStyleClass().add("dark-mode");
      System.out.println("[UI] Dark mode enabled");
    } else {
      scene.getRoot().getStyleClass().remove("dark-mode");
      System.out.println("[UI] Light mode enabled");
    }
  }

  private void updateLanguageStatusLabel() {
    LanguageManager.Language current = LanguageManager.getInstance().getCurrentLanguage();
    languageStatusLabel.setText("Language: " + current.getDisplayName());
    System.out.println("[DEBUG] Status label updated to: " + current.getDisplayName());
  }

  private void setupTabAnimations(TabPane tabPane) {
    tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
      if (newTab != null && newTab.getContent() != null) {
        // Fade in animation when tab is selected
        AnimationUtils.fadeIn(newTab.getContent(), javafx.util.Duration.millis(300)).play();
      }
    });
  }

  /**
   * Show an animated alert dialog.
   */
  public void showAnimatedAlert(Alert.AlertType type, String title, String header, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(header);
    alert.setContentText(content);
    
    // Animate dialog appearance
    alert.setOnShown(e -> {
      if (alert.getDialogPane() != null) {
        AnimationUtils.fadeScaleIn(alert.getDialogPane(), javafx.util.Duration.millis(300)).play();
      }
    });
    
    alert.showAndWait();
  }

  /**
   * Show an animated confirmation dialog.
   */
  public boolean showAnimatedConfirmation(String title, String header, String content) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle(title);
    alert.setHeaderText(header);
    alert.setContentText(content);
    
    // Animate dialog appearance
    alert.setOnShown(e -> {
      if (alert.getDialogPane() != null) {
        AnimationUtils.fadeScaleIn(alert.getDialogPane(), javafx.util.Duration.millis(300)).play();
      }
    });
    
    return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
  }

  public static void main(String[] args) {
    launch(args);
  }
}
