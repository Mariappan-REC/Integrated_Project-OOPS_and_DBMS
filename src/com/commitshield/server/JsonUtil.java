package com.commitshield.server;

import com.commitshield.model.Task;
import com.commitshield.model.TaskDependency;
import com.commitshield.model.TaskHistory;
import com.commitshield.model.TaskPriority;
import com.commitshield.model.TaskStatus;
import com.commitshield.model.WorkloadSummary;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight, safe JSON serialization(java to JSON) and parsing(JSON to java) utility.
 * Avoids heavy third-party framework dependencies.
 */
public class JsonUtil {

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    public static String taskToJson(Task task) {
        if (task == null) return "null";
        return String.format(
                "{\"id\":%d,\"title\":\"%s\",\"estimatedHours\":%.2f,\"deadline\":\"%s\",\"priority\":\"%s\",\"status\":\"%s\"}",
                task.getId(),
                escape(task.getTitle()),
                task.getEstimatedHours(),
                task.getDeadline() != null ? task.getDeadline().format(ISO_FORMAT) : "",
                task.getPriority() != null ? task.getPriority().name() : "",
                task.getStatus() != null ? task.getStatus().name() : ""
        );
    }

    public static String tasksToJson(List<Task> tasks) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < tasks.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(taskToJson(tasks.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String dependencyToJson(TaskDependency dep) {
        if (dep == null) return "null";
        return String.format(
                "{\"taskId\":%d,\"taskTitle\":\"%s\",\"dependsOnTaskId\":%d,\"dependsOnTitle\":\"%s\",\"createdAt\":\"%s\"}",
                dep.getTaskId(),
                escape(dep.getTaskTitle()),
                dep.getDependsOnTaskId(),
                escape(dep.getDependsOnTitle()),
                dep.getCreatedAt() != null ? dep.getCreatedAt().format(ISO_FORMAT) : ""
        );
    }

    public static String dependenciesToJson(List<TaskDependency> deps) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < deps.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(dependencyToJson(deps.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String historyToJson(TaskHistory history) {
        if (history == null) return "null";
        return String.format(
                "{\"historyId\":%d,\"taskId\":%d,\"taskTitle\":\"%s\",\"oldStatus\":\"%s\",\"newStatus\":\"%s\",\"changedAt\":\"%s\"}",
                history.getHistoryId(),
                history.getTaskId(),
                escape(history.getTaskTitle()),
                history.getOldStatus() != null ? history.getOldStatus().name() : "",
                history.getNewStatus() != null ? history.getNewStatus().name() : "",
                history.getChangedAt() != null ? history.getChangedAt().format(ISO_FORMAT) : ""
        );
    }

    public static String historiesToJson(List<TaskHistory> histories) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < histories.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(historyToJson(histories.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String workloadToJson(WorkloadSummary summary) {
        StringBuilder sb = new StringBuilder("{");
        sb.append(String.format("\"unfinishedHours\":%.2f,", summary.getUnfinishedHours()));
        sb.append(String.format("\"availableHours\":%.2f,", summary.getAvailableHours()));
        sb.append(String.format("\"deficit\":%.2f,", summary.getDeficit()));
        sb.append(String.format("\"hasConflict\":%b,", summary.isHasConflict()));
        sb.append("\"priorityWorkload\":{");
        int count = 0;
        for (Map.Entry<TaskPriority, Double> entry : summary.getPriorityWorkload().entrySet()) {
            if (count > 0) sb.append(",");
            sb.append(String.format("\"%s\":%.2f", entry.getKey().name(), entry.getValue()));
            count++;
        }
        sb.append("}}");
        return sb.toString();
    }

    public static String successResponse(String message, Object... extraJsonFields) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\":true,\"message\":\"").append(escape(message)).append("\"");
        for (int i = 0; i < extraJsonFields.length; i += 2) {
            if (i + 1 < extraJsonFields.length) {
                sb.append(",\"").append(extraJsonFields[i]).append("\":").append(extraJsonFields[i + 1]);
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public static String errorResponse(String errorMessage) {
        return "{\"success\":false,\"error\":\"" + escape(errorMessage) + "\"}";
    }

    /**
     * Parses simple JSON object body into key-value map of strings.
     */
    public static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;

        // Matches "key"\s*:\s*("([^"]*)"|([0-9\.\-]+)|true|false|null)
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([0-9\\.\\-]+)|(true|false|null))");
        Matcher matcher = pattern.matcher(json);

        while (matcher.find()) {
            String key = matcher.group(1);
            String stringVal = matcher.group(2);
            String numVal = matcher.group(3);
            String boolVal = matcher.group(4);

            if (stringVal != null) {
                map.put(key, stringVal);
            } else if (numVal != null) {
                map.put(key, numVal);
            } else if (boolVal != null) {
                map.put(key, boolVal);
            }
        }

        return map;
    }
}
