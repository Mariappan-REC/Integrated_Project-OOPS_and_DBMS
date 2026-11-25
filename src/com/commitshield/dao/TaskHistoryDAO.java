package com.commitshield.dao;

import com.commitshield.model.TaskHistory;
import com.commitshield.model.TaskStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing task status change history in MySQL.
 */
public class TaskHistoryDAO {

    private final TaskDAO taskDAO;

    public TaskHistoryDAO(TaskDAO taskDAO) {
        this.taskDAO = taskDAO;
    }

    public TaskHistoryDAO() {
        this(new TaskDAO());
    }

    /**
     * Records a task status change within an existing transaction.
     */
    public boolean recordHistory(Connection connection, int taskId, TaskStatus oldStatus, TaskStatus newStatus) throws SQLException {
        if (oldStatus == newStatus) {
            return false; // Do not record if status did not actually change
        }

        String sql = """
                INSERT INTO task_history (task_id, old_status, new_status, changed_at)
                VALUES (?, ?, ?, NOW())
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            stmt.setString(2, oldStatus.name());
            stmt.setString(3, newStatus.name());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Records a status transition using a new connection.
     */
    public boolean recordHistory(int taskId, TaskStatus oldStatus, TaskStatus newStatus) {
        if (oldStatus == newStatus) {
            return false;
        }

        try (Connection connection = taskDAO.getConnection()) {
            return recordHistory(connection, taskId, oldStatus, newStatus);
        } catch (SQLException | IllegalStateException e) {
            System.out.println("Failed to record task history: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves status change history for a specific task.
     */
    public List<TaskHistory> getHistoryByTaskId(int taskId) {
        String sql = """
                SELECT th.history_id, th.task_id, t.title AS task_title,
                       th.old_status, th.new_status, th.changed_at
                FROM task_history th
                JOIN tasks t ON th.task_id = t.id
                WHERE th.task_id = ?
                ORDER BY th.changed_at DESC
                """;

        List<TaskHistory> historyList = new ArrayList<>();

        try (Connection connection = taskDAO.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, taskId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    historyList.add(new TaskHistory(
                            rs.getInt("history_id"),
                            rs.getInt("task_id"),
                            rs.getString("task_title"),
                            TaskStatus.valueOf(rs.getString("old_status")),
                            TaskStatus.valueOf(rs.getString("new_status")),
                            rs.getTimestamp("changed_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException | IllegalStateException e) {
            System.out.println("Failed to retrieve history for task " + taskId + ": " + e.getMessage());
        }

        return historyList;
    }

    /**
     * Retrieves all recorded status change history entries across all tasks.
     */
    public List<TaskHistory> getAllHistory() {
        String sql = """
                SELECT th.history_id, th.task_id, t.title AS task_title,
                       th.old_status, th.new_status, th.changed_at
                FROM task_history th
                JOIN tasks t ON th.task_id = t.id
                ORDER BY th.changed_at DESC
                """;

        List<TaskHistory> historyList = new ArrayList<>();

        try (Connection connection = taskDAO.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                historyList.add(new TaskHistory(
                        rs.getInt("history_id"),
                        rs.getInt("task_id"),
                        rs.getString("task_title"),
                        TaskStatus.valueOf(rs.getString("old_status")),
                        TaskStatus.valueOf(rs.getString("new_status")),
                        rs.getTimestamp("changed_at").toLocalDateTime()
                ));
            }
        } catch (SQLException | IllegalStateException e) {
            System.out.println("Failed to retrieve all task history: " + e.getMessage());
        }

        return historyList;
    }
}
