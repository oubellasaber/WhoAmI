package org.example.ui;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditLogger {
  private static final String LOG_DIR = System.getProperty("user.home") + "/.whoami";
  private static final String LOG_FILE = LOG_DIR + "/audit.log";
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  static {
    try {
      Path logDir = Paths.get(LOG_DIR);
      if (!Files.exists(logDir)) {
        Files.createDirectories(logDir);
      }
    } catch (IOException e) {
      System.err.println("Failed to create audit log directory: " + e.getMessage());
    }
  }

  public static void log(String action, String details) {
    try {
      String timestamp = LocalDateTime.now().format(FORMATTER);
      String logEntry = String.format("[%s] %s: %s", timestamp, action, details);
      Files.write(Paths.get(LOG_FILE), (logEntry + "\n").getBytes(),
          StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException e) {
      System.err.println("Failed to write audit log: " + e.getMessage());
    }
  }

  public static String readAuditLog() {
    try {
      return Files.exists(Paths.get(LOG_FILE))
          ? Files.readString(Paths.get(LOG_FILE))
          : "No audit log entries yet.";
    } catch (IOException e) {
      return "Error reading audit log: " + e.getMessage();
    }
  }

  public static void clearAuditLog() {
    try {
      Path logPath = Paths.get(LOG_FILE);
      if (Files.exists(logPath)) {
        Files.delete(logPath);
      }
    } catch (IOException e) {
      System.err.println("Failed to clear audit log: " + e.getMessage());
    }
  }
}
