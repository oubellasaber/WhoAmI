package com.whoami.domain.strategies;

import com.whoami.domain.entities.Classroom;
import com.whoami.domain.entities.Claim;
import com.whoami.domain.entities.Direction;
import com.whoami.domain.entities.LocatedStudent;
import com.whoami.domain.entities.Position;
import com.whoami.domain.entities.Student;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Verification strategy based on peer consensus scoring.
 * Counts how many peers independently confirm the presence of a student.
 */
public class ConsensusScoreStrategy implements VerificationStrategy {

  @Override
  public double verify(LocatedStudent student, Classroom classroom) {
    if (student.getPosition() == null) {
      return 0.0;
    }

    Position pos = student.getPosition();
    int totalConfirmations = 0;
    int totalPotentialConfirmers = 0;

    // Count confirmations from all students in the classroom
    Map<Position, LocatedStudent> allPlacements = classroom.allPlacements();

    for (LocatedStudent other : allPlacements.values()) {
      if (other.equals(student)) {
        continue; // Skip self
      }

      // Check if 'other' is adjacent to our student
      if (isAdjacent(other.getPosition(), pos)) {
        totalPotentialConfirmers++;

        // Check if 'other' claimed to see our student
        if (hasClaimedStudent(other, student.getStudent())) {
          totalConfirmations++;
        }
      }
    }

    if (totalPotentialConfirmers == 0) {
      return 0.5; // No adjacent students, neutral score
    }

    // Consensus score: ratio of confirmers
    return (double) totalConfirmations / totalPotentialConfirmers;
  }

  private boolean isAdjacent(Position pos1, Position pos2) {
    int rowDiff = Math.abs(pos1.row() - pos2.row());
    int colDiff = Math.abs(pos1.col() - pos2.col());

    // Adjacent means horizontally or vertically next to each other
    return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1);
  }

  private boolean hasClaimedStudent(LocatedStudent claimer, Student target) {
    return claimer.getClaims().stream()
        .filter(claim -> !claim.isAbsentClaim()) // Skip absent claims
        .anyMatch(claim -> claim.getTarget().equals(target));
  }

  @Override
  public String getName() {
    return "ConsensusScore";
  }
}
