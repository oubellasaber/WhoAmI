package com.whoami.domain.strategies;

import com.whoami.domain.entities.LocatedStudent;
import com.whoami.domain.entities.Classroom;
import java.util.Map;

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
