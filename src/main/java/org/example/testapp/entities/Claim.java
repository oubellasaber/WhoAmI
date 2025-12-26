package org.example.testapp.entities;

public class Claim {
    private final Direction direction;
    private final Student target; // null means "I declare this position is empty/absent"

    public Claim(Direction direction, Student target) {
        this.direction = direction;
        this.target = target; // Can be null for absent neighbor claims
    }

    public Direction getDirection() {
        return direction;
    }

    public Student getTarget() {
        return target;
    } // Returns null for absent claims

    /**
     * Check if this claim declares an absent/empty neighbor.
     */
    public boolean isAbsentClaim() {
        return target == null;
    }
}