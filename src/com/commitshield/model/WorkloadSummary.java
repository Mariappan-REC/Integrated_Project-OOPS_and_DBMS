package com.commitshield.model;

import java.util.Map;

/**
 * Encapsulates the results of workload analysis.
 */
public class WorkloadSummary {

    private final double unfinishedHours;
    private final double availableHours;
    private final double deficit;
    private final boolean hasConflict;
    private final Map<TaskPriority, Double> priorityWorkload;

    public WorkloadSummary(double unfinishedHours, double availableHours, double deficit,
                           boolean hasConflict, Map<TaskPriority, Double> priorityWorkload) {
        this.unfinishedHours = unfinishedHours;
        this.availableHours = availableHours;
        this.deficit = deficit;
        this.hasConflict = hasConflict;
        this.priorityWorkload = priorityWorkload;
    }

    public double getUnfinishedHours() {
        return unfinishedHours;
    }

    public double getAvailableHours() {
        return availableHours;
    }

    public double getDeficit() {
        return deficit;
    }

    public boolean isHasConflict() {
        return hasConflict;
    }

    public Map<TaskPriority, Double> getPriorityWorkload() {
        return priorityWorkload;
    }
}
