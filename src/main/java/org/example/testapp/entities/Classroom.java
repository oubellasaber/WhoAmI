package org.example.testapp.entities;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Classroom {
    private final int rows;
    private final int cols;

    private final Map<Position, LocatedStudent> placements = new HashMap<>();

    public Classroom(int rows, int cols) {
        if (rows <= 0 || cols <= 0)
            throw new IllegalArgumentException("Rows and columns must be > 0");

        this.rows = rows;
        this.cols = cols;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public boolean isInside(Position pos) {
        return pos.row() >= 0 && pos.row() < rows
                && pos.col() >= 0 && pos.col() < cols;
    }

    public boolean isOccupied(Position pos) {
        return placements.containsKey(pos);
    }

    public Optional<LocatedStudent> getAt(Position pos) {
        return Optional.ofNullable(placements.get(pos));
    }

    public void place(LocatedStudent student) {
        if (!isInside(student.getPosition()))
            throw new IllegalArgumentException("Position out of classroom bounds");

        if (isOccupied(student.getPosition()))
            throw new IllegalStateException("Seat already occupied");

        placements.put(student.getPosition(), student);
    }

    public Map<Position, LocatedStudent> allPlacements() {
        return Map.copyOf(placements);
    }

    /**
     * Get all actual neighbors of a student at the given position.
     * Returns a map of Direction -> LocatedStudent for all occupied neighbor seats.
     */
    public Map<Direction, LocatedStudent> getNeighborsOf(Position pos) {
        Map<Direction, LocatedStudent> neighbors = new HashMap<>();

        for (Direction dir : Direction.values()) {
            Position neighborPos = pos.getNeighbor(dir);
            if (neighborPos != null && isInside(neighborPos) && isOccupied(neighborPos)) {
                getAt(neighborPos).ifPresent(student -> neighbors.put(dir, student));
            }
        }

        return neighbors;
    }

    /**
     * Validate that a student has declared all their actual neighbors.
     * A student must declare either:
     * 1. The actual occupied neighbor, OR
     * 2. That the position is empty/absent (null target)
     * 
     * @return true if all neighbors are declared, false otherwise
     */
    public boolean hasAllNeighborsDeclared(LocatedStudent student) {
        if (student.getPosition() == null) {
            return true; // No position means no neighbors to declare
        }

        Map<Direction, LocatedStudent> actualNeighbors = getNeighborsOf(student.getPosition());

        // Check that each actual neighbor is declared in claims
        for (Map.Entry<Direction, LocatedStudent> entry : actualNeighbors.entrySet()) {
            Direction direction = entry.getKey();
            LocatedStudent neighbor = entry.getValue();

            boolean isDeclared = student.getClaims().stream()
                    .anyMatch(claim -> claim.getDirection() == direction &&
                            !claim.isAbsentClaim() &&
                            claim.getTarget().equals(neighbor.getStudent()));

            if (!isDeclared) {
                return false;
            }
        }

        // Also check: student might have claimed empty positions that actually exist
        // For validation purposes, we accept claims of empty/absent neighbors
        // as long as they don't conflict with actual occupied neighbors

        return true;
    }

    /**
     * Validate all students have declared all their neighbors.
     * 
     * @throws IllegalStateException if any student hasn't declared all neighbors
     */
    public void validateAllNeighborsDeclared() {
        for (LocatedStudent student : placements.values()) {
            if (!hasAllNeighborsDeclared(student)) {
                throw new IllegalStateException(
                        "Student " + student.getStudent().getName() +
                                " has not declared all neighbors");
            }
        }
    }
}
