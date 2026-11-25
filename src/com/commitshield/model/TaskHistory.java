package com.commitshield.model;

import java.time.LocalDateTime;

/**
 * Model representing a recorded task status change (audit trail).
 */
public class TaskHistory {

    private final int historyId;
    private final int taskId;
    private final String taskTitle;
    private final TaskStatus oldStatus;
    private final TaskStatus newStatus;
    private final LocalDateTime changedAt;

    public TaskHistory(int historyId, int taskId, String taskTitle, TaskStatus oldStatus, TaskStatus newStatus, LocalDateTime changedAt) {
        this.historyId = historyId;
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedAt = changedAt;
    }

    public int getHistoryId() {
        return historyId;
    }

    public int getTaskId() {
        return taskId;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public TaskStatus getOldStatus() {
        return oldStatus;
    }

    public TaskStatus getNewStatus() {
        return newStatus;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
