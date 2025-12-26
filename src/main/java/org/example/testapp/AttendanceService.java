package org.example.testapp;

import org.example.testapp.attendance.AttendanceAggregator;
import org.example.testapp.attendance.AttendanceReport;
import org.example.testapp.entities.Claim;
import org.example.testapp.entities.Classroom;
import org.example.testapp.entities.LocatedStudent;
import org.example.testapp.strategies.ConsensusScoreStrategy;
import org.example.testapp.strategies.NeighborVerificationStrategy;
import org.example.testapp.strategies.SeatOccupancyStrategy;
import org.example.testapp.strategies.VerificationStrategy;
import org.example.testapp.verification.AttendanceConflict;
import org.example.testapp.verification.ConflictDetector;

import java.util.*;

/**
 * Service class that bridges the UI layer with the domain verification logic.
 * Orchestrates the attendance verification workflow.
 */
public class AttendanceService {
  private AttendanceAggregator aggregator;
  private ConflictDetector conflictDetector;
  private Classroom classroom;
  private Collection<LocatedStudent> locatedStudents;
  private List<Claim> claims;
  private AttendanceAnalysisResult lastResult; // Store last analysis result
  private Map<String, AttendanceReport.AttendanceStatus> manualOverrides = new HashMap<>();

  public AttendanceService() {
    // Initialize with default strategies and weights
    List<VerificationStrategy> strategies = new ArrayList<>();
    strategies.add(new NeighborVerificationStrategy());
    strategies.add(new SeatOccupancyStrategy());
    strategies.add(new ConsensusScoreStrategy());

    Map<String, Double> weights = new HashMap<>();
    weights.put("NeighborVerification", 0.4);
    weights.put("SeatOccupancy", 0.35);
    weights.put("ConsensusScore", 0.25);

    this.aggregator = new AttendanceAggregator(strategies, weights);
  }

  /**
   * Set the classroom for analysis.
   */
  public void setClassroom(Classroom classroom) {
    this.classroom = classroom;
    this.aggregator.setClassroom(classroom);
    this.conflictDetector = new ConflictDetector(classroom);
  }

  /**
   * Set students participating in the analysis.
   */
  public void setLocatedStudents(Collection<LocatedStudent> locatedStudents) {
    this.locatedStudents = locatedStudents;
  }

  /**
   * Set claims about student locations.
   */
  public void setClaims(List<Claim> claims) {
    this.claims = claims;
  }

  /**
   * Set manual attendance overrides keyed by student name.
   */
  public void setManualOverrides(Map<String, AttendanceReport.AttendanceStatus> overrides) {
    this.manualOverrides = overrides != null ? new HashMap<>(overrides) : new HashMap<>();
  }

  /**
   * Update strategy weights for analysis.
   */
  public void setStrategyWeights(double neighborWeight, double occupancyWeight, double consensusWeight) {
    // Normalize weights
    double total = neighborWeight + occupancyWeight + consensusWeight;
    Map<String, Double> weights = new HashMap<>();
    weights.put("NeighborVerification", neighborWeight / total);
    weights.put("SeatOccupancy", occupancyWeight / total);
    weights.put("ConsensusScore", consensusWeight / total);

    // Recreate aggregator with new weights
    List<VerificationStrategy> strategies = new ArrayList<>();
    strategies.add(new NeighborVerificationStrategy());
    strategies.add(new SeatOccupancyStrategy());
    strategies.add(new ConsensusScoreStrategy());

    this.aggregator = new AttendanceAggregator(strategies, weights);
    if (classroom != null) {
      aggregator.setClassroom(classroom);
    }
  }

  /**
   * Analyze attendance based on current configuration.
   */
  public AttendanceAnalysisResult analyzeAttendance() {
    if (classroom == null || locatedStudents == null) {
      throw new IllegalStateException("Classroom and students must be set before analysis");
    }

    // Score all students
    List<AttendanceReport> reports = new ArrayList<>();
    for (LocatedStudent student : locatedStudents) {
      reports.add(aggregator.scoreStudent(student));
    }

    // Apply manual overrides if provided
    if (!manualOverrides.isEmpty()) {
      for (int i = 0; i < reports.size(); i++) {
        AttendanceReport report = reports.get(i);
        String name = report.getStudent().getName();
        if (manualOverrides.containsKey(name)) {
          AttendanceReport.AttendanceStatus override = manualOverrides.get(name);
          reports.set(i, new AttendanceReport(report.getLocatedStudent(), override, 1.0,
              "Manual override: " + override));
        }
      }
    }

    // Detect conflicts
    List<AttendanceConflict> conflicts = conflictDetector != null ? conflictDetector.detectAllConflicts()
        : new ArrayList<>();

    // Store result for later retrieval
    lastResult = new AttendanceAnalysisResult(reports, conflicts);
    return lastResult;
  }

  /**
   * Get the last analysis result without re-analyzing.
   */
  public AttendanceAnalysisResult getLastResult() {
    return lastResult;
  }

  /**
   * Container for attendance analysis results.
   */
  public static class AttendanceAnalysisResult {
    public final List<AttendanceReport> reports;
    public final List<AttendanceConflict> conflicts;

    public AttendanceAnalysisResult(
        List<AttendanceReport> reports,
        List<AttendanceConflict> conflicts) {
      this.reports = reports;
      this.conflicts = conflicts;
    }
  }
}
