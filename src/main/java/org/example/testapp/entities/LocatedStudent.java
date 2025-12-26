package org.example.testapp.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LocatedStudent {

    private final Student student;

    // Nullable: we might not know the position yet
    private Position position;

    // All neighbor claims made by this student
    private final List<Claim> claims = new ArrayList<>();

    // State of this student in the system
    private Status status = Status.UNKNOWN;

    public enum Status {
        UNKNOWN,
        PLACED,
        ABSENT,
        CONFLICTING
    }

    public LocatedStudent(Student student) {
        this.student = student;
    }

    public Student getStudent() {
        return student;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
        this.status = Status.PLACED;
    }

    public List<Claim> getClaims() {
        return claims;
    }

    public void addClaim(Claim claim) {
        this.claims.add(claim);
    }

    public Status getStatus() {
        return status;
    }

    public void markAbsent() {
        this.status = Status.ABSENT;
    }

    public void markConflicting() {
        this.status = Status.CONFLICTING;
    }

    /**
     * Check if this student has declared a neighbor in the given direction.
     */
    public boolean hasDeclaredNeighbor(Direction direction) {
        return claims.stream()
                .anyMatch(claim -> claim.getDirection() == direction);
    }

    /**
     * Get the claimed neighbor in the given direction, if any.
     */
    public Optional<Student> getClaimedNeighbor(Direction direction) {
        return claims.stream()
                .filter(claim -> claim.getDirection() == direction)
                .map(Claim::getTarget)
                .findFirst();
    }
}
