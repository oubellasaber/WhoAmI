package org.example.ui;

import com.whoami.domain.entities.*;
import com.whoami.domain.attendance.AttendanceReport;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Controller for the classroom view.
 * Manages classroom layout, student placement, and claim recording.
 */
public class ClassroomController {
  private Classroom classroom;
  private Map<Student, Button> studentButtons;
  private ComboBox<Integer> rowsCombo;
  private ComboBox<Integer> colsCombo;
  private ComboBox<String> claimerCombo;
  private ComboBox<String> targetCombo;
  private ComboBox<Direction> directionCombo;
  private ListView<String> claimsListView;
  private ListView<String> studentsListView;
  private Label statusLabel;
  // Translatable UI elements
  private Label setupLabel;
  private Label rowLabel;
  private Label colLabel;
  private Button createButton;
  private Button analyzeButton;
  private Button testDataButton;
  private Button validateButton;
  private Button importCsvButton;
  private Button selectAllButton;
  private Button deselectAllButton;
  private Button batchPresentButton;
  private Button batchAbsentButton;
  private Label classroomTitleLabel;
  private Label studentsLabel;
  private TextField studentNameField;
  private Button addStudentButton;
  private Label claimsHeaderLabel;
  private Button addClaimButton;
  private Map<String, LocatedStudent> studentRegistry;
  private List<Claim> claims;
  private AttendanceService attendanceService;
  private Runnable onAnalysisComplete;
  private GridPane classroomGrid;
  private StudentDetailController studentDetailController;
  private Tab detailsTab;
  private HistoryController historyController;
  private Set<String> selectedStudents;
  private Map<String, AttendanceReport.AttendanceStatus> manualOverrides;
  private UndoRedoManager undoRedoManager;
  private boolean redactionEnabled;

  public ClassroomController() {
    this.studentButtons = new HashMap<>();
    this.studentRegistry = new HashMap<>();
    this.claims = new ArrayList<>();
    this.attendanceService = new AttendanceService();
    this.selectedStudents = new HashSet<>();
    this.manualOverrides = new HashMap<>();
    this.undoRedoManager = new UndoRedoManager(100);
    this.redactionEnabled = false;
  }

  /**
   * Load test data for demonstration.
   */
  private void loadTestData() {
    // Clear existing data
    studentRegistry.clear();
    claims.clear();
    if (studentsListView != null)
      studentsListView.getItems().clear();
    if (claimsListView != null)
      claimsListView.getItems().clear();
    if (claimerCombo != null)
      claimerCombo.getItems().clear();
    if (targetCombo != null)
      targetCombo.getItems().clear();

    // Create classroom
    classroom = new Classroom(3, 4);
    attendanceService.setClassroom(classroom);

    // Add test students with positions
    String[][] testStudents = {
        { "Fatima", "A" },
        { "Ahmed", "B" },
        { "Mohamed", "C" },
        { "Hanane", "D" },
        { Sara", "E" },
        { "Youssef", "F" }, // Absent student - no position
        { "Yassine", "G" } // Uncertain student - conflicting claims
    };

    int[][] positions = {
        { 0, 0 }, // Fatima at row 0, col 0
        { 0, 1 }, // Bob at row 0, col 1
        { 0, 2 }, // Mohamed at row 0, col 2
        { 1, 0 }, // Hanane at row 1, col 0
        { 1, 1 }, // Eve at row 1, col 1
        { -1, -1 }, // Youssef - no position (absent)
        { 1, 2 } // Yassine at row 1, col 2 - will have weak/conflicting claims
    };

    for (int i = 0; i < testStudents.length; i++) {
      String name = testStudents[i][0];
      String id = testStudents[i][1];
      Student s = new Student(id, name);
      LocatedStudent located = new LocatedStudent(s);

      // Assign position to student only if valid
      if (positions[i][0] >= 0 && positions[i][1] >= 0) {
        Position pos = new Position(positions[i][0], positions[i][1]);
        located.setPosition(pos);

        // Place student in the classroom
        classroom.place(located);
      }
      // Youssef has no position - will be marked ABSENT

      studentRegistry.put(name, located);

      // Add to UI lists
      if (studentsListView != null) {
        studentsListView.getItems().add(name + " (ID: " + id + ")");
      }

      if (claimerCombo != null) {
        claimerCombo.getItems().add(name);
      }
      if (targetCombo != null) {
        targetCombo.getItems().add(name);
      }
    }

    // Add test claims - INTENTIONALLY INCOMPLETE to demonstrate validation
    // Fatima (0,0): COMPLETE - has Bob RIGHT and Hanane BACK ✓
    addTestClaim("Fatima", "Ahmed", Direction.RIGHT);
    addTestClaim("Fatima", "Hanane", Direction.BACK);

    // Bob (0,1): COMPLETE - has Fatima LEFT, Mohamed RIGHT, and Eve BACK ✓
    addTestClaim("Ahmed", "Fatima", Direction.LEFT);
    addTestClaim("Ahmed", "Mohamed", Direction.RIGHT);
    addTestClaim("Ahmed", Sara", Direction.BACK);

    // Mohamed (0,2): has Bob LEFT and Yassine BACK ✓
    addTestClaim("Mohamed", "Ahmed", Direction.LEFT);
    addTestClaim("Mohamed", "Yassine", Direction.BACK); // Yassine is at (1,2) behind Mohamed

    // Hanane (1,0): COMPLETE - has Eve RIGHT and Fatima FRONT ✓
    addTestClaim("Hanane", Sara", Direction.RIGHT);
    addTestClaim("Hanane", "Fatima", Direction.FRONT);

    // Eve (1,1): has Hanane LEFT, Bob FRONT, declares RIGHT is Yassine
    addTestClaim(Sara", "Hanane", Direction.LEFT);
    addTestClaim(Sara", "Ahmed", Direction.FRONT);
    addTestClaim(Sara", "Yassine", Direction.RIGHT); // Yassine is to the right
    addAbsentClaim(Sara", Direction.BACK); // Position (2,1) is empty

    // Yassine (1,2): LIAR - claims absent student Youssef is her neighbor
    // This creates UNCERTAIN status through conflict detection
    addTestClaim("Yassine", "Mohamed", Direction.FRONT); // Correct: Mohamed is at (0,2)
    addTestClaim("Yassine", Sara", Direction.LEFT); // Correct: Eve is at (1,1)
    addTestClaim("Yassine", "Youssef", Direction.BACK); // LYING: Youssef is absent, not at (2,2)!
    // Yassine's uncertainty will come from claiming an absent student

    // Youssef makes no claims and has no position

    updateClaimsList();
    updateClassroomVisualization();
    statusLabel.setText(
        "✓ Test data loaded: 7 students - Fatima/Bob/Mohamed/Hanane/Eve: PRESENT, Yassine: UNCERTAIN (liar claiming absent Youssef), Youssef: ABSENT");
  }

  private void importFromCsv() {
    Window window = statusLabel != null && statusLabel.getScene() != null ? statusLabel.getScene().getWindow() : null;

    FileChooser chooser = new FileChooser();
    chooser.setTitle("Import Students from CSV");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
    File file = chooser.showOpenDialog(window);
    if (file == null) {
      return;
    }

    List<String[]> rows = new ArrayList<>();
    Map<String, Integer> idx = new HashMap<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String headerLine = reader.readLine();
      if (headerLine == null) {
        showAlert("Import Failed", "CSV is empty.");
        return;
      }

      String[] headers = headerLine.split(",");
      for (int i = 0; i < headers.length; i++) {
        idx.put(headers[i].trim().toLowerCase(), i);
      }

      if (!idx.containsKey("id") || !idx.containsKey("name")) {
        showAlert("Import Failed", "CSV must have at least 'id' and 'name' headers.");
        return;
      }

      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().isEmpty()) {
          continue;
        }
        rows.add(line.split(","));
      }
    } catch (IOException e) {
      showAlert("Import Failed", "Could not read file: " + e.getMessage());
      return;
    }

    if (rows.isEmpty()) {
      showAlert("Import Failed", "No data rows found.");
      return;
    }

    studentRegistry.clear();
    claims.clear();
    if (claimsListView != null)
      claimsListView.getItems().clear();
    if (studentsListView != null)
      studentsListView.getItems().clear();
    if (claimerCombo != null)
      claimerCombo.getItems().clear();
    if (targetCombo != null)
      targetCombo.getItems().clear();

    int maxRow = -1;
    int maxCol = -1;

    List<LocatedStudent> imported = new ArrayList<>();
    Set<String> seenIds = new HashSet<>();
    Set<String> seenNames = new HashSet<>();
    List<String> duplicates = new ArrayList<>();

    for (String[] cols : rows) {
      String id = getColumnValue(cols, idx.get("id"));
      String name = getColumnValue(cols, idx.get("name"));
      if (id.isEmpty() || name.isEmpty()) {
        continue;
      }

      // Check for duplicates
      if (seenIds.contains(id)) {
        duplicates.add("Student ID '" + id + "' appears multiple times");
      } else if (seenNames.contains(name)) {
        duplicates.add("Student name '" + name + "' appears multiple times");
      } else {
        seenIds.add(id);
        seenNames.add(name);

        Integer row = idx.containsKey("row") ? parseIntSafe(getColumnValue(cols, idx.get("row"))) : null;
        Integer col = idx.containsKey("col") ? parseIntSafe(getColumnValue(cols, idx.get("col"))) : null;

        Student s = new Student(id, name);
        LocatedStudent located = new LocatedStudent(s);
        if (row != null && col != null && row >= 0 && col >= 0) {
          located.setPosition(new Position(row, col));
          maxRow = Math.max(maxRow, row);
          maxCol = Math.max(maxCol, col);
        }
        imported.add(located);
      }
    }

    if (!duplicates.isEmpty()) {
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.setTitle("Duplicate Students Detected");
      alert.setHeaderText("The following duplicates were skipped:");
      alert.setContentText(String.join("\n", duplicates));
      alert.showAndWait();
      AuditLogger.log("IMPORT_DUPLICATES", duplicates.size() + " duplicates found and skipped");
    }

    if (imported.isEmpty()) {
      showAlert("Import Failed", "No valid students found in file.");
      return;
    }

    int rowsCount = maxRow >= 0 ? maxRow + 1 : Math.max(3, rowsCombo.getValue());
    int colsCount = maxCol >= 0 ? maxCol + 1 : Math.max(3, colsCombo.getValue());

    classroom = new Classroom(rowsCount, colsCount);
    rowsCombo.setValue(rowsCount);
    colsCombo.setValue(colsCount);
    attendanceService.setClassroom(classroom);

    for (LocatedStudent ls : imported) {
      studentRegistry.put(ls.getStudent().getId(), ls);
      if (ls.getPosition() != null) {
        classroom.place(ls);
      }

      if (studentsListView != null) {
        studentsListView.getItems().add(ls.getStudent().getName() + " (ID: " + ls.getStudent().getId() + ")");
      }
      if (claimerCombo != null)
        claimerCombo.getItems().add(ls.getStudent().getName());
      if (targetCombo != null)
        targetCombo.getItems().add(ls.getStudent().getName());
    }

    updateClassroomVisualization();
    updateClaimsList();
    statusLabel.setText("✓ Imported " + imported.size() + " students from CSV");
    AuditLogger.log("IMPORT_CSV", "Imported " + imported.size() + " students");
  }

  private Integer parseIntSafe(String value) {
    try {
      return Integer.parseInt(value.trim());
    } catch (Exception e) {
      return null;
    }
  }

  private String getColumnValue(String[] cols, int idx) {
    return idx < cols.length ? cols[idx].trim() : "";
  }

  private void showAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  private void addTestClaim(String claimer, String target, Direction direction) {
    LocatedStudent claimerLocated = studentRegistry.get(claimer);
    LocatedStudent targetLocated = studentRegistry.get(target);

    if (claimerLocated != null && targetLocated != null) {
      Claim claim = new Claim(direction, targetLocated.getStudent());
      claimerLocated.addClaim(claim);
      claims.add(claim);
    }
  }

  /**
   * Add a claim that a position is absent/empty.
   */
  private void addAbsentClaim(String claimer, Direction direction) {
    LocatedStudent claimerLocated = studentRegistry.get(claimer);
    if (claimerLocated != null) {
      Claim claim = new Claim(direction, null); // null target = absent
      claimerLocated.addClaim(claim);
      claims.add(claim);
    }
  }

  public void setOnAnalysisComplete(Runnable callback) {
    this.onAnalysisComplete = callback;
  }

  public void setStudentDetailController(StudentDetailController controller) {
    this.studentDetailController = controller;
    // Refresh visualization if it already exists
    if (classroomGrid != null && !classroomGrid.getChildren().isEmpty()) {
      updateClassroomVisualization();
    }
  }

  public void setDetailsTab(Tab tab) {
    this.detailsTab = tab;
  }

  public void setHistoryController(HistoryController controller) {
    this.historyController = controller;
  }

  public Node getView() {
    VBox mainLayout = new VBox(10);
    mainLayout.setPadding(new Insets(15));

    // Control panel
    HBox controlPanel = createControlPanel();
    mainLayout.getChildren().add(controlPanel);

    // Classroom grid and claims
    HBox contentArea = new HBox(15);
    contentArea.setPrefHeight(700);

    // Classroom grid
    VBox classroomPanel = createClassroomPanel();
    classroomPanel.setPrefWidth(500);

    // Claims and info panel
    VBox infoPanel = createInfoPanel();
    infoPanel.setPrefWidth(400);

    contentArea.getChildren().addAll(classroomPanel, infoPanel);
    HBox.setHgrow(classroomPanel, Priority.ALWAYS);
    HBox.setHgrow(infoPanel, Priority.ALWAYS);

    mainLayout.getChildren().add(contentArea);

    // Status bar
    statusLabel = new Label(LanguageManager.getInstance().get("status_ready"));
    statusLabel.setStyle("-fx-padding: 10; -fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");
    mainLayout.getChildren().add(statusLabel);

    ScrollPane sp = new ScrollPane(mainLayout);
    // Listen for language changes to update labels/buttons live
    LanguageManager.getInstance().addLanguageChangeListener(lang -> updateLanguageTexts());
    // Apply current language immediately
    updateLanguageTexts();
    return sp;
  }

  private HBox createControlPanel() {
    HBox panel = new HBox(15);
    panel.setPadding(new Insets(10));
    panel.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
    rowLabel = new Label(LanguageManager.getInstance().get("rows"));
    rowsCombo = new ComboBox<>();
    rowsCombo.getItems().addAll(2, 3, 4, 5);
    rowsCombo.setValue(3);
    colLabel = new Label(LanguageManager.getInstance().get("cols"));
    colsCombo = new ComboBox<>();
    colsCombo.getItems().addAll(2, 3, 4, 5);
    colsCombo.setValue(4);
    createButton = new Button(LanguageManager.getInstance().get("create"));
    createButton.setPrefWidth(150);
    createButton.setStyle("-fx-font-size: 11; -fx-padding: 8;");
    createButton.setOnAction(e -> createClassroom());
    analyzeButton = new Button(LanguageManager.getInstance().get("analyze"));
    analyzeButton.setPrefWidth(150);
    analyzeButton.setStyle("-fx-font-size: 11; -fx-padding: 8;");
    analyzeButton.setOnAction(e -> analyzeAttendance());
    testDataButton = new Button(LanguageManager.getInstance().get("test_data"));
    testDataButton.setPrefWidth(150);
    testDataButton.setStyle("-fx-font-size: 11; -fx-padding: 8; -fx-text-fill: #0066cc;");
    testDataButton.setOnAction(e -> loadTestData());
    validateButton = new Button(LanguageManager.getInstance().get("validate"));
    validateButton.setPrefWidth(150);
    validateButton.setStyle("-fx-font-size: 11; -fx-padding: 8; -fx-text-fill: #ff6600;");
    validateButton.setOnAction(e -> validateNeighbors());
    importCsvButton = new Button(LanguageManager.getInstance().get("import_csv"));
    importCsvButton.setPrefWidth(130);
    importCsvButton.setStyle("-fx-font-size: 11; -fx-padding: 8; -fx-text-fill: #007700;");
    importCsvButton.setOnAction(e -> importFromCsv());
    selectAllButton = new Button(LanguageManager.getInstance().get("select_all"));
    selectAllButton.setPrefWidth(100);
    selectAllButton.setStyle("-fx-font-size: 11; -fx-padding: 8;");
    selectAllButton.setOnAction(e -> selectAllStudents());
    deselectAllButton = new Button(LanguageManager.getInstance().get("deselect_all"));
    deselectAllButton.setPrefWidth(110);
    deselectAllButton.setStyle("-fx-font-size: 11; -fx-padding: 8;");
    deselectAllButton.setOnAction(e -> deselectAllStudents());
    batchPresentButton = new Button(LanguageManager.getInstance().get("mark_present"));
    batchPresentButton.setPrefWidth(150);
    batchPresentButton.setStyle("-fx-font-size: 11; -fx-padding: 8; -fx-text-fill: #2ecc71;");
    batchPresentButton.setOnAction(e -> batchMarkPresent());
    batchAbsentButton = new Button(LanguageManager.getInstance().get("mark_absent"));
    batchAbsentButton.setPrefWidth(150);
    batchAbsentButton.setStyle("-fx-font-size: 11; -fx-padding: 8; -fx-text-fill: #e74c3c;");
    batchAbsentButton.setOnAction(e -> batchMarkAbsent());

    setupLabel = new Label(LanguageManager.getInstance().get("setup_classroom"));
    panel.getChildren().addAll(
        setupLabel,
        rowLabel, rowsCombo,
        colLabel, colsCombo,
        createButton,
        new Separator(javafx.geometry.Orientation.VERTICAL),
        analyzeButton,
        testDataButton,
        validateButton,
        importCsvButton,
        new Separator(javafx.geometry.Orientation.VERTICAL),
        selectAllButton, deselectAllButton,
        batchPresentButton, batchAbsentButton);

    return panel;
  }

  private VBox createClassroomPanel() {
    VBox panel = new VBox(10);
    panel.setPadding(new Insets(10));
    panel.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1;");

    classroomTitleLabel = new Label(LanguageManager.getInstance().get("classroom_layout"));
    classroomTitleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

    ScrollPane gridScroll = new ScrollPane();
    classroomGrid = new GridPane();
    classroomGrid.setHgap(5);
    classroomGrid.setVgap(5);
    classroomGrid.setPadding(new Insets(10));
    classroomGrid.setStyle("-fx-border-color: #f0f0f0;");

    gridScroll.setContent(classroomGrid);
    gridScroll.setFitToWidth(true);
    gridScroll.setFitToHeight(true);

    panel.getChildren().addAll(classroomTitleLabel, gridScroll);

    return panel;
  }

  /**
   * Update classroom grid visualization with students.
   */
  private void updateClassroomVisualization() {
    if (classroom == null || classroomGrid == null)
      return;

    classroomGrid.getChildren().clear();

    int rows = classroom.getRows();
    int cols = classroom.getCols();

    // Create grid cells
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        VBox cell = createClassroomCell(row, col);
        classroomGrid.add(cell, col, row);
      }
    }
  }

  /**
   * Create a single classroom cell with optional student.
   */
  private VBox createClassroomCell(int row, int col) {
    VBox cell = new VBox();
    cell.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-background-color: #f9f9f9;");
    cell.setPrefWidth(80);
    cell.setPrefHeight(80);
    cell.setAlignment(Pos.CENTER);
    cell.setPadding(new Insets(5));

    Position pos = new Position(row, col);
    Optional<LocatedStudent> student = classroom.getAt(pos);

    if (student.isPresent()) {
      LocatedStudent located = student.get();
      String name = located.getStudent().getName();
      String id = located.getStudent().getId();
      String key = name; // registry is keyed by student name
      int claimCount = located.getClaims().size();

      CheckBox selectCheckBox = new CheckBox();
      selectCheckBox.setStyle("-fx-padding: 2;");
      // Preserve selection state when rebuilding the grid
      selectCheckBox.setSelected(selectedStudents.contains(key));
      selectCheckBox.setOnAction(e -> {
        if (selectCheckBox.isSelected()) {
          selectedStudents.add(key);
          cell.setStyle("-fx-border-color: #ff9800; -fx-border-width: 3; -fx-background-color: #ffe0b2;");
        } else {
          selectedStudents.remove(key);
          boolean allNeighborsDeclared = classroom.hasAllNeighborsDeclared(located);
          if (!allNeighborsDeclared) {
            cell.setStyle("-fx-border-color: #ff6600; -fx-border-width: 3; -fx-background-color: #fff0e6;");
          } else {
            cell.setStyle("-fx-border-color: #0066cc; -fx-border-width: 2; -fx-background-color: #e6f2ff;");
          }
        }
      });

      Label nameLabel = new Label(name);
      nameLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold;");
      nameLabel.setWrapText(true);
      nameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

      Label posLabel = new Label("(" + row + "," + col + ")");
      posLabel.setStyle("-fx-font-size: 9; -fx-text-fill: #666666;");

      Label claimsLabel = new Label(claimCount + " claims");
      claimsLabel.setStyle("-fx-font-size: 8; -fx-text-fill: #0066cc;");

      // Check if all neighbors are declared
      boolean allNeighborsDeclared = classroom.hasAllNeighborsDeclared(located);
      if (selectedStudents.contains(key)) {
        cell.setStyle("-fx-border-color: #ff9800; -fx-border-width: 3; -fx-background-color: #ffe0b2;");
        cell.getChildren().addAll(selectCheckBox, nameLabel, posLabel, claimsLabel);
      } else if (!allNeighborsDeclared) {
        cell.setStyle("-fx-border-color: #ff6600; -fx-border-width: 3; -fx-background-color: #fff0e6;");
        Label warningLabel = new Label(LanguageManager.getInstance().get("incomplete"));
        warningLabel.setStyle("-fx-font-size: 7; -fx-text-fill: #ff6600; -fx-font-weight: bold;");
        cell.getChildren().addAll(selectCheckBox, nameLabel, posLabel, claimsLabel, warningLabel);
      } else {
        cell.setStyle("-fx-border-color: #0066cc; -fx-border-width: 2; -fx-background-color: #e6f2ff;");
        cell.getChildren().addAll(selectCheckBox, nameLabel, posLabel, claimsLabel);
      }

      // Click to show details
      final LocatedStudent finalLocated = located;
      nameLabel.setOnMouseClicked(e -> {
        System.out.println("[DEBUG] Student clicked: " + finalLocated.getStudent().getName());
        if (studentDetailController != null) {
          System.out.println("[DEBUG] Displaying student details for: " + finalLocated.getStudent().getName());
          studentDetailController.displayStudent(finalLocated);
          // Switch to Student Details tab
          if (detailsTab != null) {
            detailsTab.getTabPane().getSelectionModel().select(detailsTab);
          }
        } else {
          System.out.println("[DEBUG] studentDetailController is null!");
        }
      });
      nameLabel.setCursor(javafx.scene.Cursor.HAND);
    } else {
      Label emptyLabel = new Label(LanguageManager.getInstance().get("empty"));
      emptyLabel.setStyle("-fx-text-fill: #999999;");
      cell.getChildren().add(emptyLabel);
    }

    return cell;
  }

  private VBox createInfoPanel() {
    VBox panel = new VBox(10);
    panel.setPadding(new Insets(10));
    panel.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1;");

    // Students section
    studentsLabel = new Label(LanguageManager.getInstance().get("students"));
    studentsLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold;");

    studentsListView = new ListView<>();
    studentsListView.setPrefHeight(150);
    studentsListView.setStyle("-fx-control-inner-background: #ffffff;");

    // Add student button
    HBox addStudentBox = new HBox(5);
    studentNameField = new TextField();
    studentNameField.setPromptText(LanguageManager.getInstance().get("student_name"));
    addStudentButton = new Button(LanguageManager.getInstance().get("add_student"));
    addStudentButton.setOnAction(e -> {
      String name = studentNameField.getText().trim();
      if (!name.isEmpty()) {
        addStudent(name, studentsListView);
        studentNameField.clear();
      }
    });
    addStudentBox.getChildren().addAll(studentNameField, addStudentButton);

    // Claims section
    claimsHeaderLabel = new Label(LanguageManager.getInstance().get("claims"));
    claimsHeaderLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold;");

    claimsListView = new ListView<>();
    claimsListView.setPrefHeight(200);
    claimsListView.setStyle("-fx-control-inner-background: #ffffff;");

    // Add claim button
    HBox addClaimBox = new HBox(5);
    claimerCombo = new ComboBox<>();
    targetCombo = new ComboBox<>();
    directionCombo = new ComboBox<>();
    directionCombo.getItems().addAll(Direction.values());
    claimerCombo.setPromptText(LanguageManager.getInstance().get("claimer"));
    targetCombo.setPromptText(LanguageManager.getInstance().get("target"));
    directionCombo.setPromptText(LanguageManager.getInstance().get("direction"));

    addClaimButton = new Button(LanguageManager.getInstance().get("add_claim"));
    addClaimButton.setOnAction(e -> {
      if (claimerCombo.getValue() != null && targetCombo.getValue() != null && directionCombo.getValue() != null) {
        addClaim(claimerCombo.getValue(), targetCombo.getValue(), directionCombo.getValue());
        updateClaimsList();
      }
    });

    addClaimBox.getChildren().addAll(claimerCombo, targetCombo, directionCombo, addClaimButton);
    addClaimBox.setStyle("-fx-font-size: 10;");

    panel.getChildren().addAll(
        studentsLabel,
        studentsListView,
        addStudentBox,
        claimsHeaderLabel,
        claimsListView,
        addClaimBox);

    VBox.setVgrow(studentsListView, Priority.ALWAYS);
    VBox.setVgrow(claimsListView, Priority.ALWAYS);

    return panel;
  }

  private void updateLanguageTexts() {
    LanguageManager lm = LanguageManager.getInstance();
    if (setupLabel != null)
      setupLabel.setText(lm.get("setup_classroom"));
    if (rowLabel != null)
      rowLabel.setText(lm.get("rows"));
    if (colLabel != null)
      colLabel.setText(lm.get("cols"));
    if (createButton != null)
      createButton.setText(lm.get("create"));
    if (analyzeButton != null)
      analyzeButton.setText(lm.get("analyze"));
    if (testDataButton != null)
      testDataButton.setText(lm.get("test_data"));
    if (validateButton != null)
      validateButton.setText(lm.get("validate"));
    if (importCsvButton != null)
      importCsvButton.setText(lm.get("import_csv"));
    if (selectAllButton != null)
      selectAllButton.setText(lm.get("select_all"));
    if (deselectAllButton != null)
      deselectAllButton.setText(lm.get("deselect_all"));
    if (batchPresentButton != null)
      batchPresentButton.setText(lm.get("mark_present"));
    if (batchAbsentButton != null)
      batchAbsentButton.setText(lm.get("mark_absent"));
    if (classroomTitleLabel != null)
      classroomTitleLabel.setText(lm.get("classroom_layout"));
    if (studentsLabel != null)
      studentsLabel.setText(lm.get("students"));
    if (studentNameField != null)
      studentNameField.setPromptText(lm.get("student_name"));
    if (addStudentButton != null)
      addStudentButton.setText(lm.get("add_student"));
    if (claimsHeaderLabel != null)
      claimsHeaderLabel.setText(lm.get("claims"));
    if (claimerCombo != null)
      claimerCombo.setPromptText(lm.get("claimer"));
    if (targetCombo != null)
      targetCombo.setPromptText(lm.get("target"));
    if (directionCombo != null)
      directionCombo.setPromptText(lm.get("direction"));
    if (addClaimButton != null)
      addClaimButton.setText(lm.get("add_claim"));
    if (statusLabel != null && (statusLabel.getText() == null || statusLabel.getText().isEmpty()
        || statusLabel.getText().equals(LanguageManager.getInstance().get("status_ready")))) {
      statusLabel.setText(lm.get("status_ready"));
    }
  }

  private void createClassroom() {
    int rows = rowsCombo.getValue();
    int cols = colsCombo.getValue();

    try {
      classroom = new Classroom(rows, cols);
      studentButtons.clear();
      studentRegistry.clear();

      // Update the grid visualization
      updateClassroomGrid();
      updateClassroomVisualization();
      statusLabel.setText("Classroom created: " + rows + "x" + cols);
    } catch (Exception e) {
      showError("Error creating classroom: " + e.getMessage());
    }
  }

  private void updateClassroomGrid() {
    if (classroom == null)
      return;

    // This is handled by the main view setup
    statusLabel.setText("Classroom grid updated");
  }

  private void addStudent(String name, ListView<String> studentsList) {
    if (classroom == null) {
      showError("Please create a classroom first");
      return;
    }

    String studentId = String.valueOf(studentRegistry.size() + 1);
    Student student = new Student(studentId, name);
    LocatedStudent located = new LocatedStudent(student);

    studentRegistry.put(name, located);
    studentsList.getItems().add(name + " (ID: " + studentId + ")");

    // Update ComboBoxes with new student
    if (claimerCombo != null) {
      claimerCombo.getItems().add(name);
    }
    if (targetCombo != null) {
      targetCombo.getItems().add(name);
    }

    statusLabel.setText("Student added: " + name);
  }

  private void addClaim(String claimer, String target, Direction direction) {
    LocatedStudent claimerLocated = studentRegistry.get(claimer);
    LocatedStudent targetLocated = studentRegistry.get(target);

    if (claimerLocated != null && targetLocated != null) {
      Claim claim = new Claim(direction, targetLocated.getStudent());
      claimerLocated.addClaim(claim);
      claims.add(claim); // Store in claims list for analysis
      statusLabel.setText(claimer + " claims " + target + " to the " + direction.toString().toLowerCase());

      // Reset ComboBoxes after adding claim
      claimerCombo.setValue(null);
      targetCombo.setValue(null);
      directionCombo.setValue(null);
    } else {
      showError("Invalid student selection");
    }
  }

  private void updateClaimsList() {
    claimsListView.getItems().clear();
    for (LocatedStudent student : studentRegistry.values()) {
      for (Claim claim : student.getClaims()) {
        String targetName = claim.isAbsentClaim() ? "[ABSENT/EMPTY]" : claim.getTarget().getName();
        claimsListView.getItems().add(
            student.getStudent().getName() + " → " +
                targetName + " (" + claim.getDirection() + ")");
      }
    }
  }

  public void analyzeAttendance() {
    if (classroom == null || studentRegistry.isEmpty()) {
      showError("Please setup classroom and add students first");
      return;
    }

    try {
      // Validate that all students have declared all their neighbors
      classroom.validateAllNeighborsDeclared();

      // Configure the service
      attendanceService.setClassroom(classroom);
      attendanceService.setLocatedStudents(studentRegistry.values());
      attendanceService.setClaims(claims);
      attendanceService.setManualOverrides(manualOverrides);

      // Perform analysis
      AttendanceService.AttendanceAnalysisResult result = attendanceService.analyzeAttendance();

      // Report success and trigger callback
      statusLabel.setText("Analysis complete: " + result.reports.size() + " students analyzed");
      if (onAnalysisComplete != null) {
        onAnalysisComplete.run();
      }

      // Show results summary
      int presentCount = (int) result.reports.stream()
          .filter(r -> r.getStatus().equals(com.whoami.domain.attendance.AttendanceReport.AttendanceStatus.PRESENT))
          .count();
      int absentCount = (int) result.reports.stream()
          .filter(r -> r.getStatus().equals(com.whoami.domain.attendance.AttendanceReport.AttendanceStatus.ABSENT))
          .count();

      // Record to history
      if (historyController != null) {
        String summary = String.format("Analyzed %d students (%d present, %d absent, %d conflicts)",
            result.reports.size(), presentCount, absentCount, result.conflicts.size());
        historyController.recordAnalysis(summary);
      }

      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("Analysis Results");
      alert.setHeaderText(null);
      alert.setContentText(String.format(
          "Analysis Results:\n\n" +
              "Total Students: %d\n" +
              "Present: %d\n" +
              "Absent: %d\n" +
              "Conflicts Detected: %d\n\n" +
              "See Results tab for details",
          result.reports.size(), presentCount, absentCount, result.conflicts.size()));
      alert.showAndWait();

    } catch (Exception e) {
      showError("Error during analysis: " + e.getMessage());
      statusLabel.setText("Analysis failed");
    }
  }

  private void showError(String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Error");
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  /**
   * Validate that all students have declared all their neighbors.
   */
  private void validateNeighbors() {
    if (classroom == null || studentRegistry.isEmpty()) {
      showError("Please setup classroom and add students first");
      return;
    }

    StringBuilder report = new StringBuilder();
    int invalidCount = 0;

    for (LocatedStudent student : studentRegistry.values()) {
      if (student.getPosition() == null) {
        continue; // Skip students without positions
      }

      if (!classroom.hasAllNeighborsDeclared(student)) {
        invalidCount++;
        report.append("\n• ").append(student.getStudent().getName()).append(" at ")
            .append(student.getPosition()).append(":");

        // Find which neighbors are missing
        var actualNeighbors = classroom.getNeighborsOf(student.getPosition());
        for (var entry : actualNeighbors.entrySet()) {
          Direction dir = entry.getKey();
          LocatedStudent neighbor = entry.getValue();

          boolean isDeclared = student.getClaims().stream()
              .anyMatch(claim -> claim.getDirection() == dir &&
                  !claim.isAbsentClaim() &&
                  claim.getTarget().equals(neighbor.getStudent()));

          if (!isDeclared) {
            report.append("\n  - Missing: ").append(neighbor.getStudent().getName())
                .append(" to the ").append(dir.toString().toLowerCase());
          }
        }
      }
    }

    // Update visualization to show warnings
    updateClassroomVisualization();

    // Show results
    Alert alert;
    if (invalidCount == 0) {
      alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("Validation Success");
      alert.setHeaderText("All students have declared all their neighbors! ✓");
      alert.setContentText("All " + studentRegistry.size() + " students have complete neighbor declarations.");
      statusLabel.setText("✓ All neighbor declarations complete");
    } else {
      alert = new Alert(Alert.AlertType.WARNING);
      alert.setTitle("Validation Failed");
      alert.setHeaderText(invalidCount + " student(s) have incomplete neighbor declarations:");
      alert.setContentText(report.toString());
      statusLabel.setText("⚠ " + invalidCount + " students missing neighbor declarations");
    }
    alert.showAndWait();
  }

  /**
   * Get the attendance service for accessing analysis results.
   */
  public AttendanceService getAttendanceService() {
    return attendanceService;
  }

  /**
   * Get the classroom for accessing structure data.
   */
  public Classroom getClassroom() {
    return classroom;
  }

  public void newSession() {
    classroom = null;
    studentButtons.clear();
    studentRegistry.clear();
    claims.clear();
    claimsListView.getItems().clear();
    statusLabel.setText("New session started");
  }

  /**
   * Load a classroom from a saved session.
   */
  public void loadClassroom(Classroom loadedClassroom) {
    this.classroom = loadedClassroom;

    // Recreate UI elements
    studentButtons.clear();
    claimsListView.getItems().clear();

    // Refresh grid visualization
    updateClassroomVisualization();

    statusLabel.setText("Session loaded with " + classroom.allPlacements().size() + " students");
  }

  private void selectAllStudents() {
    selectedStudents.clear();
    for (LocatedStudent student : classroom.allPlacements().values()) {
      selectedStudents.add(student.getStudent().getName());
    }
    // Selecting all does not change manual overrides
    updateClassroomVisualization();
    statusLabel.setText("Selected " + selectedStudents.size() + " students");
    AuditLogger.log("BATCH_SELECT", "Selected all " + selectedStudents.size() + " students");
  }

  private void deselectAllStudents() {
    selectedStudents.clear();
    updateClassroomVisualization();
    statusLabel.setText("Deselected all students");
    AuditLogger.log("BATCH_DESELECT", "Deselected all students");
  }

  private void batchMarkPresent() {
    if (selectedStudents.isEmpty()) {
      showAlert("No Selection", "Please select students first");
      return;
    }

    for (String key : selectedStudents) {
      LocatedStudent student = studentRegistry.get(key);
      if (student != null) {
        undoRedoManager.executeCommand(new UndoRedoManager.Command() {
          private Claim claim;
          private AttendanceReport.AttendanceStatus previousOverride;

          public void execute() {
            claim = new Claim(Direction.FRONT, null);
            student.addClaim(claim);
            claims.add(claim);
            previousOverride = manualOverrides.put(key, AttendanceReport.AttendanceStatus.PRESENT);
          }

          public void undo() {
            student.getClaims().remove(claim);
            claims.remove(claim);
            if (previousOverride != null) {
              manualOverrides.put(key, previousOverride);
            } else {
              manualOverrides.remove(key);
            }
          }

          public void redo() {
            student.addClaim(claim);
            if (!claims.contains(claim))
              claims.add(claim);
            manualOverrides.put(key, AttendanceReport.AttendanceStatus.PRESENT);
          }
        });
      }
    }
    updateClaimsList();
    updateClassroomVisualization();
    statusLabel.setText("Marked " + selectedStudents.size() + " students as present");
    AuditLogger.log("BATCH_MARK_PRESENT", "Marked " + selectedStudents.size() + " students present");
  }

  private void batchMarkAbsent() {
    if (selectedStudents.isEmpty()) {
      showAlert("No Selection", "Please select students first");
      return;
    }

    for (String key : selectedStudents) {
      LocatedStudent student = studentRegistry.get(key);
      if (student != null) {
        undoRedoManager.executeCommand(new UndoRedoManager.Command() {
          private Claim claim;
          private AttendanceReport.AttendanceStatus previousOverride;

          public void execute() {
            claim = new Claim(Direction.BACK, null);
            student.addClaim(claim);
            claims.add(claim);
            previousOverride = manualOverrides.put(key, AttendanceReport.AttendanceStatus.ABSENT);
          }

          public void undo() {
            student.getClaims().remove(claim);
            claims.remove(claim);
            if (previousOverride != null) {
              manualOverrides.put(key, previousOverride);
            } else {
              manualOverrides.remove(key);
            }
          }

          public void redo() {
            student.addClaim(claim);
            if (!claims.contains(claim))
              claims.add(claim);
            manualOverrides.put(key, AttendanceReport.AttendanceStatus.ABSENT);
          }
        });
      }
    }
    updateClaimsList();
    updateClassroomVisualization();
    statusLabel.setText("Marked " + selectedStudents.size() + " students as absent");
    AuditLogger.log("BATCH_MARK_ABSENT", "Marked " + selectedStudents.size() + " students absent");
  }

  public void undo() {
    if (undoRedoManager.canUndo()) {
      System.out.println("[DEBUG] Undo executed");
      undoRedoManager.undo();
      updateClaimsList();
      updateClassroomVisualization();
      statusLabel.setText("Action undone");
      AuditLogger.log("UNDO", "Undo performed");
    } else {
      System.out.println("[DEBUG] Cannot undo - stack empty");
    }
  }

  public void redo() {
    if (undoRedoManager.canRedo()) {
      System.out.println("[DEBUG] Redo executed");
      undoRedoManager.redo();
      updateClaimsList();
      updateClassroomVisualization();
      statusLabel.setText("Action redone");
      AuditLogger.log("REDO", "Redo performed");
    } else {
      System.out.println("[DEBUG] Cannot redo - stack empty");
    }
  }

  public boolean canUndo() {
    return undoRedoManager.canUndo();
  }

  public boolean canRedo() {
    return undoRedoManager.canRedo();
  }

  public void setRedactionEnabled(boolean enabled) {
    this.redactionEnabled = enabled;
    updateClassroomVisualization();
  }

  public boolean isRedactionEnabled() {
    return redactionEnabled;
  }
}