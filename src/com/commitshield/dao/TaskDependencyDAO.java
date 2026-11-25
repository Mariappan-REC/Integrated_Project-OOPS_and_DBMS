package com.commitshield.dao;

import com.commitshield.model.TaskDependency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing task dependencies in MySQL.
 */
public class TaskDependencyDAO {

    private final TaskDAO taskDAO;

    public TaskDependencyDAO(TaskDAO taskDAO) {
        this.taskDAO = taskDAO;
    }

    public TaskDependencyDAO() {
        this(new TaskDAO());
    }

    /**
     * Adds a dependency: taskId depends on dependsOnTaskId.
     * Prevents self-dependency and duplicate pairs.
     * @return true if successfully inserted, false otherwise.
     */
    public boolean addDependency(int taskId, int dependsOnTaskId) {
        if (taskId == dependsOnTaskId) {
            System.out.println("Validation Error: A task cannot depend on itself.");
            return false;
        }

        String sql = """
                INSERT INTO task_dependencies (task_id, depends_on_task_id)
                VALUES (?, ?)
                """;

        try (Connection connection = taskDAO.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, taskId);
            stmt.setInt(2, dependsOnTaskId);

            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Failed to add dependency (Task " + taskId + " -> " + dependsOnTaskId + "): " + e.getMessage());
            return false;
        } catch (IllegalStateException e) {
            System.out.println("Database configuration error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Removes a dependency link between two tasks.
     * @return true if a dependency was deleted, false if not found.
     */
    public boolean removeDependency(int taskId, int dependsOnTaskId) {
        String sql = "DELETE FROM task_dependencies WHERE task_id = ? AND depends_on_task_id = ?";

        try (Connection connection = taskDAO.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, taskId);
            stmt.setInt(2, dependsOnTaskId);

            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException | IllegalStateException e) {
            System.out.println("Failed to remove dependency: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves all dependencies with task titles for frontend display.
     */
    public List<TaskDependency> getAllDependencies() {
        String sql = """
                SELECT td.task_id, t1.title AS task_title,
                       td.depends_on_task_id, t2.title AS depends_on_title,
                       td.created_at
                FROM task_dependencies td
                JOIN tasks t1 ON td.task_id = t1.id
                JOIN tasks t2 ON td.depends_on_task_id = t2.id
                ORDER BY td.created_at DESC
                """;

        List<TaskDependency> dependencies = new ArrayList<>();

        try (Connection connection = taskDAO.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                dependencies.add(new TaskDependency(
                        rs.getInt("task_id"),
                        rs.getString("task_title"),
                        rs.getInt("depends_on_task_id"),
                        rs.getString("depends_on_title"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException | IllegalStateException e) {
            System.out.println("Failed to retrieve dependencies: " + e.getMessage());
        }

        return dependencies;
    }

    /**
     * Retrieves prerequisite tasks that a given task depends upon.
     */
    public List<TaskDependency> getDependenciesForTask(int taskId) {
        String sql = """
                SELECT td.task_id, t1.title AS task_title,
                       td.depends_on_task_id, t2.title AS depends_on_title,
                       td.created_at
                FROM task_dependencies td
                JOIN tasks t1 ON td.task_id = t1.id
                JOIN tasks t2 ON td.depends_on_task_id = t2.id
                WHERE td.task_id = ?
                ORDER BY td.created_at DESC
                """;

        List<TaskDependency> dependencies = new ArrayList<>();

        try (Connection connection = taskDAO.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, taskId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    dependencies.add(new TaskDependency(
                            rs.getInt("task_id"),
                            rs.getString("task_title"),
                            rs.getInt("depends_on_task_id"),
                            rs.getString("depends_on_title"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException | IllegalStateException e) {
            System.out.println("Failed to retrieve dependencies for task " + taskId + ": " + e.getMessage());
        }

        return dependencies;
    }
}
