
package com.commitshield.service;

// Import Comparator to sort tasks by priority and deadline.
import java.util.Comparator;

// Import the DAO and model classes.
import com.commitshield.dao.TaskDAO;
import com.commitshield.dao.TaskDependencyDAO;
import com.commitshield.dao.TaskHistoryDAO;
import com.commitshield.model.Task;
import com.commitshield.model.TaskDependency;
import com.commitshield.model.TaskHistory;
import com.commitshield.model.TaskPriority;
import com.commitshield.model.TaskStatus;
import com.commitshield.model.WorkloadSummary;

// Import Java collection classes.
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TaskService {

    // Create the objects used for database and workload operations.
    private final TaskDAO taskDAO = new TaskDAO();
    private final TaskDependencyDAO dependencyDAO = new TaskDependencyDAO(taskDAO);
    private final TaskHistoryDAO historyDAO = new TaskHistoryDAO(taskDAO);
    private final ConflictDetector conflictDetector = new ConflictDetector();

    // Create a task in the database and return its ID.
    public int createTask(Task task) {
        return taskDAO.createTask(task);
    }

    // Retrieve all tasks.
    public List<Task> getAllTasks() {
        return taskDAO.getAllTasks();
    }

    // Find a task by its ID.
    public Optional<Task> getTaskById(int taskId) {
        return taskDAO.getTaskById(taskId);
    }

    // Update a task's status.
    public boolean updateTaskStatus(int taskId, TaskStatus newStatus) {
        return taskDAO.updateTaskStatus(taskId, newStatus);
    }

    // Delete a task by its ID.
    public boolean deleteTask(int taskId) {
        return taskDAO.deleteTask(taskId);
    }

    // Return structured workload summary for API / Web Frontend.
    public WorkloadSummary getWorkloadSummary(double availableHours) {
        if (!Double.isFinite(availableHours) || availableHours < 0) {
            availableHours = 0;
        }

        List<Task> tasks = taskDAO.getAllTasks();
        double unfinishedHours = conflictDetector.calculateUnfinishedHours(tasks);
        double deficit = conflictDetector.calculateDeficit(unfinishedHours, availableHours);
        Map<TaskPriority, Double> priorityWorkload = conflictDetector.calculatePriorityWiseWorkload(tasks);
        boolean hasConflict = deficit > 0;

        return new WorkloadSummary(unfinishedHours, availableHours, deficit, hasConflict, priorityWorkload);
    }

    // Analyze unfinished workload and display the priority breakdown in console.
    public void analyzeWorkload(double availableHours) {

        // Reject invalid available-hour values.
        if (!Double.isFinite(availableHours) || availableHours < 0) {
            System.out.println("Available hours must be a valid non-negative number.");
            return;
        }

        // Retrieve tasks from MySQL.
        List<Task> tasks = taskDAO.getAllTasks();

        // Calculate the total unfinished workload.
        double unfinishedHours =
                conflictDetector.calculateUnfinishedHours(tasks);

        // Calculate the workload deficit.
        double deficit = conflictDetector.calculateDeficit(
                unfinishedHours, availableHours
        );

        // Calculate unfinished hours for each priority.
        Map<TaskPriority, Double> priorityWorkload =
                conflictDetector.calculatePriorityWiseWorkload(tasks);

        // Display priorities from highest to lowest.
        TaskPriority[] priorities = {
            TaskPriority.CRITICAL,
            TaskPriority.HIGH,
            TaskPriority.MEDIUM,
            TaskPriority.LOW
        };

        System.out.println("\n========== COMMITSHIELD WORKLOAD ANALYSIS ==========");

        // Print the workload for each priority.
        for (TaskPriority priority : priorities) {
            System.out.printf(
                    "%-8s : %.2f hours%n",
                    priority,
                    priorityWorkload.get(priority)
            );
        }

        // Display the workload summary.
        System.out.printf("%nTotal unfinished workload : %.2f hours%n",
                unfinishedHours);
        System.out.printf("Available hours            : %.2f hours%n",
                availableHours);
        System.out.printf("Workload deficit           : %.2f hours%n",
                deficit);

        // Report whether the workload exceeds available time.
        if (deficit > 0) {
            System.out.println("Result: Workload conflict detected!");
        } else {
            System.out.println("Result: No workload conflict.");
        }

        System.out.println("====================================================");
    }

    // Retrieve unfinished tasks in priority order.
    public List<Task> getUnfinishedTasksByPriority() {

        // Retrieve all tasks from the database.
        List<Task> tasks = taskDAO.getAllTasks();

        // Remove completed tasks because they no longer need to be scheduled.
        tasks.removeIf(task -> task.getStatus() == TaskStatus.COMPLETED);

        // Sort priorities from CRITICAL to LOW.
        // For tasks with equal priority, show the earliest deadline first.
        tasks.sort(
            Comparator.comparing(Task::getPriority)
                      .reversed()
                      .thenComparing(Task::getDeadline)
        );

        // Return the sorted list of unfinished tasks.
        return tasks;
    }

    // Task dependency methods
    public boolean addDependency(int taskId, int dependsOnTaskId) {
        return dependencyDAO.addDependency(taskId, dependsOnTaskId);
    }

    public boolean removeDependency(int taskId, int dependsOnTaskId) {
        return dependencyDAO.removeDependency(taskId, dependsOnTaskId);
    }

    public List<TaskDependency> getAllDependencies() {
        return dependencyDAO.getAllDependencies();
    }

    public List<TaskDependency> getDependenciesForTask(int taskId) {
        return dependencyDAO.getDependenciesForTask(taskId);
    }

    // Task history methods
    public List<TaskHistory> getHistoryByTaskId(int taskId) {
        return historyDAO.getHistoryByTaskId(taskId);
    }

    public List<TaskHistory> getAllHistory() {
        return historyDAO.getAllHistory();
    }

    // =========================================================================
    // Core OOP Demonstration: Compile-Time & Runtime Polymorphism
    // =========================================================================

    /**
     * Common execution method demonstrating Runtime Polymorphism (Dynamic Method Dispatch).
     * Accepts a parent-type reference (TaskUpdate) which dynamically calls the overridden
     * execute() method of whichever child class (TaskTitleUpdate or TaskHoursUpdate)
     * was instantiated and passed at runtime.
     *
     * @param update Polymorphic reference to the abstract TaskUpdate parent type.
     * @return true if the update succeeded in MySQL, false otherwise.
     */
    public boolean executeUpdate(TaskUpdate update) {
        if (update == null || update.getTaskId() <= 0) {
            return false;
        }
        // Runtime Polymorphism: The JVM dynamically dispatches to the overridden execute()
        // method of the actual child class (TaskTitleUpdate or TaskHoursUpdate).
        return update.execute(taskDAO);
    }

    /**
     * Overloaded method 1: Update task title.
     * Demonstrates Compile-Time Polymorphism (Method Overloading) by parameter type (String).
     * Instantiates the TaskTitleUpdate child class and passes it to executeUpdate().
     *
     * @param taskId   The unique ID of the task.
     * @param newTitle The new corrected title.
     * @return true if updated in MySQL, false otherwise.
     */
    public boolean updateTask(int taskId, String newTitle) {
        // Compile-time binding binds this call to updateTask(int, String).
        // It then demonstrates runtime polymorphism via the TaskUpdate parent reference.
        TaskUpdate update = new TaskTitleUpdate(taskId, newTitle);
        return executeUpdate(update);
    }

    /**
     * Overloaded method 2: Update task estimated working hours.
     * Demonstrates Compile-Time Polymorphism (Method Overloading) by parameter type (double).
     * Instantiates the TaskHoursUpdate child class and passes it to executeUpdate().
     *
     * @param taskId   The unique ID of the task.
     * @param newHours The updated estimated working hours.
     * @return true if updated in MySQL, false otherwise.
     */
    public boolean updateTask(int taskId, double newHours) {
        // Compile-time binding binds this call to updateTask(int, double).
        // It then demonstrates runtime polymorphism via the TaskUpdate parent reference.
        TaskUpdate update = new TaskHoursUpdate(taskId, newHours);
        return executeUpdate(update);
    }
}
