package com.whoami.domain.entities;

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

    public boolean isInside(Position pos){
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
}

