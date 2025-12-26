package org.example.testapp.strategies;

import org.example.testapp.entities.Classroom;
import org.example.testapp.entities.LocatedStudent;

/**
 * Interface for different attendance verification strategies.
 * Each strategy provides a confidence score based on a different approach.
 */
public interface VerificationStrategy {
  /**
   * Verify the attendance status of a student.
   * 
   * @param student   the student to verify
   * @param classroom the classroom containing all placed students
   * @return confidence score between 0.0 and 1.0
   */
  double verify(LocatedStudent student, Classroom classroom);

  /**
   * Get the name of this verification strategy
   */
  String getName();
}
