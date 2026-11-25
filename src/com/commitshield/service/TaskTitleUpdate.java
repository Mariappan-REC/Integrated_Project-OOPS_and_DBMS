package com.commitshield.service;

import com.commitshield.dao.TaskDAO;

/**
 * Child class representing a title update on a task.
 * 
 * Demonstrates:
 * 1. Inheritance - uses the 'extends' keyword to inherit state and behavior from TaskUpdate.
 * 2. Constructor Reuse - uses 'super(taskId)' to initialize the inherited parent state.
 * 3. Method Overriding - overrides execute(TaskDAO) with @Override to provide title-specific update behavior.
 * 4. Runtime Polymorphism - executed dynamically when referenced via a TaskUpdate parent reference.
 */
public class TaskTitleUpdate extends TaskUpdate {

    // Encapsulated field for the new task title
    private final String newTitle;

    /**
     * Constructor initializing the inherited taskId via super() and the new title.
     *
     * @param taskId   The ID of the task to update.
     * @param newTitle The corrected title for the task.
     */
    public TaskTitleUpdate(int taskId, String newTitle) {
        super(taskId); // Initialize task ID in parent class
        this.newTitle = newTitle;
    }

    /**
     * Getter for the new task title.
     *
     * @return The new task title.
     */
    public String getNewTitle() {
        return newTitle;
    }

    /**
     * Overrides the parent's abstract method to perform the task title update.
     * Validates that the title is not blank and does not exceed 100 characters,
     * then executes the update via TaskDAO.
     *
     * @param taskDAO The Data Access Object.
     * @return true if the title was updated in MySQL, false otherwise.
     */
    @Override
    public boolean execute(TaskDAO taskDAO) {
        if (taskDAO == null) {
            return false;
        }
        // Validate input: title cannot be empty or exceed 100 characters
        if (newTitle == null || newTitle.trim().isEmpty() || newTitle.trim().length() > 100) {
            System.out.println("Validation Error: Task title cannot be empty and must not exceed 100 characters.");
            return false;
        }
        return taskDAO.updateTaskTitle(getTaskId(), newTitle.trim());
    }
}
