package org.example.testapp;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import org.example.testapp.attendance.AttendanceReport;
import org.example.testapp.entities.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.example.testapp.services.FirestoreService;

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
  private Button setupFirestoreButton; // NEW: Button to setup/initialize Firestore
  private Button importFirestoreButton; // Button to import from Firestore
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
  private boolean firestoreSetupComplete = false; // Track if setup is done

  public ClassroomController() {
    this.studentButtons = new HashMap<>();
    this.studentRegistry = new HashMap<>();
    this.claims = new ArrayList<>();
    this.attendanceService = new AttendanceService();
    this.selectedStudents = new HashSet<>();
    this.manualOverrides = new HashMap<>();
    this.undoRedoManager = new UndoRedoManager(100);
    this.redactionEnabled = false;
    this.firestoreSetupComplete = false;
  }

  /**
   * Setup/Initialize Firestore for this classroom session
   * This should be called by the teacher to allow students to register
   */
  private void setupFirestoreForClass() {
    try {
      // Clear any existing classroom data in Firestore (optional safety measure)
      clearExistingFirestoreData();

      // Initialize Firestore service
      if (!FirestoreService.isInitialized()) {
        FirestoreService.initialize();
        System.out.println("Firestore service initialized.");
      } else {
        System.out.println("Firestore service already initialized.");
      }

      // Create a setup document to mark this session
      Firestore firestore = FirestoreService.getFirestore();

      // Create a classroom setup document with timestamp
      Map<String, Object> setupData = new HashMap<>();
      setupData.put("teacherId", "teacher_" + System.currentTimeMillis()); // You might want to use actual teacher ID
      setupData.put("setupTime", new Date());
      setupData.put("status", "active");
      setupData.put("classroomSize", rowsCombo.getValue() + "x" + colsCombo.getValue());

      // Create or update the setup document
      firestore.collection("classroom_setup").document("current_session")
          .set(setupData);

      // Clear the students collection to start fresh
      clearStudentsCollection();

      firestoreSetupComplete = true;
      statusLabel.setText("✓ Classroom setup complete! Students can now register.");

      // Show confirmation
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("Classroom Setup Complete");
      alert.setHeaderText("Firestore is ready for student registration");
      alert.setContentText("Students can now register their positions and claims.\n" +
          "When ready, click 'Import Data' to import all registered students.\n\n" +
          "Classroom size: " + rowsCombo.getValue() + " rows × " + colsCombo.getValue() + " columns");
      alert.showAndWait();

      // Enable the import button now that setup is complete
      importFirestoreButton.setDisable(false);
      importFirestoreButton.setStyle("-fx-font-size: 11; -fx-padding: 8; -fx-text-fill: #FF6B35;");

    } catch (Exception e) {
      showAlert("Firestore Setup Failed", "Error setting up Firestore: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Clear existing students from Firestore (optional safety measure)
   */
  private void clearExistingFirestoreData() {
    try {
      if (FirestoreService.isInitialized()) {
        Firestore firestore = FirestoreService.getFirestore();

        // Delete all documents in students collection
        ApiFuture<QuerySnapshot> future = firestore.collection("students").get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        if (!documents.isEmpty()) {
          WriteBatch batch = firestore.batch();
          for (QueryDocumentSnapshot doc : documents) {
            batch.delete(doc.getReference());
          }
          batch.commit().get();
          System.out.println("Cleared " + documents.size() + " existing student records.");
        }
      }
    } catch (Exception e) {
      System.err.println("Warning: Could not clear existing Firestore data: " + e.getMessage());
      // Continue anyway - this is just a safety measure
    }
  }

  /**
   * Clear the students collection (empty it)
   */
  private void clearStudentsCollection() throws Exception {
    Firestore firestore = FirestoreService.getFirestore();

    // Get all documents in students collection
    ApiFuture<QuerySnapshot> future = firestore.collection("students").get();
    List<QueryDocumentSnapshot> documents = future.get().getDocuments();

    if (!documents.isEmpty()) {
      WriteBatch batch = firestore.batch();
      for (QueryDocumentSnapshot doc : documents) {
        batch.delete(doc.getReference());
      }
      batch.commit().get();
      System.out.println("Cleared " + documents.size() + " documents from students collection.");
    } else {
      System.out.println("Students collection is already empty.");
    }
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
        { "Alice", "A" },
        { "Bob", "B" },
        { "Charlie", "C" },
        { "Diana", "D" },
        { "Eve", "E" },
        { "Frank", "F" }, // Absent student - no position
        { "Grace", "G" } // Uncertain student - conflicting claims
    };

    int[][] positions = {
        { 0, 0 }, // Alice at row 0, col 0
        { 0, 1 }, // Bob at row 0, col 1
        { 0, 2 }, // Charlie at row 0, col 2
        { 1, 0 }, // Diana at row 1, col 0
        { 1, 1 }, // Eve at row 1, col 1
        { -1, -1 }, // Frank - no position (absent)
        { 1, 2 } // Grace at row 1, col 2 - will have weak/conflicting claims
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
      // Frank has no position - will be marked ABSENT

      studentRegistry.put(id, located);

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
    // Alice (0,0): COMPLETE - has Bob RIGHT and Diana BACK ✓
    addTestClaim("Alice", "Bob", Direction.RIGHT);
    addTestClaim("Alice", "Diana", Direction.BACK);

    // Bob (0,1): COMPLETE - has Alice LEFT, Charlie RIGHT, and Eve BACK ✓
    addTestClaim("Bob", "Alice", Direction.LEFT);
    addTestClaim("Bob", "Charlie", Direction.RIGHT);
    addTestClaim("Bob", "Eve", Direction.BACK);

    // Charlie (0,2): has Bob LEFT and Grace BACK ✓
    addTestClaim("Charlie", "Bob", Direction.LEFT);
    addTestClaim("Charlie", "Grace", Direction.BACK); // Grace is at (1,2) behind Charlie

    // Diana (1,0): COMPLETE - has Eve RIGHT and Alice FRONT ✓
    addTestClaim("Diana", "Eve", Direction.RIGHT);
    addTestClaim("Diana", "Alice", Direction.FRONT);

    // Eve (1,1): has Diana LEFT, Bob FRONT, declares RIGHT is Grace
    addTestClaim("Eve", "Diana", Direction.LEFT);
    addTestClaim("Eve", "Bob", Direction.FRONT);
    addTestClaim("Eve", "Grace", Direction.RIGHT); // Grace is to the right
    addAbsentClaim("Eve", Direction.BACK); // Position (2,1) is empty

    // Grace (1,2): LIAR - claims absent student Frank is her neighbor
    // This creates UNCERTAIN status through conflict detection
    addTestClaim("Grace", "Charlie", Direction.FRONT); // Correct: Charlie is at (0,2)
    addTestClaim("Grace", "Eve", Direction.LEFT); // Correct: Eve is at (1,1)
    addTestClaim("Grace", "Frank", Direction.BACK); // LYING: Frank is absent, not at (2,2)!
    // Grace's uncertainty will come from claiming an absent student

    // Frank makes no claims and has no position

    updateClaimsList();
    updateClassroomVisualization();
    statusLabel.setText(
        "✓ Test data loaded: 7 students - Alice/Bob/Charlie/Diana/Eve: PRESENT, Grace: UNCERTAIN (liar claiming absent Frank), Frank: ABSENT");
  }

  /**
   * Import classroom data directly from Firestore
   */
  /**
   * Import classroom data directly from Firestore and close registration.
   */
  private void importFromFirestore() {
    if (!firestoreSetupComplete) {
      showAlert("Setup Required",
          "Please click 'Setup Firestore' first to initialize the classroom.");
      return;
    }

    try {
      // --- NEW LOGIC: CLOSE REGISTRATION IMMEDIATELY ---
      // This prevents students from submitting while the import is running.
      if (FirestoreService.isInitialized()) {
        Firestore firestore = FirestoreService.getFirestore();
        Map<String, Object> closeData = new HashMap<>();
        closeData.put("status", "closed");
        closeData.put("closeTime", new Date());

        // Update the setup document to 'closed'
        firestore.collection("classroom_setup").document("current_session")
            .set(closeData, com.google.cloud.firestore.SetOptions.merge());

        System.out.println("Registration gate CLOSED in Firestore.");
      }
      // --------------------------------------------------

      // Clear existing local data
      studentRegistry.clear();
      claims.clear();
      selectedStudents.clear();
      manualOverrides.clear();
      undoRedoManager.clear();

      if (claimsListView != null)
        claimsListView.getItems().clear();
      if (studentsListView != null)
        studentsListView.getItems().clear();
      if (claimerCombo != null)
        claimerCombo.getItems().clear();
      if (targetCombo != null)
        targetCombo.getItems().clear();

      Firestore firestore = FirestoreService.getFirestore();
      CollectionReference studentsRef = firestore.collection("students");
      ApiFuture<QuerySnapshot> querySnapshot = studentsRef.get();

      List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

      if (documents.isEmpty()) {
        showAlert("No Data", "No students have registered yet in Firestore.");
        // Re-enable setup if teacher wants to try again later
        return;
      }

      int maxRow = -1;
      int maxCol = -1;
      int studentsImported = 0;
      Set<String> seenIds = new HashSet<>();
      List<LocatedStudent> importedStudents = new ArrayList<>();

      for (QueryDocumentSnapshot doc : documents) {
        try {
          String studentId = doc.getString("studentId");
          String name = doc.getString("name");

          if (studentId == null || studentId.isEmpty() || seenIds.contains(studentId)) {
            continue;
          }
          seenIds.add(studentId);

          Student student = new Student(studentId, name);
          LocatedStudent located = new LocatedStudent(student);

          // Extract position
          Long rowLong = doc.getLong("row");
          Long colLong = doc.getLong("col");
          Integer row = rowLong != null ? rowLong.intValue() : null;
          Integer col = colLong != null ? colLong.intValue() : null;

          if (row != null && col != null && row >= 0 && col >= 0) {
            located.setPosition(new Position(row, col));
            maxRow = Math.max(maxRow, row);
            maxCol = Math.max(maxCol, col);
          }

          // Extract Claims (neighbor data)
          List<Map<String, Object>> claimsList = (List<Map<String, Object>>) doc.get("claims");
          if (claimsList == null)
            claimsList = (List<Map<String, Object>>) doc.get("neighborClaims");

          if (claimsList != null) {
            for (Map<String, Object> claimMap : claimsList) {
              String dirStr = (String) claimMap.get("direction");
              if (dirStr == null)
                continue;
              Direction direction = Direction.valueOf(dirStr.toUpperCase());

              Object targetObj = claimMap.get("student");
              if (targetObj == null) {
                Claim claim = new Claim(direction, null);
                located.addClaim(claim);
                claims.add(claim);
              } else {
                // Simplified extraction for the claim target
                String targetId = (targetObj instanceof Map) ? ((Map<String, String>) targetObj).get("studentId")
                    : targetObj.toString();
                String targetName = (targetObj instanceof Map) ? ((Map<String, String>) targetObj).get("name")
                    : targetId;

                Student targetStudent = new Student(targetId, targetName);
                Claim claim = new Claim(direction, targetStudent);
                located.addClaim(claim);
                claims.add(claim);
              }
            }
          }

          importedStudents.add(located);
          studentRegistry.put(studentId, located);
          studentsImported++;

          if (studentsListView != null)
            studentsListView.getItems().add(name + " (ID: " + studentId + ")");
          if (claimerCombo != null)
            claimerCombo.getItems().add(name);
          if (targetCombo != null)
            targetCombo.getItems().add(name);

        } catch (Exception e) {
          System.err.println("Error processing doc: " + e.getMessage());
        }
      }

      // Classroom layout setup
      int rowsCount = maxRow >= 0 ? maxRow + 1 : Math.max(3, rowsCombo.getValue());
      int colsCount = maxCol >= 0 ? maxCol + 1 : Math.max(3, colsCombo.getValue());
      classroom = new Classroom(rowsCount, colsCount);
      rowsCombo.setValue(rowsCount);
      colsCombo.setValue(colsCount);
      attendanceService.setClassroom(classroom);

      for (LocatedStudent ls : importedStudents) {
        if (ls.getPosition() != null)
          classroom.place(ls);
      }

      updateClassroomVisualization();
      updateClaimsList();

      statusLabel.setText("✓ Imported " + studentsImported + " students. Registration CLOSED.");

      // Post-import cleanup
      Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
      confirmAlert.setTitle("Import Complete");
      confirmAlert.setHeaderText("Registration is now closed.");
      confirmAlert.setContentText("Do you want to clear student records from the cloud database now?");

      Optional<ButtonType> result = confirmAlert.showAndWait();
      if (result.isPresent() && result.get() == ButtonType.OK) {
        clearStudentsCollection();
        // Delete session doc so students see it's completely over
        firestore.collection("classroom_setup").document("current_session").delete();

        firestoreSetupComplete = false;
        importFirestoreButton.setDisable(true);
        importFirestoreButton.setStyle("-fx-font-size: 11; -fx-padding: 8; -fx-text-fill: #aaaaaa;");
      }

    } catch (Exception e) {
      showAlert("Firestore Import Failed", "Error: " + e.getMessage());
      e.printStackTrace();
    }
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
    // Find students by name in test data
    LocatedStudent claimerLocated = studentRegistry.values().stream()
        .filter(ls -> ls.getStudent().getName().equals(claimer))
        .findFirst().orElse(null);

    LocatedStudent targetLocated = studentRegistry.values().stream()
        .filter(ls -> ls.getStudent().getName().equals(target))
        .findFirst().orElse(null);

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
    // Find student by name in test data
    LocatedStudent claimerLocated = studentRegistry.values().stream()
        .filter(ls -> ls.getStudent().getName().equals(claimer))
        .findFirst().orElse(null);

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
    VBox mainLayout = new VBox(15);
    mainLayout.setPadding(new Insets(20));
    mainLayout.setStyle("-fx-background-color: -fx-background;");

    // Control panel
    HBox controlPanel = createControlPanel();
    mainLayout.getChildren().add(controlPanel);

    // Classroom grid and claims
    HBox contentArea = new HBox(20);
    contentArea.setPrefHeight(700);

    // Classroom grid
    VBox classroomPanel = createClassroomPanel();
    classroomPanel.setPrefWidth(550);

    // Claims and info panel
    VBox infoPanel = createInfoPanel();
    infoPanel.setPrefWidth(450);

    contentArea.getChildren().addAll(classroomPanel, infoPanel);
    HBox.setHgrow(classroomPanel, Priority.ALWAYS);
    HBox.setHgrow(infoPanel, Priority.ALWAYS);

    mainLayout.getChildren().add(contentArea);

    // Status bar
    statusLabel = new Label(LanguageManager.getInstance().get("status_ready"));
    statusLabel.getStyleClass().add("status-bar");
    statusLabel.setStyle("-fx-padding: 12px;");
    mainLayout.getChildren().add(statusLabel);

    ScrollPane sp = new ScrollPane(mainLayout);
    // Listen for language changes to update labels/buttons live
    LanguageManager.getInstance().addLanguageChangeListener(lang -> updateLanguageTexts());
    // Apply current language immediately
    updateLanguageTexts();
    return sp;
  }

  private HBox createControlPanel() {
    HBox panel = new HBox(12);
    panel.setPadding(new Insets(15));
    panel.getStyleClass().add("card");
    panel.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    
    rowLabel = new Label(LanguageManager.getInstance().get("rows"));
    rowLabel.getStyleClass().add("label-header");
    
    rowsCombo = new ComboBox<>();
    rowsCombo.getItems().addAll(2, 3, 4, 5);
    rowsCombo.setValue(3);
    rowsCombo.setPrefWidth(70);
    
    colLabel = new Label(LanguageManager.getInstance().get("cols"));
    colLabel.getStyleClass().add("label-header");
    
    colsCombo = new ComboBox<>();
    colsCombo.getItems().addAll(2, 3, 4, 5);
    colsCombo.setValue(4);
    colsCombo.setPrefWidth(70);
    
    createButton = new Button(LanguageManager.getInstance().get("create"));
    createButton.setPrefWidth(120);
    createButton.setOnAction(e -> createClassroom());
    
    analyzeButton = new Button(LanguageManager.getInstance().get("analyze"));
    analyzeButton.setPrefWidth(120);
    analyzeButton.getStyleClass().add("button-warning");
    analyzeButton.setOnAction(e -> analyzeAttendance());
    
    testDataButton = new Button(LanguageManager.getInstance().get("test_data"));
    testDataButton.setPrefWidth(120);
    testDataButton.getStyleClass().add("button-outline");
    testDataButton.setOnAction(e -> loadTestData());
    
    validateButton = new Button(LanguageManager.getInstance().get("validate"));
    validateButton.setPrefWidth(120);
    validateButton.getStyleClass().add("button-outline");
    validateButton.setOnAction(e -> validateNeighbors());
    
    importCsvButton = new Button(LanguageManager.getInstance().get("import_csv"));
    importCsvButton.setPrefWidth(120);
    importCsvButton.getStyleClass().add("button-success");
    importCsvButton.setOnAction(e -> importFromCsv());

    // NEW: Setup Firestore Button (to initialize for student registration)
    setupFirestoreButton = new Button(LanguageManager.getInstance().get("setup_firestore"));
    setupFirestoreButton.setPrefWidth(120);
    setupFirestoreButton.getStyleClass().add("button-success");
    setupFirestoreButton.setTooltip(new Tooltip("Initialize Firestore for student registration"));
    setupFirestoreButton.setOnAction(e -> setupFirestoreForClass());

    // Import Firestore Button (disabled until setup is done)
    importFirestoreButton = new Button(LanguageManager.getInstance().get("import_firestore"));
    importFirestoreButton.setPrefWidth(120);
    importFirestoreButton.getStyleClass().add("button-success");
    importFirestoreButton.setDisable(true);
    importFirestoreButton.setTooltip(new Tooltip("Import registered students (requires setup first)"));
    importFirestoreButton.setOnAction(e -> importFromFirestore());

    selectAllButton = new Button(LanguageManager.getInstance().get("select_all"));
    selectAllButton.setPrefWidth(100);
    selectAllButton.getStyleClass().add("button-secondary");
    selectAllButton.setOnAction(e -> selectAllStudents());
    
    deselectAllButton = new Button(LanguageManager.getInstance().get("deselect_all"));
    deselectAllButton.setPrefWidth(110);
    deselectAllButton.getStyleClass().add("button-secondary");
    deselectAllButton.setOnAction(e -> deselectAllStudents());
    
    batchPresentButton = new Button(LanguageManager.getInstance().get("mark_present"));
    batchPresentButton.setPrefWidth(120);
    batchPresentButton.getStyleClass().add("button-success");
    batchPresentButton.setOnAction(e -> batchMarkPresent());
    
    batchAbsentButton = new Button(LanguageManager.getInstance().get("mark_absent"));
    batchAbsentButton.setPrefWidth(120);
    batchAbsentButton.getStyleClass().add("button-danger");
    batchAbsentButton.setOnAction(e -> batchMarkAbsent());

    setupLabel = new Label(LanguageManager.getInstance().get("setup_classroom"));
    setupLabel.getStyleClass().add("label-header");
    setupLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 10 0 0;");
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
        setupFirestoreButton,
        importFirestoreButton,
        new Separator(javafx.geometry.Orientation.VERTICAL),
        selectAllButton, deselectAllButton,
        batchPresentButton, batchAbsentButton);

    return panel;
  }

  private VBox createClassroomPanel() {
    VBox panel = new VBox(12);
    panel.setPadding(new Insets(15));
    panel.getStyleClass().add("card");

    classroomTitleLabel = new Label(LanguageManager.getInstance().get("classroom_layout"));
    classroomTitleLabel.getStyleClass().add("label-subtitle");

    ScrollPane gridScroll = new ScrollPane();
    classroomGrid = new GridPane();
    classroomGrid.setHgap(8);
    classroomGrid.setVgap(8);
    classroomGrid.setPadding(new Insets(15));
    classroomGrid.getStyleClass().add("classroom-grid");

    gridScroll.setContent(classroomGrid);
    gridScroll.setFitToWidth(true);
    gridScroll.setFitToHeight(true);
    gridScroll.getStyleClass().add("classroom-scroll");

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
    VBox cell = new VBox(6);
    cell.getStyleClass().add("classroom-cell");
    cell.setPrefWidth(100);
    cell.setPrefHeight(100);
    cell.setAlignment(Pos.CENTER);
    cell.setPadding(new Insets(8));

    Position pos = new Position(row, col);
    Optional<LocatedStudent> student = classroom.getAt(pos);

    if (student.isPresent()) {
      LocatedStudent located = student.get();
      String name = located.getStudent().getName();
      String id = located.getStudent().getId();
      String key = id; // Use ID as key for consistency
      int claimCount = located.getClaims().size();

      CheckBox selectCheckBox = new CheckBox();
      selectCheckBox.setStyle("-fx-font-size: 12px;");
      // Preserve selection state when rebuilding the grid
      selectCheckBox.setSelected(selectedStudents.contains(key));
      selectCheckBox.setOnAction(e -> {
        if (selectCheckBox.isSelected()) {
          selectedStudents.add(key);
          cell.getStyleClass().remove("cell-verified");
          cell.getStyleClass().remove("cell-unverified");
          cell.getStyleClass().add("cell-selected");
        } else {
          selectedStudents.remove(key);
          boolean allNeighborsDeclared = classroom.hasAllNeighborsDeclared(located);
          if (!allNeighborsDeclared) {
            cell.getStyleClass().remove("cell-selected");
            cell.getStyleClass().remove("cell-verified");
            cell.getStyleClass().add("cell-unverified");
          } else {
            cell.getStyleClass().remove("cell-selected");
            cell.getStyleClass().remove("cell-unverified");
            cell.getStyleClass().add("cell-verified");
          }
        }
      });

      Label nameLabel = new Label(name);
      nameLabel.getStyleClass().add("cell-name");
      nameLabel.setWrapText(true);
      nameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

      Label posLabel = new Label("(" + row + "," + col + ")");
      posLabel.getStyleClass().add("cell-position");

      Label claimsLabel = new Label(claimCount + " claims");
      claimsLabel.getStyleClass().add("cell-claims");

      // Check if all neighbors are declared
      boolean allNeighborsDeclared = classroom.hasAllNeighborsDeclared(located);
      if (selectedStudents.contains(key)) {
        cell.getStyleClass().add("cell-selected");
        cell.getChildren().addAll(selectCheckBox, nameLabel, posLabel, claimsLabel);
      } else if (!allNeighborsDeclared) {
        cell.getStyleClass().add("cell-unverified");
        Label warningLabel = new Label(LanguageManager.getInstance().get("incomplete"));
        warningLabel.getStyleClass().add("cell-warning");
        cell.getChildren().addAll(selectCheckBox, nameLabel, posLabel, claimsLabel, warningLabel);
      } else {
        cell.getStyleClass().add("cell-verified");
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
    VBox panel = new VBox(15);
    panel.setPadding(new Insets(15));
    panel.getStyleClass().add("card");

    // Students section
    studentsLabel = new Label(LanguageManager.getInstance().get("students"));
    studentsLabel.getStyleClass().add("label-subtitle");

    studentsListView = new ListView<>();
    studentsListView.setPrefHeight(150);

    // Add student box with better styling
    HBox addStudentBox = new HBox(8);
    addStudentBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    
    studentNameField = new TextField();
    studentNameField.setPromptText(LanguageManager.getInstance().get("student_name"));
    studentNameField.setPrefWidth(200);
    HBox.setHgrow(studentNameField, Priority.ALWAYS);
    
    addStudentButton = new Button(LanguageManager.getInstance().get("add_student"));
    addStudentButton.getStyleClass().add("button-success");
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
    claimsHeaderLabel.getStyleClass().add("label-subtitle");

    claimsListView = new ListView<>();
    claimsListView.setPrefHeight(200);

    // Add claim box with better styling
    VBox addClaimBox = new VBox(8);
    
    HBox combo1 = new HBox(8);
    claimerCombo = new ComboBox<>();
    claimerCombo.setPromptText(LanguageManager.getInstance().get("claimer"));
    claimerCombo.setPrefWidth(150);
    
    targetCombo = new ComboBox<>();
    targetCombo.setPromptText(LanguageManager.getInstance().get("target"));
    targetCombo.setPrefWidth(150);
    
    combo1.getChildren().addAll(claimerCombo, targetCombo);
    
    HBox combo2 = new HBox(8);
    directionCombo = new ComboBox<>();
    directionCombo.getItems().addAll(Direction.values());
    directionCombo.setPromptText(LanguageManager.getInstance().get("direction"));
    directionCombo.setPrefWidth(150);

    addClaimButton = new Button(LanguageManager.getInstance().get("add_claim"));
    addClaimButton.getStyleClass().add("button-success");
    addClaimButton.setPrefWidth(150);
    addClaimButton.setOnAction(e -> {
      if (claimerCombo.getValue() != null && targetCombo.getValue() != null && directionCombo.getValue() != null) {
        addClaim(claimerCombo.getValue(), targetCombo.getValue(), directionCombo.getValue());
        updateClaimsList();
      }
    });

    combo2.getChildren().addAll(directionCombo, addClaimButton);
    addClaimBox.getChildren().addAll(combo1, combo2);

    panel.getChildren().addAll(
        studentsLabel,
        studentsListView,
        addStudentBox,
        new Separator(),
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
    if (setupFirestoreButton != null)
      setupFirestoreButton.setText(lm.get("setup_firestore"));
    if (importFirestoreButton != null)
      importFirestoreButton.setText(lm.get("import_firestore"));
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

    // Use studentId as key for consistency
    studentRegistry.put(studentId, located);
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
    // Find students by name in registry
    LocatedStudent claimerLocated = studentRegistry.values().stream()
        .filter(ls -> ls.getStudent().getName().equals(claimer))
        .findFirst().orElse(null);

    LocatedStudent targetLocated = studentRegistry.values().stream()
        .filter(ls -> ls.getStudent().getName().equals(target))
        .findFirst().orElse(null);

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
      //classroom.validateAllNeighborsDeclared();

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
          .filter(r -> r.getStatus().equals(org.example.testapp.attendance.AttendanceReport.AttendanceStatus.PRESENT))
          .count();
      int absentCount = (int) result.reports.stream()
          .filter(r -> r.getStatus().equals(org.example.testapp.attendance.AttendanceReport.AttendanceStatus.ABSENT))
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
      selectedStudents.add(student.getStudent().getId());
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