package com.whoami.domain.entities;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke tests for core domain entities.
 */
public class DomainSmokeTest {

  @Test
  public void testStudentCreation() {
    Student student = new Student("S001", "Alice");
    assertEquals("S001", student.getId());
    assertEquals("Alice", student.getName());
  }

  @Test
  public void testPositionCreation() {
    Position pos = new Position(2, 3);
    assertEquals(2, pos.row());
    assertEquals(3, pos.col());
  }

  @Test
  public void testLocatedStudentCreation() {
    Student student = new Student("S002", "Bob");
    LocatedStudent located = new LocatedStudent(student);
    assertEquals("Bob", located.getStudent().getName());
    assertNull(located.getPosition());
  }

  @Test
  public void testLocatedStudentSetPosition() {
    Student student = new Student("S003", "Charlie");
    LocatedStudent located = new LocatedStudent(student);
    Position pos = new Position(1, 2);
    located.setPosition(pos);
    assertNotNull(located.getPosition());
    assertEquals(1, located.getPosition().row());
  }

  @Test
  public void testClaimCreation() {
    Student target = new Student("S004", "Diana");
    Claim claim = new Claim(Direction.LEFT, target);
    assertEquals(Direction.LEFT, claim.getDirection());
    assertEquals("Diana", claim.getTarget().getName());
  }

  @Test
  public void testClaimWithNullTarget() {
    Claim claim = new Claim(Direction.RIGHT, null);
    assertEquals(Direction.RIGHT, claim.getDirection());
    assertNull(claim.getTarget());
  }

  @Test
  public void testClassroomCreation() {
    Classroom classroom = new Classroom(3, 4);
    assertEquals(3, classroom.getRows());
    assertEquals(4, classroom.getCols());
  }

  @Test
  public void testClassroomPlacement() {
    Classroom classroom = new Classroom(2, 2);
    Student student = new Student("S005", "Eve");
    LocatedStudent located = new LocatedStudent(student);
    Position pos = new Position(0, 1);
    located.setPosition(pos);

    classroom.place(located);

    assertTrue(classroom.getAt(pos).isPresent());
    assertEquals("Eve", classroom.getAt(pos).get().getStudent().getName());
  }

  @Test
  public void testAddClaim() {
    Student claimer = new Student("S006", "Frank");
    LocatedStudent claimerLoc = new LocatedStudent(claimer);
    Student target = new Student("S007", "Grace");

    Claim claim = new Claim(Direction.FRONT, target);
    claimerLoc.addClaim(claim);

    assertEquals(1, claimerLoc.getClaims().size());
    assertEquals(Direction.FRONT, claimerLoc.getClaims().get(0).getDirection());
  }

  @Test
  public void testDirectionEnum() {
    assertEquals(Direction.LEFT, Direction.LEFT);
    assertEquals(Direction.RIGHT, Direction.RIGHT);
    assertEquals(Direction.FRONT, Direction.FRONT);
    assertEquals(Direction.BACK, Direction.BACK);
  }
}
