package org.example.testapp.attendance;

import org.example.testapp.entities.LocatedStudent;
import org.example.testapp.entities.Student;

import java.util.Objects;

/**
 * Represents the attendance result for a single student.
 * Includes the student, their inferred status, and the confidence score.
 */
public class AttendanceReport {
  private final LocatedStudent locatedStudent;
  private final AttendanceStatus status;
  private final double confidenceScore;
  private final String reason;

  public enum AttendanceStatus {
    PRESENT,
    ABSENT,
    CONFLICTING,
    UNCERTAIN
  }

  public AttendanceReport(LocatedStudent locatedStudent, AttendanceStatus status,
      double confidenceScore, String reason) {
    this.locatedStudent = locatedStudent;
    this.status = status;
    this.confidenceScore = Math.max(0.0, Math.min(1.0, confidenceScore)); // Clamp to [0, 1]
    this.reason = reason;
  }

  public LocatedStudent getLocatedStudent() {
    return locatedStudent;
  }

  public Student getStudent() {
    return locatedStudent.getStudent();
  }

  public AttendanceStatus getStatus() {
    return status;
  }

  public double getConfidenceScore() {
    return confidenceScore;
  }

  public String getReason() {
    return reason;
  }

  @Override
  public String toString() {
    return String.format("%s: %s (confidence: %.2f%%) - %s",
        getStudent().getName(), status, confidenceScore * 100, reason);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof AttendanceReport report))
      return false;
    return Objects.equals(locatedStudent, report.locatedStudent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(locatedStudent);
  }
}
