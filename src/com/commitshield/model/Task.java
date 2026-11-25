package com.commitshield.model;

import java.time.LocalDateTime;

public class Task {

    private int id;
    private String title;
    private double estimatedHours;
    private LocalDateTime deadline;
    private TaskPriority priority;
    private TaskStatus status;
    //constructor
    public Task(
        int id,
        String title,
        double estimatedHours,
        LocalDateTime deadline,
        TaskPriority priority,
        TaskStatus status) {

    this.id = id;
    this.title = title;
    this.estimatedHours = estimatedHours;
    this.deadline = deadline;
    this.priority = priority;
    this.status = status;
}   
    // Returns the task's unique ID.
public int getId() {
    return id;
}

// Returns the task's title.
public String getTitle() {
    return title;
}

// Returns the estimated number of hours needed to complete the task.
public double getEstimatedHours() {
    return estimatedHours;
}

// Returns the task's deadline.
public LocalDateTime getDeadline() {
    return deadline;
}

// Returns the task's priority.
public TaskPriority getPriority() {
    return priority;
}

// Returns the task's current status.
public TaskStatus getStatus() {
    return status;
}
// Updates the task's priority.
public void setPriority(TaskPriority priority) {
    this.priority = priority;
}

// Updates the task's status.
public void setStatus(TaskStatus status) {
    this.status = status;
}
}