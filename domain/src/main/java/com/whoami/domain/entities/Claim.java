package com.whoami.domain.entities;

public class Claim {
    private final Direction direction;
    private final Student target;

    public Claim(Direction direction, Student target) {
        this.direction = direction;
        this.target = target;
    }

    public Direction getDirection() { return direction; }
    public Student getTarget() { return target; }
}