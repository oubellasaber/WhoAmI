package com.whoami.domain.entities;

import java.util.Objects;

public final class Position {
    private final int row;
    private final int col;

    public Position(int row, int col) {
        if (row < 0 || col < 0) {
            throw new IllegalArgumentException("Row and col must be >= 0");
        }
        this.row = row;
        this.col = col;
    }

    public int row() {
        return row;
    }

    public int col() {
        return col;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Position position))
            return false;
        return row == position.row && col == position.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return "(" + row + "," + col + ")";
    }

    /**
     * Get the neighbor position in the given direction.
     * Returns null if the direction would result in negative coordinates.
     */
    public Position getNeighbor(Direction direction) {
        return switch (direction) {
            case LEFT -> col > 0 ? new Position(row, col - 1) : null;
            case RIGHT -> new Position(row, col + 1);
            case FRONT -> row > 0 ? new Position(row - 1, col) : null;
            case BACK -> new Position(row + 1, col);
        };
    }
}
