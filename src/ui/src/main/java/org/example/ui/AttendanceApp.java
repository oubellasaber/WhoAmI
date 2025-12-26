package org.example.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
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

    // Add language status label at the bottom
    languageStatusLabel = new Label();
    languageStatusLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #333333;");
    updateLanguageStatusLabel();
    LanguageManager.getInstance().addLanguageChangeListener(lang -> {
      System.out.println("[DEBUG] AttendanceApp listener triggered! Language changed to: " + lang);
      updateLanguageStatusLabel();
      applyTranslations();
    });

    HBox bottomBar = new HBox(10);
    bottomBar.setPadding(new Insets(8, 15, 8, 15));
    bottomBar.setStyle("-fx-border-color: #d0d0d0; -fx-border-width: 1 0 0 0; -fx-background-color: #f9f9f9;");
    bottomBar.getChildren().add(languageStatusLabel);
    mainLayout.setBottom(bottomBar);

    // Create scene
    scene = new Scene(mainLayout);
    primaryStage.setScene(scene);
    // Apply translations to tabs now that they're created
    applyTranslations();
    applyTheme(false);
    primaryStage.show();
  }

  private MenuBar createMenuBar() {
    MenuBar menuBar = new MenuBar();

    fileMenu = new Menu("File");
    newItem = new MenuItem("New Session");
    newItem.setOnAction(e -> classroomController.newSession());

    saveItem = new MenuItem("Save Session");
    saveItem.setOnAction(e -> saveSession());

    loadItem = new MenuItem("Load Session");
    loadItem.setOnAction(e -> loadSession());

    exitItem = new MenuItem("Exit");
    exitItem.setOnAction(e -> primaryStage.close());

    fileMenu.getItems().addAll(newItem, new SeparatorMenuItem(), saveItem, loadItem,
        new SeparatorMenuItem(), exitItem);

    editMenu = new Menu("Edit");
    undoItem = new MenuItem("Undo");
    undoItem.setOnAction(e -> classroomController.undo());

    redoItem = new MenuItem("Redo");
    redoItem.setOnAction(e -> classroomController.redo());

    editMenu.getItems().addAll(undoItem, redoItem);

    helpMenu = new Menu("Help");
    aboutItem = new MenuItem("About");
    aboutItem.setOnAction(e -> showAboutDialog());
    helpMenu.getItems().add(aboutItem);

    viewMenu = new Menu("View");
    darkToggle = new CheckMenuItem("Dark Mode");
    darkToggle.setOnAction(e -> applyTheme(darkToggle.isSelected()));

    redactionToggle = new CheckMenuItem("Anonymize Data in Exports");
    redactionToggle.setOnAction(e -> classroomController.setRedactionEnabled(redactionToggle.isSelected()));

    viewMenu.getItems().addAll(darkToggle, redactionToggle);

    languageMenu = new Menu("Language");
    LanguageManager langManager = LanguageManager.getInstance();
    for (LanguageManager.Language lang : langManager.getAvailableLanguages()) {
      MenuItem langItem = new MenuItem(lang.getDisplayName());
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
    // Menus
    fileMenu.setText(lm.get("file"));
    editMenu.setText(lm.get("edit"));
    viewMenu.setText(lm.get("view"));
    helpMenu.setText(lm.get("help"));
    languageMenu.setText(lm.get("language"));
    // File items
    newItem.setText(lm.get("new_session"));
    saveItem.setText(lm.get("save_session"));
    loadItem.setText(lm.get("load_session"));
    exitItem.setText(lm.get("exit"));
    // Edit items
    undoItem.setText(lm.get("undo"));
    redoItem.setText(lm.get("redo"));
    // View items
    darkToggle.setText(lm.get("dark_mode"));
    redactionToggle.setText(lm.get("anonymize_exports"));
    // Help item
    aboutItem.setText(lm.get("about"));
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
        com.whoami.domain.entities.Classroom classroom = SessionManager.loadSession(file);
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
    VBox mainLayout = new VBox(10);
    mainLayout.setPadding(new Insets(15));

    auditTitleLabel = new Label(LanguageManager.getInstance().get("audit_log"));
    auditTitleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

    TextArea auditTextArea = new TextArea();
    auditTextArea.setEditable(false);
    auditTextArea.setWrapText(true);
    auditTextArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10;");
    auditTextArea.setText(AuditLogger.readAuditLog());

    HBox buttonBox = new HBox(10);
    buttonBox.setPadding(new Insets(10));

    auditRefreshButton = new Button(LanguageManager.getInstance().get("refresh"));
    auditRefreshButton.setPrefWidth(100);
    auditRefreshButton.setOnAction(e -> auditTextArea.setText(AuditLogger.readAuditLog()));

    auditClearButton = new Button(LanguageManager.getInstance().get("clear_log"));
    auditClearButton.setPrefWidth(120);
    auditClearButton.setStyle("-fx-text-fill: #e74c3c;");
    auditClearButton.setOnAction(e -> {
      Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
      confirm.setTitle(LanguageManager.getInstance().get("confirm"));
      confirm.setHeaderText(LanguageManager.getInstance().get("are_you_sure"));
      confirm.setContentText(LanguageManager.getInstance().get("clear_log_confirm"));
      if (confirm.showAndWait().get() == ButtonType.OK) {
        AuditLogger.clearAuditLog();
        auditTextArea.setText(LanguageManager.getInstance().get("audit_log_cleared"));
      }
    });

    buttonBox.getChildren().addAll(auditRefreshButton, auditClearButton);

    mainLayout.getChildren().addAll(auditTitleLabel, auditTextArea, buttonBox);
    return new ScrollPane(mainLayout);
  }

  private void showAboutDialog() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("About Smart Attendance System");
    alert.setHeaderText("Multi-Strategy Smart Attendance System v1.0");
    alert.setContentText("A distributed attendance verification system using peer-confirmation networks.\n\n" +
        "Features:\n" +
        "• Neighbor-based verification\n" +
        "• Seat occupancy analysis\n" +
        "• Consensus scoring\n" +
        "• Conflict detection\n\n" +
        "Keyboard Shortcuts:\n" +
        "Alt+A: Analyze Attendance\n" +
        "Ctrl+S: Save Session\n" +
        "Ctrl+L: Load Session");
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
      scene.getRoot().setStyle(
          "-fx-base: #1e1e1e; -fx-background: #1e1e1e; -fx-control-inner-background: #2a2a2a; -fx-text-fill: #f3f3f3;");
    } else {
      scene.getRoot().setStyle("");
    }
  }

  private void updateLanguageStatusLabel() {
    LanguageManager.Language current = LanguageManager.getInstance().getCurrentLanguage();
    languageStatusLabel.setText("Language: " + current.getDisplayName());
    System.out.println("[DEBUG] Status label updated to: " + current.getDisplayName());
  }

  public static void main(String[] args) {
    launch(args);
  }
}
