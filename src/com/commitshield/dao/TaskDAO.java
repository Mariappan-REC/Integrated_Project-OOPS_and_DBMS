package com.commitshield.dao;

// Import Optional from Java's standard library.
import java.util.Optional;
// Import the Task model.
import com.commitshield.model.Task;
import com.commitshield.model.TaskPriority;
import com.commitshield.model.TaskStatus;

// Import JDBC classes for connecting to MySQL and executing SQL.
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
public class TaskDAO {

    // Establish a connection to the CommitShield database.
    Connection getConnection() throws SQLException {

        // Read database settings from environment variables.
        String url = System.getenv("DB_URL");
        String username = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        // Verify that all required settings are available.
        if (url == null || username == null || password == null) {
            throw new IllegalStateException(
                "Database environment variables are not configured."
            );
        }

        // Request a database connection using JDBC.
        return DriverManager.getConnection(url, username, password);
    }

    // Temporarily test whether Java can connect to MySQL.
    public void testConnection() {

        // Automatically close the connection after testing.
        try (Connection connection = getConnection()) {

            // Print this message only if the connection succeeds.
            System.out.println("Successfully connected to MySQL!");

        } catch (SQLException e) {

            // Display the database error if the connection fails.
            System.out.println("MySQL connection failed.");
            System.out.println("Reason: " + e.getMessage());
        }
    }

    // Save a new task in the MySQL database and return generated ID.
    public int createTask(Task task) {

        // SQL query with placeholders for the task values.
        String sql = """
                INSERT INTO tasks
                (title, estimated_hours, deadline, priority, status)
                VALUES (?, ?, ?, ?, ?)
                """;

        // Open a database connection and prepare the SQL query.
        try (
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)
        ) {

            // Set the first ? to the task's title.
            statement.setString(1, task.getTitle());

            // Set the second ? to the estimated number of hours.
            statement.setDouble(2, task.getEstimatedHours());

            // Set the third ? to the task's deadline.
            statement.setTimestamp(
                3,
                java.sql.Timestamp.valueOf(task.getDeadline())
            );

            // Set the fourth ? to the priority name, such as HIGH.
            statement.setString(4, task.getPriority().name());

            // Set the fifth ? to the status name, such as PENDING.
            statement.setString(5, task.getStatus().name());

            // Execute the INSERT query and get the number of inserted rows.
            int rowsInserted = statement.executeUpdate();

            // Confirm that the task was inserted successfully.
            if (rowsInserted > 0) {
                System.out.println("Task created successfully!");
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
                return 1;
            }

        } catch (SQLException e) {

            // Display the database error if insertion fails.
            System.out.println("Failed to create task.");
            System.out.println("Reason: " + e.getMessage());
        }
        return 0;
    }
// Retrieve all tasks stored in the MySQL database.
public List<Task> getAllTasks() {

    // Define the SQL query to retrieve every task.
    String sql = "SELECT * FROM tasks";

    // Create an empty list to hold the retrieved Task objects.
    List<Task> tasks = new ArrayList<>();

    // Open the connection, prepare the query, and execute it.
    try (
        Connection connection = getConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()
    ) {

        // Process each row returned by MySQL.
        while (resultSet.next()) {

            // Read the task ID from the current row.
            int id = resultSet.getInt("id");

            // Read the task title.
            String title = resultSet.getString("title");

            // Read the estimated hours.
            double estimatedHours =
                    resultSet.getDouble("estimated_hours");

            // Convert the SQL timestamp into LocalDateTime.
            java.time.LocalDateTime deadline =
                    resultSet.getTimestamp("deadline").toLocalDateTime();

            // Convert the stored priority string into a TaskPriority enum.
            TaskPriority priority = TaskPriority.valueOf(
                    resultSet.getString("priority")
            );

            // Convert the stored status string into a TaskStatus enum.
            TaskStatus status = TaskStatus.valueOf(
                    resultSet.getString("status")
            );

            // Create a Task object using the values from this row.
            Task task = new Task(
                    id,
                    title,
                    estimatedHours,
                    deadline,
                    priority,
                    status
            );

            // Add the Task object to the list.
            tasks.add(task);
        }

    } catch (SQLException e) {

        // Display an error if retrieving tasks fails.
        System.out.println("Failed to retrieve tasks.");
        System.out.println("Reason: " + e.getMessage());
    }

    // Return the list containing all retrieved tasks.
    return tasks;
}
// Search for a task using its ID.
public Optional<Task> getTaskById(int taskId) {

    // SQL query to find the task matching the requested ID.
    String sql = "SELECT * FROM tasks WHERE id = ?";

    // Open the database connection and prepare the query.
    try (
        Connection connection = getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)
    ) {

        // Set the first placeholder to the requested task ID.
        statement.setInt(1, taskId);

        // Execute the SELECT query and automatically close the ResultSet.
        try (ResultSet resultSet = statement.executeQuery()) {

            // Check whether MySQL returned a matching row.
            if (resultSet.next()) {

                // Read the task ID from the current database row.
                int id = resultSet.getInt("id");

                // Read the task title.
                String title = resultSet.getString("title");

                // Read the estimated hours.
                double estimatedHours =
                        resultSet.getDouble("estimated_hours");

                // Convert the database timestamp into LocalDateTime.
                java.time.LocalDateTime deadline =
                        resultSet.getTimestamp("deadline").toLocalDateTime();

                // Convert the stored priority string into an enum value.
                TaskPriority priority = TaskPriority.valueOf(
                        resultSet.getString("priority")
                );

                // Convert the stored status string into an enum value.
                TaskStatus status = TaskStatus.valueOf(
                        resultSet.getString("status")
                );

                // Construct a Task object from the database row.
                Task task = new Task(
                        id,
                        title,
                        estimatedHours,
                        deadline,
                        priority,
                        status
                );

                // Return the task because it was found.
                return Optional.of(task);
            }
        }

    } catch (SQLException e) {

        // Display the database error if the search fails.
        System.out.println("Failed to search for task.");
        System.out.println("Reason: " + e.getMessage());

        // An empty result alone cannot distinguish an SQL error from no match.
        return Optional.empty();
    }

    // Return an empty Optional when no matching task exists.
    return Optional.empty();
}
// Update the status of a task identified by its ID and record history transactionally.
public boolean updateTaskStatus(int taskId, TaskStatus newStatus) {

    String selectSql = "SELECT status FROM tasks WHERE id = ?";
    String updateSql = "UPDATE tasks SET status = ? WHERE id = ?";
    String historySql = "INSERT INTO task_history (task_id, old_status, new_status, changed_at) VALUES (?, ?, ?, NOW())";

    try (Connection connection = getConnection()) {

        // 1. Check current status
        TaskStatus currentStatus = null;
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setInt(1, taskId);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    currentStatus = TaskStatus.valueOf(rs.getString("status"));
                } else {
                    System.out.println("Task not found.");
                    return false;
                }
            }
        }

        // If the status is unchanged, skip update and history logging
        if (currentStatus == newStatus) {
            System.out.println("Task status is already " + newStatus + ". No change recorded.");
            return true;
        }

        // 2. Perform atomic transaction: update task status and insert history record
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try {
            try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                updateStmt.setString(1, newStatus.name());
                updateStmt.setInt(2, taskId);
                updateStmt.executeUpdate();
            }

            // Insert audit history record
            try (PreparedStatement historyStmt = connection.prepareStatement(historySql)) {
                historyStmt.setInt(1, taskId);
                historyStmt.setString(2, currentStatus.name());
                historyStmt.setString(3, newStatus.name());
                historyStmt.executeUpdate();
            }

            connection.commit();
            System.out.println("Task status updated successfully!");
            return true;
        } catch (SQLException e) {
            connection.rollback();
            System.out.println("Failed to update task status (transaction rolled back).");
            System.out.println("Reason: " + e.getMessage());
            return false;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }

    } catch (SQLException e) {
        System.out.println("Failed to update task status.");
        System.out.println("Reason: " + e.getMessage());
        return false;
    }
}
// Delete a task using its unique ID.
public boolean deleteTask(int taskId) {

    // SQL query to delete only the task matching the supplied ID.
    String sql = "DELETE FROM tasks WHERE id = ?";

    // Open the database connection and prepare the SQL statement.
    try (
        Connection connection = getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)
    ) {

        // Replace the first placeholder with the task ID.
        statement.setInt(1, taskId);

        // Execute the DELETE query and count deleted rows.
        int rowsDeleted = statement.executeUpdate();

        // Check whether a task was actually deleted.
        if (rowsDeleted > 0) {
            System.out.println("Task deleted successfully!");
            return true;
        } else {
            System.out.println("Task not found.");
            return false;
        }

    } catch (SQLException e) {

        // Display an error if the deletion fails.
        System.out.println("Failed to delete task.");
        System.out.println("Reason: " + e.getMessage());
        return false;
    }
}

    // =========================================================================
    // Task Editing Operations (Title and Estimated Hours)
    // =========================================================================

    /**
     * Updates only the title of an existing task in MySQL.
     * Uses a parameterized PreparedStatement to prevent SQL injection.
     * Does NOT modify priority, deadline, status, dependencies, or history.
     *
     * @param taskId   The unique ID of the task to update.
     * @param newTitle The new title (must not be empty, max 100 chars).
     * @return true if one row was updated, false if task not found or on SQL error.
     */
    public boolean updateTaskTitle(int taskId, String newTitle) {
        if (newTitle == null || newTitle.trim().isEmpty() || newTitle.trim().length() > 100) {
            System.out.println("Validation Error: Task title cannot be empty and must not exceed 100 characters.");
            return false;
        }

        String sql = "UPDATE tasks SET title = ? WHERE id = ?";

        try (
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, newTitle.trim());
            statement.setInt(2, taskId);

            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Task title updated successfully in MySQL for ID: " + taskId);
                return true;
            } else {
                System.out.println("Task not found with ID: " + taskId);
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Failed to update task title: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates only the estimated working hours of an existing task in MySQL.
     * Uses a parameterized PreparedStatement to prevent SQL injection.
     * Does NOT modify priority, deadline, status, dependencies, or history.
     *
     * @param taskId   The unique ID of the task to update.
     * @param newHours The new estimated hours (must be finite, > 0, <= 9999.99).
     * @return true if one row was updated, false if task not found or on SQL error.
     */
    public boolean updateTaskHours(int taskId, double newHours) {
        if (!Double.isFinite(newHours) || newHours <= 0 || newHours > 9999.99) {
            System.out.println("Validation Error: Estimated hours must be a positive finite number up to 9999.99.");
            return false;
        }

        String sql = "UPDATE tasks SET estimated_hours = ? WHERE id = ?";

        try (
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setDouble(1, newHours);
            statement.setInt(2, taskId);

            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Task estimated hours updated successfully in MySQL for ID: " + taskId);
                return true;
            } else {
                System.out.println("Task not found with ID: " + taskId);
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Failed to update task hours: " + e.getMessage());
            return false;
        }
    }
}