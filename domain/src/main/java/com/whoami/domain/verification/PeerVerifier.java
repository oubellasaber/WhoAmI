package com.whoami.domain.verification;

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
 * Verifies student claims through peer confirmation.
 * Cross-validates neighbor reports and detects inconsistencies.
 */
public class PeerVerifier {
  private final Classroom classroom;

  public PeerVerifier(Classroom classroom) {
    this.classroom = classroom;
  }

  /**
   * Verify if a claim is reciprocated by the claimed student.
   * 
   * @param claimer the student making the claim
   * @param claim   the claim being made
   * @return true if the target has a reciprocal claim
   */
  public boolean isClaimReciprocated(LocatedStudent claimer, Claim claim) {
    // Absent claims cannot be reciprocated
    if (claim.isAbsentClaim()) {
      return false;
    }

    Student target = claim.getTarget();
    Direction direction = claim.getDirection();

    // Find the target student in the classroom
    Optional<LocatedStudent> targetOpt = classroom.allPlacements().values().stream()
        .filter(ls -> ls.getStudent().equals(target))
        .findFirst();

    if (targetOpt.isEmpty()) {
      return false;
    }

    LocatedStudent targetStudent = targetOpt.get();
    Direction oppositeDirection = getOppositeDirection(direction);

    // Check if target has a claim about the claimer in the opposite direction
    return targetStudent.getClaims().stream()
        .filter(targetClaim -> !targetClaim.isAbsentClaim()) // Skip absent claims
        .anyMatch(targetClaim -> targetClaim.getTarget().equals(claimer.getStudent()) &&
            targetClaim.getDirection() == oppositeDirection);
  }

  /**
   * Count how many students confirm seeing a particular student.
   */
  public int countConfirmations(LocatedStudent student) {
    return (int) classroom.allPlacements().values().stream()
        .filter(other -> !other.equals(student))
        .filter(other -> other.getClaims().stream()
            .filter(claim -> !claim.isAbsentClaim()) // Skip absent claims
            .anyMatch(claim -> claim.getTarget().equals(student.getStudent())))
        .count();
  }

  /**
   * Count total claims made by a student.
   */
  public int countStudentClaims(LocatedStudent student) {
    return student.getClaims().size();
  }

  /**
   * Count how many of a student's claims are reciprocated.
   */
  public int countReciprocalClaims(LocatedStudent student) {
    return (int) student.getClaims().stream()
        .filter(claim -> isClaimReciprocated(student, claim))
        .count();
  }

  /**
   * Get the reciprocity ratio for a student (reciprocal claims / total claims).
   */
  public double getReciprocityRatio(LocatedStudent student) {
    int totalClaims = countStudentClaims(student);
    if (totalClaims == 0) {
      return 0.5; // Neutral if no claims
    }

    int reciprocalClaims = countReciprocalClaims(student);
    return (double) reciprocalClaims / totalClaims;
  }

  private Direction getOppositeDirection(Direction direction) {
    return switch (direction) {
      case LEFT -> Direction.RIGHT;
      case RIGHT -> Direction.LEFT;
      case FRONT -> Direction.BACK;
      case BACK -> Direction.FRONT;
    };
  }
}
