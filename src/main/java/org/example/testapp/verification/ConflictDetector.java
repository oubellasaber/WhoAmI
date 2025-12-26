package org.example.testapp.verification;

import org.example.testapp.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Detects conflicts and inconsistencies in student attendance claims.
 * Identifies suspicious patterns such as:
 * - Students claiming to see absent neighbors
 * - Contradictory claims between students
 * - Spatial impossibilities
 */
public class ConflictDetector {
  private final Classroom classroom;

  public ConflictDetector(Classroom classroom) {
    this.classroom = classroom;
  }

  /**
   * Detect all conflicts in the classroom.
   */
  public List<AttendanceConflict> detectAllConflicts() {
    List<AttendanceConflict> conflicts = new ArrayList<>();

    for (LocatedStudent student : classroom.allPlacements().values()) {
      conflicts.addAll(detectStudentConflicts(student));
    }

    return conflicts;
  }

  /**
   * Detect conflicts specific to one student.
   */
  public List<AttendanceConflict> detectStudentConflicts(LocatedStudent student) {
    List<AttendanceConflict> conflicts = new ArrayList<>();

    for (Claim claim : student.getClaims()) {
      // Skip absent claims (null target means student declares position is empty)
      if (claim.isAbsentClaim()) {
        continue;
      }

      // Check if claimed student is actually absent
      if (isClaimedStudentAbsent(claim.getTarget())) {
        conflicts.add(new AttendanceConflict(
            student.getStudent(),
            claim.getTarget(),
            AttendanceConflict.ConflictType.CLAIMING_ABSENT_STUDENT,
            "Student claimed to see " + claim.getTarget().getName() + " but they are absent"));
      }

      // Check for spatial impossibility (claiming someone in wrong direction)
      if (!isClaimSpatiallyValid(student, claim)) {
        conflicts.add(new AttendanceConflict(
            student.getStudent(),
            claim.getTarget(),
            AttendanceConflict.ConflictType.SPATIAL_IMPOSSIBILITY,
            "Claim about " + claim.getTarget().getName() + " in direction " + claim.getDirection()
                + " is spatially impossible from position " + student.getPosition()));
      }
    }

    return conflicts;
  }

  /**
   * Check if a claimed student is actually absent.
   */
  private boolean isClaimedStudentAbsent(Student target) {
    return classroom.allPlacements().values().stream()
        .noneMatch(ls -> ls.getStudent().equals(target));
  }

  /**
   * Verify that a claim is spatially valid (student is in the right direction).
   */
  private boolean isClaimSpatiallyValid(LocatedStudent claimer, Claim claim) {
    Position claimerPos = claimer.getPosition();
    if (claimerPos == null) {
      return true; // Can't validate, assume valid
    }

    Optional<LocatedStudent> targetOpt = classroom.allPlacements().values().stream()
        .filter(ls -> ls.getStudent().equals(claim.getTarget()))
        .findFirst();

    if (targetOpt.isEmpty()) {
      return false; // Target not in classroom
    }

    LocatedStudent target = targetOpt.get();
    Position targetPos = target.getPosition();

    return isInDirection(claimerPos, targetPos, claim.getDirection());
  }

  private boolean isInDirection(Position from, Position to, org.example.testapp.entities.Direction direction) {
    return switch (direction) {
      case LEFT -> to.col() < from.col() && to.row() == from.row();
      case RIGHT -> to.col() > from.col() && to.row() == from.row();
      case FRONT -> to.row() < from.row() && to.col() == from.col();
      case BACK -> to.row() > from.row() && to.col() == from.col();
    };
  }

  /**
   * Find students with suspicious behavior (many unreciprocated claims).
   */
  public List<Student> findSuspiciousStudents(double minReciprocityThreshold) {
    PeerVerifier verifier = new PeerVerifier(classroom);
    List<Student> suspicious = new ArrayList<>();

    for (LocatedStudent student : classroom.allPlacements().values()) {
      double reciprocityRatio = verifier.getReciprocityRatio(student);
      if (reciprocityRatio < minReciprocityThreshold) {
        suspicious.add(student.getStudent());
      }
    }

    return suspicious;
  }
}
