package org.example.testapp.strategies;

import org.example.testapp.entities.Classroom;
import org.example.testapp.entities.LocatedStudent;

/**
 * Verification strategy based on seat occupancy reporting.
 * Students who report their position actively are more likely to be present.
 */
public class SeatOccupancyStrategy implements VerificationStrategy {

  @Override
  public double verify(LocatedStudent student, Classroom classroom) {
    // Student is placed at a seat: high confidence of presence
    if (student.getPosition() != null) {
      // Additional boost if they've made claims (peer engagement)
      if (!student.getClaims().isEmpty()) {
        return 0.95; // Very high confidence
      }
      return 0.85; // High confidence
    }

    // Student with no position but has claims: reported neighbors without placing
    // themselves
    if (!student.getClaims().isEmpty()) {
      return 0.60; // Moderate confidence
    }

    // Student with nothing reported: likely absent
    return 0.1; // Very low confidence
  }

  @Override
  public String getName() {
    return "SeatOccupancy";
  }
}
