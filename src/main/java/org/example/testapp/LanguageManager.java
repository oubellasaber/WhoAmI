package org.example.testapp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Manages language translations for the application.
 * Supports English and French.
 * Persists language preference to disk.
 */
public class LanguageManager {
  private static LanguageManager instance;
  private Language currentLanguage = Language.ENGLISH;
  private final Map<Language, Map<String, String>> translations = new HashMap<>();
  private final List<LanguageChangeListener> listeners = new ArrayList<>();
  private static final String PREFS_DIR = System.getProperty("user.home") + "/.whoami";
  private static final String LANG_PREFS_FILE = PREFS_DIR + "/language.prefs";

  public interface LanguageChangeListener {
    void onLanguageChanged(Language newLanguage);
  }

  public enum Language {
    ENGLISH("English"),
    FRENCH("Français");

    private final String displayName;

    Language(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }

  private LanguageManager() {
    initializeTranslations();
    loadLanguagePreference();
  }

  public static LanguageManager getInstance() {
    if (instance == null) {
      instance = new LanguageManager();
    }
    return instance;
  }

  private void initializeTranslations() {
    // English translations
    Map<String, String> english = new HashMap<>();
    english.put("setup_classroom", "Setup Classroom:");
    english.put("rows", "Rows:");
    english.put("cols", "Columns:");
    english.put("create", "Create");
    english.put("analyze", "Analyze Attendance");
    english.put("test_data", "Load Test Data");
    english.put("validate", "Validate Neighbors");
    english.put("import_csv", "Import CSV");
    english.put("setup_firestore", "Setup");
    english.put("import_firestore", "Import Data");
    english.put("select_all", "Select All");
    english.put("deselect_all", "Deselect All");
    english.put("mark_present", "Mark Selected Present");
    english.put("mark_absent", "Mark Selected Absent");
    english.put("classroom_layout", "Classroom Layout");
    english.put("students", "Students");
    english.put("add_student", "Add Student");
    english.put("student_name", "Student name...");
    english.put("claims", "Claims");
    english.put("claimer", "Claimer:");
    english.put("target", "Target:");
    english.put("direction", "Direction:");
    english.put("add_claim", "Add Claim");
    english.put("claims_list", "Claims List");
    english.put("status", "Status:");
    english.put("student_details", "Student Details");
    english.put("details", "Details");
    english.put("results", "Results");
    english.put("statistics", "Statistics");
    english.put("settings", "Settings");
    english.put("audit_log", "Audit Log");
    english.put("total_students", "Total Students");
    english.put("present", "Present");
    english.put("absent", "Absent");
    english.put("attendance_rate", "Attendance Rate");
    english.put("chart_title", "Attendance Distribution");
    english.put("no_stats", "No statistics yet. Run analysis to populate.");
    english.put("undo", "Undo");
    english.put("redo", "Redo");
    english.put("file", "File");
    english.put("edit", "Edit");
    english.put("view", "View");
    english.put("help", "Help");
    english.put("new_session", "New Session");
    english.put("save_session", "Save Session");
    english.put("load_session", "Load Session");
    english.put("exit", "Exit");
    english.put("dark_mode", "Dark Mode");
    english.put("anonymize_exports", "Anonymize Data in Exports");
    english.put("about", "About");
    english.put("language", "Language");
    english.put("attendance_results", "Attendance Results");
    english.put("conflict_analysis", "Conflict Analysis");
    english.put("analysis_history", "Analysis History");
    english.put("attendance_statistics", "Attendance Statistics");
    english.put("no_selection", "No Selection");
    english.put("select_students", "Please select students first");
    english.put("empty", "Empty");
    english.put("position", "Position");
    english.put("claims_count", "Claims Count");
    english.put("incomplete", "⚠ Incomplete");
    english.put("refresh", "Refresh");
    english.put("clear_log", "Clear Audit Log");
    english.put("confirm", "Confirm");
    english.put("are_you_sure", "Are you sure?");
    english.put("clear_log_confirm", "This will permanently delete all audit log entries.");
    english.put("about_title", "About Smart Attendance System");
    english.put("about_header", "Multi-Strategy Smart Attendance System v1.0");
    english.put("about_content", "A distributed attendance verification system using peer-confirmation networks.");
    english.put("shortcuts", "Keyboard Shortcuts");
    english.put("action_undone", "Action undone");
    english.put("action_redone", "Action redone");
    english.put("selected_count", "Selected %d students");
    english.put("deselected_all", "Deselected all students");
    english.put("marked_present", "Marked %d students as present");
    english.put("marked_absent", "Marked %d students as absent");
    english.put("status_ready", "Ready");

    // Results tab
    english.put("attendance_analysis_results", "Attendance Analysis Results");
    english.put("search_name", "Search Name:");
    english.put("enter_student_name", "Enter student name...");
    english.put("filter_status", "Filter Status:");
    english.put("all", "All");
    english.put("clear_filters", "Clear Filters");
    english.put("refresh_results", "Refresh Results");
    english.put("summary_statistics", "Summary Statistics");
    english.put("no_results_yet", "No results yet");
    english.put("no_analysis_results", "No analysis results available. Run analysis from Classroom tab.");
    english.put("export_json", "Export as JSON");
    english.put("export_csv", "Export as CSV");
    english.put("export_pdf", "Export as PDF");
    english.put("export_excel", "Export as Excel");
    english.put("export_image", "Export as Image");
    english.put("student_name_col", "Student Name");
    english.put("status_col", "Status");
    english.put("confidence_col", "Confidence");
    english.put("neighbor_col", "Neighbor");
    english.put("occupancy_col", "Occupancy");
    english.put("consensus_col", "Consensus");

    // Conflict tab
    english.put("conflict_title", "Conflict Analysis & Liar Detection");
    english.put("analyze_conflicts", "Analyze Conflicts");
    english.put("conflict_prompt", "Click 'Analyze Conflicts' to detect false claims");
    english.put("conflict_details", "Conflict Details:");
    english.put("no_data", "No Data");
    english.put("no_classroom_or_students", "No classroom or students available");
    english.put("no_conflicts", "✓ No conflicts detected. All students are honest!");
    english.put("analysis_complete", "Analysis complete.");
    english.put("analysis_failed", "Analysis Failed");
    english.put("conflict_detected_summary", "⚠ %d conflict(s) detected - Students making false claims");
    english.put("conflict_report_header", "=== CONFLICT ANALYSIS REPORT ===");
    english.put("conflict_total_conflicts", "Total Conflicts Found: %d");
    english.put("conflict_total_students", "Total Students: %d");
    english.put("conflict_rate", "Conflict Rate: %.1f%%");
    english.put("conflict_detailed", "=== DETAILED CONFLICTS ===");
    english.put("conflict_claimer", "Claimer: %s");
    english.put("conflict_type", "Conflict Type: %s");
    english.put("conflict_detail_line", "Details: %s");
    english.put("conflict_separator", "---");
    english.put("conflict_suspicious", "=== SUSPICIOUS STUDENTS (LOW RECIPROCITY) ===");
    english.put("conflict_suspicious_entry", "⚠ %s (ID: %s)");
    english.put("conflict_truthful_summary",
        "All students have provided truthful claims about their neighbors.\nTotal students: %d");
    english.put("conflict_error", "Error analyzing conflicts: %s");
    english.put("audit_log_cleared", "Audit log cleared.");

    // History tab
    english.put("clear_history", "Clear History");
    english.put("past_analyses", "Past Analyses:");
    english.put("details", "Details");
    english.put("clear_history_confirm", "Remove all analysis history records?");
    english.put("error", "Error");
    english.put("failed_clear_history", "Failed to clear history:");

    // Settings tab
    english.put("configuration_settings", "Configuration Settings");
    english.put("strategy_weights", "Strategy Weights");
    english.put("confidence_thresholds", "Confidence Thresholds");
    english.put("apply_settings", "Apply Settings");
    english.put("reset_defaults", "Reset to Defaults");
    english.put("settings_warnings_title", "Settings Validation Warnings");
    english.put("configuration_issues", "Configuration Issues Detected:");
    english.put("continue", "Continue");
    english.put("cancel", "Cancel");
    english.put("name", "Name");
    english.put("id", "ID");
    english.put("row", "Row");
    english.put("col", "Column");
    english.put("absent", "Absent");
    english.put("no_claims_recorded", "No claims recorded");
    english.put("verification_score", "Verification Score");
    english.put("direction_left", "Left");
    english.put("direction_right", "Right");
    english.put("direction_front", "Front");
    english.put("direction_back", "Back");

    translations.put(Language.ENGLISH, english);

    // French translations
    Map<String, String> french = new HashMap<>();
    french.put("setup_classroom", "Configurer la classe:");
    french.put("rows", "Lignes:");
    french.put("cols", "Colonnes:");
    french.put("create", "Créer");
    french.put("analyze", "Analyser l'présence");
    french.put("test_data", "Charger données de test");
    french.put("validate", "Valider voisins");
    french.put("import_csv", "Importer CSV");
    french.put("setup_firestore", "Configurer");
    french.put("import_firestore", "Importer les données");
    french.put("select_all", "Sélectionner tout");
    french.put("deselect_all", "Désélectionner tout");
    french.put("mark_present", "Marquer sélectionnés présents");
    french.put("mark_absent", "Marquer sélectionnés absents");
    french.put("classroom_layout", "Disposition de la classe");
    french.put("students", "Étudiants");
    french.put("add_student", "Ajouter un étudiant");
    french.put("student_name", "Nom de l'étudiant...");
    french.put("claims", "Déclarations");
    french.put("claimer", "Déclarant:");
    french.put("target", "Cible:");
    french.put("direction", "Direction:");
    french.put("add_claim", "Ajouter déclaration");
    french.put("claims_list", "Liste des déclarations");
    french.put("status", "Statut:");
    french.put("student_details", "Détails étudiant");
    french.put("details", "Détails");
    french.put("results", "Résultats");
    french.put("statistics", "Statistiques");
    french.put("settings", "Paramètres");
    french.put("audit_log", "Journal d'audit");
    french.put("total_students", "Nombre total d'étudiants");
    french.put("present", "Présent");
    french.put("absent", "Absent");
    french.put("attendance_rate", "Taux de présence");
    french.put("chart_title", "Distribution de présence");
    french.put("no_stats", "Pas de statistiques. Exécutez l'analyse pour remplir.");
    french.put("undo", "Annuler");
    french.put("redo", "Rétablir");
    french.put("file", "Fichier");
    french.put("edit", "Édition");
    french.put("view", "Affichage");
    french.put("help", "Aide");
    french.put("new_session", "Nouvelle session");
    french.put("save_session", "Enregistrer session");
    french.put("load_session", "Charger session");
    french.put("exit", "Quitter");
    french.put("dark_mode", "Mode sombre");
    french.put("anonymize_exports", "Anonymiser les données dans les exports");
    french.put("about", "À propos");
    french.put("language", "Langue");
    french.put("attendance_results", "Résultats de présence");
    french.put("conflict_analysis", "Analyse des conflits");
    french.put("analysis_history", "Historique d'analyse");
    french.put("attendance_statistics", "Statistiques de présence");
    french.put("no_selection", "Aucune sélection");
    french.put("select_students", "Veuillez d'abord sélectionner des étudiants");
    french.put("empty", "Vide");
    french.put("position", "Position");
    french.put("claims_count", "Nombre de déclarations");
    french.put("incomplete", "⚠ Incomplet");
    french.put("refresh", "Actualiser");
    french.put("clear_log", "Effacer journal d'audit");
    french.put("confirm", "Confirmer");
    french.put("are_you_sure", "Êtes-vous sûr?");
    french.put("clear_log_confirm", "Ceci supprimera définitivement toutes les entrées du journal d'audit.");
    french.put("about_title", "À propos du système d'assistance intelligente");
    french.put("about_header", "Système d'assistance multi-stratégies v1.0");
    french.put("about_content",
        "Un système de vérification de présence distribué utilisant des réseaux de confirmation par les pairs.");
    french.put("shortcuts", "Raccourcis clavier");
    french.put("action_undone", "Action annulée");
    french.put("action_redone", "Action rétablie");
    french.put("selected_count", "%d étudiants sélectionnés");
    french.put("deselected_all", "Tous les étudiants désélectionnés");
    french.put("marked_present", "%d étudiants marqués présents");
    french.put("marked_absent", "%d étudiants marqués absents");
    french.put("status_ready", "Prêt");

    // Results tab
    french.put("attendance_analysis_results", "Résultats de l'analyse de présence");
    french.put("search_name", "Rechercher un nom:");
    french.put("enter_student_name", "Entrer le nom de l'étudiant...");
    french.put("filter_status", "Filtrer par statut:");
    french.put("all", "Tous");
    french.put("clear_filters", "Effacer les filtres");
    french.put("refresh_results", "Rafraîchir les résultats");
    french.put("summary_statistics", "Statistiques de synthèse");
    french.put("no_results_yet", "Pas encore de résultats");
    french.put("no_analysis_results", "Aucun résultat d'analyse. Lancez l'analyse depuis l'onglet Classe.");
    french.put("export_json", "Exporter en JSON");
    french.put("export_csv", "Exporter en CSV");
    french.put("export_pdf", "Exporter en PDF");
    french.put("export_excel", "Exporter en Excel");
    french.put("export_image", "Exporter en image");
    french.put("student_name_col", "Nom de l'étudiant");
    french.put("status_col", "Statut");
    french.put("confidence_col", "Confiance");
    french.put("neighbor_col", "Voisin");
    french.put("occupancy_col", "Occupation");
    french.put("consensus_col", "Consensus");

    // Conflict tab
    french.put("conflict_title", "Analyse des conflits et détection des menteurs");
    french.put("analyze_conflicts", "Analyser les conflits");
    french.put("conflict_prompt", "Cliquez sur 'Analyser les conflits' pour détecter les fausses déclarations");
    french.put("conflict_details", "Détails des conflits:");
    french.put("no_data", "Pas de données");
    french.put("no_classroom_or_students", "Aucune classe ou étudiant disponible");
    french.put("no_conflicts", "✓ Aucun conflit détecté. Tous les étudiants sont honnêtes !");
    french.put("analysis_complete", "Analyse terminée.");
    french.put("analysis_failed", "Échec de l'analyse");
    french.put("conflict_detected_summary", "⚠ %d conflit(s) détecté(s) - Étudiants faisant de fausses déclarations");
    french.put("conflict_report_header", "=== RAPPORT D'ANALYSE DES CONFLITS ===");
    french.put("conflict_total_conflicts", "Nombre total de conflits : %d");
    french.put("conflict_total_students", "Nombre total d'étudiants : %d");
    french.put("conflict_rate", "Taux de conflit : %.1f%%");
    french.put("conflict_detailed", "=== CONFLITS DÉTAILLÉS ===");
    french.put("conflict_claimer", "Déclarant : %s");
    french.put("conflict_type", "Type de conflit : %s");
    french.put("conflict_detail_line", "Détails : %s");
    french.put("conflict_separator", "---");
    french.put("conflict_suspicious", "=== ÉTUDIANTS SUSPECTS (FAIBLE RÉCIPROCITÉ) ===");
    french.put("conflict_suspicious_entry", "⚠ %s (ID : %s)");
    french.put("conflict_truthful_summary",
        "Tous les étudiants ont fourni des déclarations véridiques sur leurs voisins.\nNombre total d'étudiants : %d");
    french.put("conflict_error", "Erreur lors de l'analyse des conflits : %s");
    french.put("audit_log_cleared", "Journal d'audit effacé.");

    // History tab
    french.put("clear_history", "Effacer l'historique");
    french.put("past_analyses", "Analyses passées:");
    french.put("details", "Détails");
    french.put("clear_history_confirm", "Supprimer tous les enregistrements de l'historique ?");
    french.put("error", "Erreur");
    french.put("failed_clear_history", "Échec de la suppression de l'historique :");

    // Settings tab
    french.put("configuration_settings", "Paramètres de configuration");
    french.put("strategy_weights", "Pondérations des stratégies");
    french.put("confidence_thresholds", "Seuils de confiance");
    french.put("apply_settings", "Appliquer les paramètres");
    french.put("reset_defaults", "Réinitialiser les valeurs par défaut");
    french.put("settings_warnings_title", "Avertissements de validation des paramètres");
    french.put("configuration_issues", "Problèmes de configuration détectés :");
    french.put("continue", "Continuer");
    french.put("cancel", "Annuler");
    french.put("name", "Nom");
    french.put("id", "ID");
    french.put("row", "Ligne");
    french.put("col", "Colonne");
    french.put("absent", "Absent");
    french.put("no_claims_recorded", "Aucune déclaration enregistrée");
    french.put("verification_score", "Score de vérification");
    french.put("direction_left", "Gauche");
    french.put("direction_right", "Droite");
    french.put("direction_front", "Devant");
    french.put("direction_back", "Arrière");

    translations.put(Language.FRENCH, french);
  }

  public void setLanguage(Language language) {
    System.out.println("[DEBUG] setLanguage() called with: " + language);
    this.currentLanguage = language;
    saveLanguagePreference();
    System.out.println("[DEBUG] About to call notifyListeners()");
    notifyListeners();
    System.out.println("[DEBUG] setLanguage() complete");
  }

  public void addLanguageChangeListener(LanguageChangeListener listener) {
    System.out.println("[DEBUG] addLanguageChangeListener() called for: " + listener.getClass().getName());
    listeners.add(listener);
    System.out.println("[DEBUG] Listener added. Total listeners now: " + listeners.size());
  }

  public void removeLanguageChangeListener(LanguageChangeListener listener) {
    listeners.remove(listener);
  }

  private void notifyListeners() {
    System.out.println("[DEBUG] notifyListeners() called. Current language: " + currentLanguage + ", Listeners count: "
        + listeners.size());
    for (LanguageChangeListener listener : listeners) {
      System.out.println("[DEBUG] Notifying listener: " + listener.getClass().getName());
      listener.onLanguageChanged(currentLanguage);
    }
    System.out.println("[DEBUG] notifyListeners() complete");
  }

  public Language getCurrentLanguage() {
    return currentLanguage;
  }

  public String get(String key) {
    Map<String, String> current = translations.get(currentLanguage);
    return current.getOrDefault(key, key);
  }

  public String get(String key, Object... args) {
    String template = get(key);
    return String.format(template, args);
  }

  public List<Language> getAvailableLanguages() {
    return Arrays.asList(Language.values());
  }

  private void saveLanguagePreference() {
    try {
      Files.createDirectories(Paths.get(PREFS_DIR));
      Files.write(Paths.get(LANG_PREFS_FILE), currentLanguage.name().getBytes());
      System.out.println("[DEBUG] Language preference saved: " + currentLanguage.name() + " to " + LANG_PREFS_FILE);
    } catch (IOException e) {
      System.err.println("Failed to save language preference: " + e.getMessage());
    }
  }

  private void loadLanguagePreference() {
    try {
      if (Files.exists(Paths.get(LANG_PREFS_FILE))) {
        String langName = new String(Files.readAllBytes(Paths.get(LANG_PREFS_FILE))).trim();
        System.out.println("[DEBUG] Language file found, content: '" + langName + "'");
        try {
          currentLanguage = Language.valueOf(langName);
          System.out.println("[DEBUG] Language loaded from preference: " + currentLanguage.name());
        } catch (IllegalArgumentException e) {
          System.out.println("[DEBUG] Invalid language in preference file: " + langName + ", using ENGLISH");
          currentLanguage = Language.ENGLISH;
        }
      } else {
        System.out
            .println("[DEBUG] No language preference file found at " + LANG_PREFS_FILE + ", using default: ENGLISH");
      }
    } catch (IOException e) {
      System.err.println("Failed to load language preference: " + e.getMessage());
    }
  }
}
