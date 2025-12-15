package org.example;

import com.whoami.domain.entities.*;
import com.whoami.domain.attendance.AttendanceAggregator;
import com.whoami.domain.attendance.AttendanceReport;
import com.whoami.domain.strategies.*;
import com.whoami.domain.verification.ConflictDetector;
import com.whoami.domain.verification.PeerVerifier;
import com.whoami.domain.verification.AttendanceConflict;

import java.util.*;

/**
 * Multi-Strategy Smart Attendance System Demonstration
 *
 * This system automatically tracks student presence using:
 * 1. Neighbor-based verification (peer confirmation)
 * 2. Seat occupancy reporting (position-based evidence)
 * 3. Consensus scoring (distributed verification)
 *
 * The system detects absences, inconsistencies, and suspicious patterns
 * through peer-confirmation networks and consensus algorithms.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║   Multi-Strategy Smart Attendance System                    ║");
        System.out.println("║   Self-Verifying Peer Confirmation Network                  ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        // Initialize classroom: 3 rows × 4 columns
        Classroom classroom = new Classroom(3, 4);

        // Create students
        Student s1 = new Student("1", "Alice");
        Student s2 = new Student("2", "Bob");
        Student s3 = new Student("3", "Charlie");
        Student s4 = new Student("4", "Diana");
        Student s5 = new Student("5", "Eve");
        Student s6 = new Student("6", "Frank");
        Student s7 = new Student("7", "Grace");

        // Create located students and place them in classroom
        LocatedStudent alice = new LocatedStudent(s1);
        alice.setPosition(new Position(0, 1));

        LocatedStudent bob = new LocatedStudent(s2);
        bob.setPosition(new Position(0, 2));

        LocatedStudent charlie = new LocatedStudent(s3);
        charlie.setPosition(new Position(1, 1));

        LocatedStudent diana = new LocatedStudent(s4);
        diana.setPosition(new Position(1, 2));

        LocatedStudent eve = new LocatedStudent(s5);
        eve.setPosition(new Position(0, 0));

        LocatedStudent frank = new LocatedStudent(s6);
        frank.setPosition(new Position(2, 1));

        LocatedStudent grace = new LocatedStudent(s7);
        grace.setPosition(new Position(1, 3));

        // Place all students in classroom
        classroom.place(alice);
        classroom.place(bob);
        classroom.place(charlie);
        classroom.place(diana);
        classroom.place(eve);
        classroom.place(frank);
        classroom.place(grace);

        System.out.println("📍 CLASSROOM LAYOUT:");
        printClassroomLayout(classroom);
        System.out.println();

        // Students make neighbor claims (peer verification)
        System.out.println("📋 STUDENT CLAIMS (Peer Verification):");
        alice.addClaim(new Claim(Direction.RIGHT, s2)); // Alice claims Bob is to her right
        alice.addClaim(new Claim(Direction.BACK, s3)); // Alice claims Charlie is behind her
        alice.addClaim(new Claim(Direction.LEFT, s5)); // Alice claims Eve is to her left

        bob.addClaim(new Claim(Direction.LEFT, s1)); // Bob claims Alice is to his left
        bob.addClaim(new Claim(Direction.BACK, s4)); // Bob claims Diana is behind him

        charlie.addClaim(new Claim(Direction.FRONT, s1)); // Charlie claims Alice is in front
        charlie.addClaim(new Claim(Direction.RIGHT, s4)); // Charlie claims Diana is to his right

        diana.addClaim(new Claim(Direction.LEFT, s3)); // Diana claims Charlie is to her left
        diana.addClaim(new Claim(Direction.FRONT, s2)); // Diana claims Bob is in front
        diana.addClaim(new Claim(Direction.RIGHT, s7)); // Diana claims Grace is to her right

        eve.addClaim(new Claim(Direction.RIGHT, s1)); // Eve claims Alice is to her right

        frank.addClaim(new Claim(Direction.FRONT, s3)); // Frank claims Charlie is in front
        frank.addClaim(new Claim(Direction.RIGHT, s6)); // Frank makes a false claim (no one to his right)

        grace.addClaim(new Claim(Direction.LEFT, s4)); // Grace claims Diana is to her left

        printClaims(classroom);
        System.out.println();

        // Peer verification
        System.out.println("🔗 PEER VERIFICATION (Reciprocity Analysis):");
        PeerVerifier verifier = new PeerVerifier(classroom);
        printReciprocityAnalysis(classroom, verifier);
        System.out.println();

        // Conflict detection
        System.out.println("⚠️  CONFLICT DETECTION (Inconsistencies):");
        ConflictDetector detector = new ConflictDetector(classroom);
        List<AttendanceConflict> conflicts = detector.detectAllConflicts();
        printConflicts(conflicts);
        System.out.println();

        // Attendance aggregation using multiple strategies
        System.out.println("🎯 MULTI-STRATEGY ATTENDANCE ANALYSIS:");
        Map<String, Double> strategyWeights = new HashMap<>();
        strategyWeights.put("NeighborVerification", 0.4);
        strategyWeights.put("SeatOccupancy", 0.35);
        strategyWeights.put("ConsensusScore", 0.25);

        List<VerificationStrategy> strategies = Arrays.asList(
                new NeighborVerificationStrategy(),
                new SeatOccupancyStrategy(),
                new ConsensusScoreStrategy());

        AttendanceAggregator aggregator = new AttendanceAggregator(strategies, strategyWeights);
        aggregator.setClassroom(classroom);
        List<AttendanceReport> reports = classroom.allPlacements().values().stream()
                .map(aggregator::scoreStudent)
                .sorted(Comparator.comparingDouble(AttendanceReport::getConfidenceScore).reversed())
                .toList();

        printAttendanceReports(reports);
        System.out.println();

        // Summary statistics
        System.out.println("📊 SUMMARY STATISTICS:");
        long presentCount = reports.stream()
                .filter(r -> r.getStatus() == AttendanceReport.AttendanceStatus.PRESENT)
                .count();
        long absentCount = reports.stream()
                .filter(r -> r.getStatus() == AttendanceReport.AttendanceStatus.ABSENT)
                .count();
        long uncertainCount = reports.stream()
                .filter(r -> r.getStatus() == AttendanceReport.AttendanceStatus.UNCERTAIN)
                .count();

        System.out.printf("Total Students: %d%n", reports.size());
        System.out.printf("✓ Present: %d (%.1f%%)%n", presentCount, 100.0 * presentCount / reports.size());
        System.out.printf("✗ Absent: %d (%.1f%%)%n", absentCount, 100.0 * absentCount / reports.size());
        System.out.printf("? Uncertain: %d (%.1f%%)%n", uncertainCount, 100.0 * uncertainCount / reports.size());

        double avgConfidence = reports.stream()
                .mapToDouble(AttendanceReport::getConfidenceScore)
                .average()
                .orElse(0.0);
        System.out.printf("Average Confidence: %.2f%%%n", avgConfidence * 100);

        System.out.println("\n✅ Multi-Strategy Smart Attendance System Complete!\n");
    }

    private static void printClassroomLayout(Classroom classroom) {
        int maxRow = -1;
        int maxCol = -1;

        Map<Position, LocatedStudent> placements = classroom.allPlacements();
        for (Position pos : placements.keySet()) {
            maxRow = Math.max(maxRow, pos.row());
            maxCol = Math.max(maxCol, pos.col());
        }

        for (int r = 0; r <= maxRow; r++) {
            for (int c = 0; c <= maxCol; c++) {
                Position pos = new Position(r, c);
                if (classroom.isOccupied(pos)) {
                    LocatedStudent student = placements.get(pos);
                    String initials = student.getStudent().getName().substring(0, 1);
                    System.out.printf("[%s] ", initials);
                } else {
                    System.out.print("[ ] ");
                }
            }
            System.out.println();
        }
    }

    private static void printClaims(Classroom classroom) {
        for (LocatedStudent student : classroom.allPlacements().values()) {
            if (!student.getClaims().isEmpty()) {
                System.out.printf("%s claims neighbors:%n", student.getStudent().getName());
                for (Claim claim : student.getClaims()) {
                    String targetName = claim.isAbsentClaim() ? "[EMPTY]" : claim.getTarget().getName();
                    System.out.printf("  → %s is to the %s%n",
                            targetName,
                            claim.getDirection().toString().toLowerCase());
                }
            }
        }
    }

    private static void printReciprocityAnalysis(Classroom classroom, PeerVerifier verifier) {
        for (LocatedStudent student : classroom.allPlacements().values()) {
            if (!student.getClaims().isEmpty()) {
                int reciprocal = verifier.countReciprocalClaims(student);
                int total = verifier.countStudentClaims(student);
                double ratio = verifier.getReciprocityRatio(student);
                int confirmations = verifier.countConfirmations(student);

                System.out.printf("%s: %d/%d reciprocal (%.0f%%), %d external confirmations%n",
                        student.getStudent().getName(), reciprocal, total, ratio * 100, confirmations);
            }
        }
    }

    private static void printConflicts(List<AttendanceConflict> conflicts) {
        if (conflicts.isEmpty()) {
            System.out.println("✓ No conflicts detected!");
        } else {
            for (AttendanceConflict conflict : conflicts) {
                System.out.printf("⚠ %s%n", conflict);
            }
        }
    }

    private static void printAttendanceReports(List<AttendanceReport> reports) {
        for (AttendanceReport report : reports) {
            String statusIcon = switch (report.getStatus()) {
                case PRESENT -> "✓";
                case ABSENT -> "✗";
                case UNCERTAIN -> "?";
                case CONFLICTING -> "⚠";
            };

            System.out.printf("%s %s%n", statusIcon, report);
        }
    }
}