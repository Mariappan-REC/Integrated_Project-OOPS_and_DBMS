package com.commitshield.model;

import java.time.LocalDateTime;

/**
 * Model representing a dependency between two tasks.
 * E.g., taskId depends on dependsOnTaskId.
 */
public class TaskDependency {

    private final int taskId;
    private final String taskTitle;
    private final int dependsOnTaskId;
    private final String dependsOnTitle;
    private final LocalDateTime createdAt;

    public TaskDependency(int taskId, String taskTitle, int dependsOnTaskId, String dependsOnTitle, LocalDateTime createdAt) {
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.dependsOnTaskId = dependsOnTaskId;
        this.dependsOnTitle = dependsOnTitle;
        this.createdAt = createdAt;
    }

    public TaskDependency(int taskId, int dependsOnTaskId) {
        this(taskId, "", dependsOnTaskId, "", LocalDateTime.now());
    }

    public int getTaskId() {
        return taskId;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public int getDependsOnTaskId() {
        return dependsOnTaskId;
    }

    public String getDependsOnTitle() {
        return dependsOnTitle;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
