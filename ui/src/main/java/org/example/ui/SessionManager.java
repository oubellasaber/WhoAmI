package org.example.ui;

import com.whoami.domain.entities.Classroom;
import com.whoami.domain.entities.Student;
import com.whoami.domain.entities.LocatedStudent;
import com.whoami.domain.entities.Position;
import com.whoami.domain.entities.Claim;
import com.whoami.domain.entities.Direction;
import java.io.*;
import java.util.*;

/**
 * Manages saving and loading classroom sessions.
 */
public class SessionManager {

  public static void saveSession(Classroom classroom, File file) throws IOException {
    try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
      // Save classroom dimensions
      writer.println("CLASSROOM," + classroom.getRows() + "," + classroom.getCols());

      // Save students and their placements
      for (var entry : classroom.allPlacements().entrySet()) {
        LocatedStudent locStudent = entry.getValue();
        Position pos = entry.getKey();

        writer.println("STUDENT," + locStudent.getStudent().getId() + "," + locStudent.getStudent().getName());
        writer.println("POSITION," + pos.row() + "," + pos.col());

        // Save claims
        for (Claim claim : locStudent.getClaims()) {
          if (claim.isAbsentClaim()) {
            writer.println("CLAIM," + claim.getDirection().name() + ",ABSENT");
          } else {
            writer.println("CLAIM," + claim.getDirection().name() + "," + claim.getTarget().getId());
          }
        }
      }
    }
  }

  public static Classroom loadSession(File file) throws IOException {
    Classroom classroom = null;
    Map<String, Student> students = new HashMap<>();
    Map<String, LocatedStudent> locatedStudents = new HashMap<>();
    Map<String, Position> studentPositions = new HashMap<>();
    String currentStudent = null;
    String currentStudentId = null;

    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split(",");
        if (parts.length == 0)
          continue;

        switch (parts[0]) {
          case "CLASSROOM" -> {
            if (parts.length >= 3) {
              int rows = Integer.parseInt(parts[1]);
              int cols = Integer.parseInt(parts[2]);
              classroom = new Classroom(rows, cols);
            }
          }
          case "STUDENT" -> {
            if (parts.length >= 3) {
              currentStudentId = parts[1];
              currentStudent = parts[2];
              Student student = new Student(currentStudentId, currentStudent);
              students.put(currentStudentId, student);
              LocatedStudent locStudent = new LocatedStudent(student);
              locatedStudents.put(currentStudentId, locStudent);
            }
          }
          case "POSITION" -> {
            if (currentStudentId != null && parts.length >= 3) {
              int row = Integer.parseInt(parts[1]);
              int col = Integer.parseInt(parts[2]);
              Position pos = new Position(row, col);
              studentPositions.put(currentStudentId, pos);
              locatedStudents.get(currentStudentId).setPosition(pos);
            }
          }
          case "CLAIM" -> {
            if (currentStudentId != null && parts.length >= 3) {
              Direction direction = Direction.valueOf(parts[1]);
              String targetId = parts[2];

              Student target = targetId.equals("ABSENT") ? null : students.get(targetId);
              Claim claim = new Claim(direction, target);
              locatedStudents.get(currentStudentId).addClaim(claim);
            }
          }
        }
      }
    }

    if (classroom == null) {
      throw new IOException("Invalid session file: classroom dimensions not found");
    }

    // Place all students
    for (var entry : locatedStudents.entrySet()) {
      LocatedStudent locStudent = entry.getValue();
      if (locStudent.getPosition() != null) {
        classroom.place(locStudent);
      }
    }

    return classroom;
  }
}
