package org.example.testapp.verification;

import org.example.testapp.entities.Student;

/**
 * Represents a detected conflict in attendance reporting.
 */
public class AttendanceConflict {
  private final Student student;
  private final Student involvedStudent;
  private final ConflictType type;
  private final String description;

  public enum ConflictType {
    CLAIMING_ABSENT_STUDENT,
    SPATIAL_IMPOSSIBILITY,
    CONTRADICTORY_CLAIMS,
    SUSPICIOUS_PATTERN
  }

  public AttendanceConflict(Student student, Student involvedStudent, ConflictType type, String description) {
    this.student = student;
    this.involvedStudent = involvedStudent;
    this.type = type;
    this.description = description;
  }

  public Student getStudent() {
    return student;
  }

  public Student getInvolvedStudent() {
    return involvedStudent;
  }

  public ConflictType getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return String.format("[%s] %s: %s", type, student.getName(), description);
  }
}
