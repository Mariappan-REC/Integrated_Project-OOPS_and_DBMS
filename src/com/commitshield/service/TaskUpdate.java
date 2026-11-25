package com.commitshield.service;

import com.commitshield.dao.TaskDAO;

/**
 * Abstract parent class representing an update operation on an existing task.
 * 
 * Demonstrates:
 * 1. Abstraction - declares the common abstract execute() method that must be implemented by subclasses.
 * 2. Encapsulation - hides taskId behind a private field accessible only via getter and constructor.
 */
public abstract class TaskUpdate {

    // Encapsulated task ID field (private, accessible only via getter)
    private final int taskId;

    /**
     * Constructor to initialize the encapsulated task ID.
     * Subclasses invoke this using super(taskId).
     *
     * @param taskId The ID of the task to be updated.
     */
    protected TaskUpdate(int taskId) {
        this.taskId = taskId;
    }

    /**
     * Getter method providing controlled access to the encapsulated task ID.
     *
     * @return The task ID.
     */
    public int getTaskId() {
        return taskId;
    }

    /**
     * Abstract method representing the execution of a task update.
     * 
     * Subclasses (TaskTitleUpdate, TaskHoursUpdate) must override this method
     * to provide their specific database update behavior, demonstrating Runtime Polymorphism.
     *
     * @param taskDAO The Data Access Object used to execute the database update.
     * @return true if the update was successful, false otherwise.
     */
    public abstract boolean execute(TaskDAO taskDAO);
}
