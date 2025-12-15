package com.whoami.domain.attendance;

import com.whoami.domain.entities.Classroom;
import com.whoami.domain.entities.LocatedStudent;
import com.whoami.domain.strategies.VerificationStrategy;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates multiple verification strategies to produce a final consensus.
 * Uses weighted scoring to combine different verification methods.
 */
public class AttendanceAggregator {
  private final List<VerificationStrategy> strategies;
  private final double confidenceThresholdPresent;
  private final double confidenceThresholdAbsent;
  private final Map<String, Double> strategyWeights;
  private Classroom classroom;

  public AttendanceAggregator(List<VerificationStrategy> strategies, Map<String, Double> strategyWeights) {
    this.strategies = strategies;
    this.strategyWeights = strategyWeights;
    this.confidenceThresholdPresent = 0.65;
    this.confidenceThresholdAbsent = 0.35;
  }

  /**
   * Set the classroom context for verification strategies
   */
  public void setClassroom(Classroom classroom) {
    this.classroom = classroom;
  }

  /**
   * Generate an attendance report for all students in the classroom.
   */
  public List<AttendanceReport> generateReport(Classroom classroom) {
    this.classroom = classroom;
    Map<Long, LocatedStudent> allStudents = classroom.allPlacements().values().stream()
        .collect(Collectors.toMap(
            ls -> Long.parseLong(ls.getStudent().getId()),
            ls -> ls));

    return allStudents.values().stream()
        .map(this::scoreStudent)
        .collect(Collectors.toList());
  }

  /**
   * Score a single student using all verification strategies.
   */
  public AttendanceReport scoreStudent(LocatedStudent student) {
    double totalScore = 0.0;
    double totalWeight = 0.0;

    StringBuilder detailsBuilder = new StringBuilder();

    for (VerificationStrategy strategy : strategies) {
      double strategyScore = strategy.verify(student, classroom);

      String strategyName = strategy.getName();
      double weight = strategyWeights.getOrDefault(strategyName, 1.0);

      totalScore += strategyScore * weight;
      totalWeight += weight;

      detailsBuilder.append(strategyName).append(": ")
          .append(String.format("%.2f", strategyScore))
          .append(" | ");
    }

    double finalScore = totalWeight > 0 ? totalScore / totalWeight : 0.0;
    String details = detailsBuilder.toString();
    if (details.length() > 0) {
      details = details.substring(0, details.length() - 3); // Remove last " | "
    }

    AttendanceReport.AttendanceStatus status = determineStatus(finalScore);

    return new AttendanceReport(student, status, finalScore, details);
  }

  private AttendanceReport.AttendanceStatus determineStatus(double score) {
    if (score >= confidenceThresholdPresent) {
      return AttendanceReport.AttendanceStatus.PRESENT;
    } else if (score <= confidenceThresholdAbsent) {
      return AttendanceReport.AttendanceStatus.ABSENT;
    } else {
      return AttendanceReport.AttendanceStatus.UNCERTAIN;
    }
  }

  public double getConfidenceThresholdPresent() {
    return confidenceThresholdPresent;
  }

  public double getConfidenceThresholdAbsent() {
    return confidenceThresholdAbsent;
  }
}
