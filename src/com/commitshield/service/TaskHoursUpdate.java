package com.commitshield.service;

import com.commitshield.dao.TaskDAO;

/**
 * Child class representing an estimated working hours update on a task.
 * 
 * Demonstrates:
 * 1. Inheritance - uses the 'extends' keyword to inherit state and behavior from TaskUpdate.
 * 2. Constructor Reuse - uses 'super(taskId)' to initialize the inherited parent state.
 * 3. Method Overriding - overrides execute(TaskDAO) with @Override to provide hours-specific update behavior.
 * 4. Runtime Polymorphism - executed dynamically when referenced via a TaskUpdate parent reference.
 */
public class TaskHoursUpdate extends TaskUpdate {

    // Encapsulated field for the new estimated hours
    private final double newHours;

    /**
     * Constructor initializing the inherited taskId via super() and the new estimated hours.
     *
     * @param taskId   The ID of the task to update.
     * @param newHours The updated estimated working hours.
     */
    public TaskHoursUpdate(int taskId, double newHours) {
        super(taskId); // Initialize task ID in parent class
        this.newHours = newHours;
    }

    /**
     * Getter for the new estimated hours.
     *
     * @return The new estimated hours.
     */
    public double getNewHours() {
        return newHours;
    }

    /**
     * Overrides the parent's abstract method to perform the estimated hours update.
     * Validates that the hours value is finite, greater than zero, and within the
     * database range (DECIMAL(6,2) <= 9999.99), then executes the update via TaskDAO.
     *
     * @param taskDAO The Data Access Object.
     * @return true if the hours were updated in MySQL, false otherwise.
     */
    @Override
    public boolean execute(TaskDAO taskDAO) {
        if (taskDAO == null) {
            return false;
        }
        // Validate input: hours must be finite, strictly positive, and <= 9999.99
        if (!Double.isFinite(newHours) || newHours <= 0 || newHours > 9999.99) {
            System.out.println("Validation Error: Estimated hours must be a positive finite number up to 9999.99.");
            return false;
        }
        return taskDAO.updateTaskHours(getTaskId(), newHours);
    }
}
