package com.commitshield.service;

// Import the Task model.
import com.commitshield.model.Task;

// Import the priority enum.
import com.commitshield.model.TaskPriority;

// Import the status enum.
import com.commitshield.model.TaskStatus;

// Import List to process multiple tasks.
import java.util.List;

// Import EnumMap to store hours for each priority.
import java.util.EnumMap;

// Import Map to represent priority-wise workload.
import java.util.Map;

public class ConflictDetector {

    // Calculate the total hours of unfinished tasks.
    public double calculateUnfinishedHours(List<Task> tasks) {

        // Start the total at zero.
        double totalHours = 0;

        // Examine every task in the list.
        for (Task task : tasks) {

            // Ignore tasks that are already completed.
            if (task.getStatus() != TaskStatus.COMPLETED) {

                // Add this task's estimated hours.
                totalHours += task.getEstimatedHours();
            }
        }

        // Return the total unfinished workload.
        return totalHours;
    }

    // Calculate the workload deficit.
    public double calculateDeficit(
            double unfinishedHours,
            double availableHours) {

        // Calculate the difference between workload and availability.
        double difference = unfinishedHours - availableHours;

        // Never return a negative deficit.
        return Math.max(0, difference);
    }

    // Calculate unfinished workload separately for each priority.
    public Map<TaskPriority, Double> calculatePriorityWiseWorkload(
            List<Task> tasks) {

        // Create a map containing every priority, initially set to zero.
        Map<TaskPriority, Double> workload =
                new EnumMap<>(TaskPriority.class);

        // Initialize the workload for all four priorities.
        for (TaskPriority priority : TaskPriority.values()) {
            workload.put(priority, 0.0);
        }

        // Examine every task.
        for (Task task : tasks) {

            // Exclude completed tasks from the workload.
            if (task.getStatus() != TaskStatus.COMPLETED) {

                // Get the priority of the current task.
                TaskPriority priority = task.getPriority();

                // Get the current workload for that priority.
                double currentHours = workload.get(priority);

                // Add the current task's estimated hours.
                double updatedHours =
                        currentHours + task.getEstimatedHours();

                // Store the updated workload for this priority.
                workload.put(priority, updatedHours);
            }
        }

        // Return the completed priority-wise workload map.
        return workload;
    }
}