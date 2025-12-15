package com.whoami.domain.strategies;

import com.whoami.domain.entities.Classroom;
import com.whoami.domain.entities.Claim;
import com.whoami.domain.entities.Direction;
import com.whoami.domain.entities.LocatedStudent;
import com.whoami.domain.entities.Position;
import com.whoami.domain.entities.Student;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Verification strategy based on neighbor confirmation.
 * Checks if neighboring students have claimed this student's presence.
 */
public class NeighborVerificationStrategy implements VerificationStrategy {

  @Override
  public double verify(LocatedStudent student, Classroom classroom) {
    if (student.getPosition() == null || classroom == null) {
      return 0.0;
    }

    Position pos = student.getPosition();
    int confirmedNeighbors = 0;
    int totalNeighbors = 0;

    // Check LEFT neighbor
    if (pos.col() > 0) {
      Position left = new Position(pos.row(), pos.col() - 1);
      if (classroom.isInside(left)) {
        totalNeighbors++;
        Optional<LocatedStudent> neighbor = classroom.getAt(left);
        if (neighbor.isPresent() && hasClaimAbout(neighbor.get(), student.getStudent(), Direction.RIGHT)) {
          confirmedNeighbors++;
        }
      }
    }

    // Check RIGHT neighbor
    Position right = new Position(pos.row(), pos.col() + 1);
    if (classroom.isInside(right)) {
      totalNeighbors++;
      Optional<LocatedStudent> neighbor = classroom.getAt(right);
      if (neighbor.isPresent() && hasClaimAbout(neighbor.get(), student.getStudent(), Direction.LEFT)) {
        confirmedNeighbors++;
      }
    }

    // Check FRONT neighbor
    if (pos.row() > 0) {
      Position front = new Position(pos.row() - 1, pos.col());
      if (classroom.isInside(front)) {
        totalNeighbors++;
        Optional<LocatedStudent> neighbor = classroom.getAt(front);
        if (neighbor.isPresent() && hasClaimAbout(neighbor.get(), student.getStudent(), Direction.BACK)) {
          confirmedNeighbors++;
        }
      }
    }

    // Check BACK neighbor
    Position back = new Position(pos.row() + 1, pos.col());
    if (classroom.isInside(back)) {
      totalNeighbors++;
      Optional<LocatedStudent> neighbor = classroom.getAt(back);
      if (neighbor.isPresent() && hasClaimAbout(neighbor.get(), student.getStudent(), Direction.FRONT)) {
        confirmedNeighbors++;
      }
    }

    if (totalNeighbors == 0) {
      return 0.5; // No neighbors, neutral confidence
    }

    return (double) confirmedNeighbors / totalNeighbors;
  }

  private boolean hasClaimAbout(LocatedStudent neighbor, Student target, Direction direction) {
    return neighbor.getClaims().stream()
        .filter(claim -> !claim.isAbsentClaim()) // Skip absent claims
        .anyMatch(claim -> claim.getTarget().equals(target) && claim.getDirection() == direction);
  }

  @Override
  public String getName() {
    return "NeighborVerification";
  }
}
