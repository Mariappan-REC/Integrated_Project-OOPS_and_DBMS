
package com.commitshield;

// Import the service and model classes.
import com.commitshield.service.TaskService;
import com.commitshield.model.Task;
import com.commitshield.model.TaskPriority;
import com.commitshield.model.TaskStatus;

// Import Java utilities for input, dates, and collections.
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Main {

    // Use one Scanner for all console input.
    private static final Scanner scanner = new Scanner(System.in);

    // Create the service object used by the menu.
    private static final TaskService taskService = new TaskService();

    // Define the date format accepted by the application.
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void main(String[] args) {

        // Continue displaying the menu until the user chooses Exit.
        boolean running = true;

        while (running) {
            System.out.println("\n========== COMMITSHIELD ==========");
            System.out.println("1. Create Task");
            System.out.println("2. View All Tasks");
            System.out.println("3. Search Task by ID");
            System.out.println("4. Update Task Status");
            System.out.println("5. Delete Task");
            System.out.println("6. Analyze Workload");
            System.out.println("7. View Unfinished Tasks by Priority");
            System.out.println("8. Exit");
            System.out.println("9. Edit Task (Title / Hours)");
            System.out.println("==================================");

            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine().trim();

            // Execute the operation selected by the user.
            switch (choice) {
                case "1":
                    createTask();
                    break;

                case "2":
                    viewAllTasks();
                    break;

                case "3":
                    searchTask();
                    break;

                case "4":
                    updateTaskStatus();
                    break;

                case "5":
                    deleteTask();
                    break;

                case "6":
                    analyzeWorkload();
                    break;

                case "7":
                    viewUnfinishedTasksByPriority();
                    break;

                case "8":
                    running = false;
                    System.out.println("Thank you for using CommitShield!");
                    break;

                case "9":
                    editTask();
                    break;

                default:
                    System.out.println("Invalid choice. Enter a number from 1 to 9.");
            }
        }

        // Close console input when the application exits.
        scanner.close();
    }

    // Option 1: Create and save a new task.
    private static void createTask() {
        System.out.println("\n--- Create Task ---");

        System.out.print("Enter task title: ");
        String title = scanner.nextLine().trim();

        // Reject an empty title.
        if (title.isEmpty()) {
            System.out.println("Task title cannot be empty.");
            return;
        }

        // Read and validate the estimated time.
        double hours = readPositiveDouble("Enter estimated hours: ");

        // Read the deadline in yyyy-MM-dd HH:mm format.
        LocalDateTime deadline = readDeadline();

        // Read the task priority.
        TaskPriority priority = readPriority();

        // New tasks begin in PENDING status.
        // The database generates the actual task ID.
        Task task = new Task(
                0, title, hours, deadline, priority, TaskStatus.PENDING
        );

        // Save the task through the existing service and DAO.
        taskService.createTask(task);
    }

    // Option 2: Display every task.
    private static void viewAllTasks() {
        System.out.println("\n--- All Tasks ---");

        List<Task> tasks = taskService.getAllTasks();

        // Handle an empty result.
        if (tasks.isEmpty()) {
            System.out.println("No tasks found.");
            return;
        }

        // Display each task.
        for (Task task : tasks) {
            printTask(task);
        }
    }

    // Option 3: Find and display one task by ID.
    private static void searchTask() {
        System.out.println("\n--- Search Task ---");

        int id = readPositiveInt("Enter task ID: ");

        Optional<Task> result = taskService.getTaskById(id);

        // Optional contains a task if one was found.
        if (result.isPresent()) {
            printTask(result.get());
        } else {
            System.out.println("Task not found, or the search failed.");
        }
    }

    // Option 4: Change a task's status.
    private static void updateTaskStatus() {
        System.out.println("\n--- Update Task Status ---");

        int id = readPositiveInt("Enter task ID: ");
        TaskStatus status = readStatus();

        // The service sends the update to the database.
        taskService.updateTaskStatus(id, status);
    }

    // Option 5: Delete a task.
    private static void deleteTask() {
        System.out.println("\n--- Delete Task ---");

        int id = readPositiveInt("Enter task ID to delete: ");

        System.out.print("Are you sure you want to delete it? (yes/no): ");
        String confirmation = scanner.nextLine().trim();

        // Delete only after explicit confirmation.
        if (confirmation.equalsIgnoreCase("yes")) {
            taskService.deleteTask(id);
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    // Option 6: Calculate workload and available-time deficit.
    private static void analyzeWorkload() {
        System.out.println("\n--- Workload Analysis ---");

        double availableHours =
                readNonNegativeDouble("Enter available hours today: ");

        taskService.analyzeWorkload(availableHours);
    }

    // Option 7: Show unfinished tasks by priority and deadline.
    private static void viewUnfinishedTasksByPriority() {
        System.out.println("\n--- Unfinished Tasks by Priority ---");

        List<Task> tasks = taskService.getUnfinishedTasksByPriority();

        if (tasks.isEmpty()) {
            System.out.println("No unfinished tasks found.");
            return;
        }

        // TaskService has already sorted the tasks.
        for (Task task : tasks) {
            printTask(task);
        }
    }

    // Display all fields of a task in a consistent format.
    private static void printTask(Task task) {
        System.out.println("\n----------------------------------");
        System.out.println("ID: " + task.getId());
        System.out.println("Title: " + task.getTitle());
        System.out.printf("Estimated hours: %.2f%n",
                task.getEstimatedHours());
        System.out.println("Deadline: " + task.getDeadline());
        System.out.println("Priority: " + task.getPriority());
        System.out.println("Status: " + task.getStatus());
        System.out.println("----------------------------------");
    }

    // Read an integer greater than zero.
    private static int readPositiveInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                int value = Integer.parseInt(input);

                if (value > 0) {
                    return value;
                }

                System.out.println("Enter a number greater than zero.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Enter a whole number.");
            }
        }
    }

    // Read a finite number greater than zero.
    private static double readPositiveDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                double value = Double.parseDouble(input);

                if (Double.isFinite(value) && value > 0) {
                    return value;
                }

                System.out.println("Enter a valid number greater than zero.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Try again.");
            }
        }
    }

    // Read a finite number that can be zero but not negative.
    private static double readNonNegativeDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                double value = Double.parseDouble(input);

                if (Double.isFinite(value) && value >= 0) {
                    return value;
                }

                System.out.println("Enter a valid non-negative number.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Try again.");
            }
        }
    }

    // Read a deadline and retry if the date format is invalid.
    private static LocalDateTime readDeadline() {
        while (true) {
            System.out.print("Enter deadline (yyyy-MM-dd HH:mm): ");
            String input = scanner.nextLine().trim();

            try {
                return LocalDateTime.parse(input, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println(
                        "Invalid date. Example: 2026-11-05 18:30"
                );
            }
        }
    }

    // Read a valid priority from the priority enum.
    private static TaskPriority readPriority() {
        while (true) {
            System.out.print("Enter priority (LOW, MEDIUM, HIGH, CRITICAL): ");
            String input = scanner.nextLine().trim().toUpperCase();

            try {
                return TaskPriority.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid priority. Try again.");
            }
        }
    }

    // Read a valid status; spaces are accepted in IN PROGRESS.
    private static TaskStatus readStatus() {
        while (true) {
            System.out.print(
                    "Enter status (PENDING, IN_PROGRESS, COMPLETED): "
            );

            String input = scanner.nextLine().trim()
                    .toUpperCase()
                    .replace(' ', '_');

            try {
                return TaskStatus.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid status. Try again.");
            }
        }
    }

    // Option 9: Edit a task's title or estimated working hours.
    // Demonstrates both Compile-Time and Runtime Polymorphism through TaskService.
    private static void editTask() {
        System.out.println("\n--- Edit Task ---");
        int id = readPositiveInt("Enter task ID to edit: ");

        Optional<Task> taskOpt = taskService.getTaskById(id);
        if (taskOpt.isEmpty()) {
            System.out.println("Task not found with ID: " + id);
            return;
        }

        Task task = taskOpt.get();
        System.out.println("\nSelected Task:");
        System.out.println("ID: " + task.getId());
        System.out.println("Current Title: " + task.getTitle());
        System.out.printf("Current Estimated Hours: %.2f%n", task.getEstimatedHours());

        System.out.println("\nEdit Submenu:");
        System.out.println("1. Change Task Title");
        System.out.println("2. Change Estimated Hours");
        System.out.println("3. Return to Main Menu");
        System.out.print("Enter your choice: ");
        String subChoice = scanner.nextLine().trim();

        switch (subChoice) {
            case "1":
                System.out.print("Enter new task title: ");
                String newTitle = scanner.nextLine().trim();
                if (newTitle.isEmpty()) {
                    System.out.println("Task title cannot be empty.");
                    return;
                }
                if (newTitle.length() > 100) {
                    System.out.println("Task title must not exceed 100 characters.");
                    return;
                }
                // Calls overloaded updateTask(int, String) -> Compile-Time & Runtime Polymorphism!
                boolean titleUpdated = taskService.updateTask(id, newTitle);
                if (titleUpdated) {
                    System.out.println("Task title updated successfully!");
                } else {
                    System.out.println("Failed to update task title.");
                }
                break;

            case "2":
                double newHours = readPositiveDouble("Enter new estimated hours: ");
                if (newHours > 9999.99) {
                    System.out.println("Estimated hours cannot exceed 9999.99.");
                    return;
                }
                // Calls overloaded updateTask(int, double) -> Compile-Time & Runtime Polymorphism!
                boolean hoursUpdated = taskService.updateTask(id, newHours);
                if (hoursUpdated) {
                    System.out.println("Task estimated hours updated successfully!");
                } else {
                    System.out.println("Failed to update task hours.");
                }
                break;

            case "3":
                System.out.println("Returning to main menu.");
                break;

            default:
                System.out.println("Invalid choice. Returning to main menu.");
        }
    }
}