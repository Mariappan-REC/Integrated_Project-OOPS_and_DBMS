package com.commitshield.server;

import com.commitshield.model.Task;
import com.commitshield.model.TaskPriority;
import com.commitshield.model.TaskStatus;
import com.commitshield.model.WorkloadSummary;
import com.commitshield.service.TaskService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Lightweight HTTP Server providing REST API endpoints and serving the dark dashboard frontend.
 * Uses JDK built-in HttpServer (zero heavy third-party framework overhead).
 */
public class CommitShieldHttpServer {

    private static final int DEFAULT_PORT = 8080;
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    };

    private final TaskService taskService;
    private final int port;
    private HttpServer server;

    public CommitShieldHttpServer(int port) {
        this.port = port;
        this.taskService = new TaskService();
    }

    public CommitShieldHttpServer() {
        this(resolvePort());
    }

    private static int resolvePort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            try {
                return Integer.parseInt(portEnv.trim());
            } catch (NumberFormatException ignored) {}
        }
        return DEFAULT_PORT;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // API Contexts
        server.createContext("/api/tasks", new TasksHandler());
        server.createContext("/api/workload", new WorkloadHandler());
        server.createContext("/api/dependencies", new DependenciesHandler());
        server.createContext("/api/history", new HistoryHandler());

        // Static Frontend Context
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null); // default executor
        server.start();

        System.out.println("=================================================");
        System.out.println("   CommitShield 2.0 Web Server Started!");
        System.out.println("   Dashboard URL: http://localhost:" + port + "/");
        System.out.println("   API Base:      http://localhost:" + port + "/api/");
        System.out.println("=================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("CommitShield server stopped.");
        }
    }

    public static void main(String[] args) {
        try {
            CommitShieldHttpServer server = new CommitShieldHttpServer();
            server.start();

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        } catch (Exception e) {
            System.err.println("Failed to start CommitShield server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper: Parse URL query string
    private static Map<String, String> parseQueryParams(URI uri) {
        Map<String, String> queryMap = new HashMap<>();
        String query = uri.getRawQuery();
        if (query == null || query.isEmpty()) return queryMap;

        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                queryMap.put(pair[0].trim(), java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            } else if (pair.length == 1) {
                queryMap.put(pair[0].trim(), "");
            }
        }
        return queryMap;
    }

    // Helper: Read request body as String
    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // Helper: Send JSON response
    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static boolean handleOptions(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private static LocalDateTime parseDateTimeSafely(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        String s = str.trim();
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(s, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    // -------------------------------------------------------------
    // Handler: /api/tasks/*
    // -------------------------------------------------------------
    private class TasksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod().toUpperCase();

            try {
                // GET /api/tasks/unfinished
                if ("GET".equals(method) && path.equals("/api/tasks/unfinished")) {
                    sendJsonResponse(exchange, 200, JsonUtil.tasksToJson(taskService.getUnfinishedTasksByPriority()));
                    return;
                }

                // Sub-path routing: /api/tasks/{id} or /api/tasks/{id}/status or dependencies/history
                String subPath = path.substring("/api/tasks".length());
                if (subPath.startsWith("/")) subPath = subPath.substring(1);

                if (subPath.isEmpty()) {
                    // /api/tasks
                    if ("GET".equals(method)) {
                        sendJsonResponse(exchange, 200, JsonUtil.tasksToJson(taskService.getAllTasks()));
                    } else if ("POST".equals(method)) {
                        handleCreateTask(exchange);
                    } else {
                        sendJsonResponse(exchange, 405, JsonUtil.errorResponse("Method not allowed"));
                    }
                    return;
                }

                String[] parts = subPath.split("/");
                int taskId;
                try {
                    taskId = Integer.parseInt(parts[0]);
                } catch (NumberFormatException e) {
                    sendJsonResponse(exchange, 400, JsonUtil.errorResponse("Invalid task ID format"));
                    return;
                }

                if (parts.length == 1) {
                    // /api/tasks/{id}
                    if ("GET".equals(method)) {
                        Optional<Task> task = taskService.getTaskById(taskId);
                        if (task.isPresent()) {
                            sendJsonResponse(exchange, 200, JsonUtil.taskToJson(task.get()));
                        } else {
                            sendJsonResponse(exchange, 404, JsonUtil.errorResponse("Task not found with ID: " + taskId));
                        }
                    } else if ("DELETE".equals(method)) {
                        boolean deleted = taskService.deleteTask(taskId);
                        if (deleted) {
                            sendJsonResponse(exchange, 200, JsonUtil.successResponse("Task deleted successfully"));
                        } else {
                            sendJsonResponse(exchange, 404, JsonUtil.errorResponse("Task not found with ID: " + taskId));
                        }
                    } else if ("PUT".equals(method)) {
                        // PUT /api/tasks/{id} (edit title and/or estimated hours)
                        handleUpdateTask(exchange, taskId);
                    } else {
                        sendJsonResponse(exchange, 405, JsonUtil.errorResponse("Method not allowed"));
                    }
                } else if (parts.length == 2 && "status".equals(parts[1]) && "PUT".equals(method)) {
                    // PUT /api/tasks/{id}/status
                    handleUpdateStatus(exchange, taskId);
                } else if (parts.length == 2 && "title".equals(parts[1]) && "PUT".equals(method)) {
                    // PUT /api/tasks/{id}/title
                    handleUpdateTitle(exchange, taskId);
                } else if (parts.length == 2 && "hours".equals(parts[1]) && "PUT".equals(method)) {
                    // PUT /api/tasks/{id}/hours
                    handleUpdateHours(exchange, taskId);
                } else if (parts.length == 2 && "dependencies".equals(parts[1]) && "GET".equals(method)) {
                    // GET /api/tasks/{id}/dependencies
                    sendJsonResponse(exchange, 200, JsonUtil.dependenciesToJson(taskService.getDependenciesForTask(taskId)));
                } else if (parts.length == 2 && "history".equals(parts[1]) && "GET".equals(method)) {
                    // GET /api/tasks/{id}/history
                    sendJsonResponse(exchange, 200, JsonUtil.historiesToJson(taskService.getHistoryByTaskId(taskId)));
                } else {
                    sendJsonResponse(exchange, 404, JsonUtil.errorResponse("Endpoint not found"));
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, JsonUtil.errorResponse("Internal server error: " + e.getMessage()));
            }
        }

        private void handleCreateTask(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            Map<String, String> data = JsonUtil.parseSimpleJson(body);

            String title = data.get("title");
            if (title == null || title.trim().isEmpty()) {
                sendJsonResponse(exchange, 400, JsonUtil.errorResponse("Task title is required."));
                return;
            }

            double hours;
            try {
                hours = Double.parseDouble(data.get("estimatedHours"));
                if (!Double.isFinite(hours) || hours <= 0) {
                    sendJsonResponse(exchange, 400, JsonUtil.errorResponse("Estimated hours must be greater than zero."));
                    return;
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, JsonUtil.errorResponse("Invalid estimated hours value."));
                return;
            }

            LocalDateTime deadline = parseDateTimeSafely(data.get("deadline"));
            if (deadline == null) {
                sendJsonResponse(exchange, 400, JsonUtil.errorResponse("Invalid deadline format. Use yyyy-MM-dd HH:mm."));
                return;
            }

            TaskPriority priority;
            try {
                priority = TaskPriority.valueOf(data.get("priority").toUpperCase());
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, JsonUtil.errorResponse("Invalid priority. Must be LOW, MEDIUM, HIGH, or CRITICAL."));
                return;
            }

            TaskStatus status = TaskStatus.PENDING;
            if (data.containsKey("status")) {
                try {
                    status = TaskStatus.valueOf(data.get("status").toUpperCase().replace(' ', '_'));
                } catch (Exception ignored) {}
            }

            Task task = new Task(0, title.trim(), hours, deadline, priority, status);
            int newId = taskService.createTask(task);

            if (newId > 0) {
                Optional<Task> created = taskService.getTaskById(newId);
                String taskJson = created.isPresent() ? JsonUtil.taskToJson(created.get()) : "{\"id\":" + newId + "}";
                sendJsonResponse(exchange, 201, JsonUtil.successResponse("Task created successfully", "task", taskJson));
            } else {
                sendJsonResponse(exchange, 500, JsonUtil.errorResponse("Failed to create task in MySQL database."));
            }
        }

        private void handleUpdateStatus(HttpExchange exchange, int taskId) throws IOException {
            String body = readRequestBody(exchange);
            Map<String, String> data = JsonUtil.parseSimpleJson(body);

            String statusStr = data.get("status");
            if (statusStr == null || statusStr.trim().isEmpty()) {
                sendJsonResponse(exchange, 400, JsonUtil.errorResponse("Status field is required."));
                return;
            }

            TaskStatus newStatus;
            try {
                newStatus = TaskStatus.valueOf(statusStr.toUpperCase().replace(' ', '_'));
            } catch (IllegalArgumentException e) {
                sendJsonResponse(exchange, 400, JsonUtil.errorResponse("Invalid status value. Use PENDING, IN_PROGRESS, or COMPLETED."));
                return;
            }

            boolean updated = taskService.updateTaskStatus(taskId, newStatus);
            if (updated) {
                sendJsonResponse(exchange, 200, JsonUtil.successResponse("Task status updated successfully"));
            } else {
                sendJsonResponse(exchange, 404, JsonUtil.errorResponse("Task not found or update failed"));
            }
        }

        private void handleUpdateTitle(HttpExchange exchange, int taskId) throws IOException {
            String body = readRequestBody(exchange);
            Map<String, String> data = JsonUtil.parseSimpleJson(body);

            String title = data.get("title");
            if (title == null || title.trim().isEmpty()) {
                sendJsonResponse(exchange, 400, JsonUtil.errorResponse("Task title is required and cannot be empty."));
                return;
            }
            if (title.trim().length() > 100) {
                sendJsonResponse(exchange, 400, JsonUtil.errorResponse("Task title must not exceed 100 characters."));
                return;
            }

            // Calls overloaded method updateTask(int, String)
            boolean updated = taskService.updateTask(taskId, title.trim());
            if (updated) {
                sendJsonResponse(exchange, 200, JsonUtil.successResponse("Task title updated successfully"));
            } else {
                sendJsonResponse(exchange, 404, JsonUtil.errorResponse("Task not found with ID: " + taskId + " or update failed."));
            }
        }

        private void handleUpdateHours(HttpExchange exchange, int taskId) throws IOException {
            String body = readRequestBody(exchange);
            Map<String, String> data = JsonUtil.parseSimpleJson(body);

            String hoursStr = data.get("estimatedHours");
            if (hoursStr == null) hoursStr = data.get("hours");

            if (hoursStr == null || hoursStr.trim().isEmpty()) {
                sendJsonResponse(exchange, 400, JsonUtil.errorResponse("Estimated hours field is required."));
                return;
            }

            double hours;
            try {
                hours = Double.parseDouble(hoursStr.trim());
                if (!Double.isFinite(hours) || hours <= 0 || hours > 9999.99) {
                    sendJsonResponse(exchange, 400, JsonUtil.errorResponse("Estimated hours must be a positive finite number up to 9999.99."));
                    return;
                }
            } catch (NumberFormatException e) {
                sendJsonResponse(exchange, 400, JsonUtil.errorResponse("Invalid estimated hours format."));
                return;
            }

            // Calls overloaded method updateTask(int, double)
            boolean updated = taskService.updateTask(taskId, hours);
            if (updated) {
                sendJsonResponse(exchange, 200, JsonUtil.successResponse("Task estimated hours updated successfully"));
            } else {
                sendJsonResponse(exchange, 404, JsonUtil.errorResponse("Task not found with ID: " + taskId + " or update failed."));
            }
        }

        private void handleUpdateTask(HttpExchange exchange, int taskId) throws IOException {
            String body = readRequestBody(exchange);
            Map<String, String> data = JsonUtil.parseSimpleJson(body);

            boolean hasTitle = data.containsKey("title");
            boolean hasHours = data.containsKey("estimatedHours") || data.containsKey("hours");

            if (!hasTitle && !hasHours) {
                sendJsonResponse(exchange, 400, JsonUtil.errorResponse("At least one field ('title' or 'estimatedHours') must be provided to edit."));
                return;
            }

            // Verify task exists first
            if (taskService.getTaskById(taskId).isEmpty()) {
                sendJsonResponse(exchange, 404, JsonUtil.errorResponse("Task not found with ID: " + taskId));
                return;
            }

            if (hasTitle) {
                String title = data.get("title");
                if (title == null || title.trim().isEmpty() || title.trim().length() > 100) {
                    sendJsonResponse(exchange, 400, JsonUtil.errorResponse("Task title cannot be empty and must not exceed 100 characters."));
                    return;
                }
                boolean titleUpdated = taskService.updateTask(taskId, title.trim());
                if (!titleUpdated) {
                    sendJsonResponse(exchange, 500, JsonUtil.errorResponse("Failed to update task title."));
                    return;
                }
            }

            if (hasHours) {
                String hoursStr = data.get("estimatedHours");
                if (hoursStr == null) hoursStr = data.get("hours");
                double hours;
                try {
                    hours = Double.parseDouble(hoursStr.trim());
                    if (!Double.isFinite(hours) || hours <= 0 || hours > 9999.99) {
                        sendJsonResponse(exchange, 400, JsonUtil.errorResponse("Estimated hours must be a positive finite number up to 9999.99."));
                        return;
                    }
                } catch (Exception e) {
                    sendJsonResponse(exchange, 400, JsonUtil.errorResponse("Invalid estimated hours value."));
                    return;
                }
                boolean hoursUpdated = taskService.updateTask(taskId, hours);
                if (!hoursUpdated) {
                    sendJsonResponse(exchange, 500, JsonUtil.errorResponse("Failed to update task estimated hours."));
                    return;
                }
            }

            sendJsonResponse(exchange, 200, JsonUtil.successResponse("Task updated successfully"));
        }
    }

    // -------------------------------------------------------------
    // Handler: /api/workload
    // -------------------------------------------------------------
    private class WorkloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, JsonUtil.errorResponse("Method not allowed"));
                return;
            }

            Map<String, String> query = parseQueryParams(exchange.getRequestURI());
            double availableHours = 0.0;
            if (query.containsKey("availableHours")) {
                try {
                    availableHours = Double.parseDouble(query.get("availableHours"));
                } catch (NumberFormatException ignored) {}
            }

            WorkloadSummary summary = taskService.getWorkloadSummary(availableHours);
            sendJsonResponse(exchange, 200, JsonUtil.workloadToJson(summary));
        }
    }

    // -------------------------------------------------------------
    // Handler: /api/dependencies
    // -------------------------------------------------------------
    private class DependenciesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;

            String method = exchange.getRequestMethod().toUpperCase();

            if ("GET".equals(method)) {
                sendJsonResponse(exchange, 200, JsonUtil.dependenciesToJson(taskService.getAllDependencies()));
            } else if ("POST".equals(method)) {
                String body = readRequestBody(exchange);
                Map<String, String> data = JsonUtil.parseSimpleJson(body);

                try {
                    int taskId = Integer.parseInt(data.get("taskId"));
                    int dependsOnTaskId = Integer.parseInt(data.get("dependsOnTaskId"));

                    if (taskId == dependsOnTaskId) {
                        sendJsonResponse(exchange, 400, JsonUtil.errorResponse("A task cannot depend on itself."));
                        return;
                    }

                    boolean added = taskService.addDependency(taskId, dependsOnTaskId);
                    if (added) {
                        sendJsonResponse(exchange, 201, JsonUtil.successResponse("Dependency created successfully"));
                    } else {
                        sendJsonResponse(exchange, 400, JsonUtil.errorResponse("Failed to create dependency. Verify tasks exist and dependency is not duplicate."));
                    }
                } catch (Exception e) {
                    sendJsonResponse(exchange, 400, JsonUtil.errorResponse("Invalid taskId or dependsOnTaskId."));
                }
            } else if ("DELETE".equals(method)) {
                Map<String, String> query = parseQueryParams(exchange.getRequestURI());
                int taskId = -1, dependsOnTaskId = -1;

                if (query.containsKey("taskId") && query.containsKey("dependsOnTaskId")) {
                    try {
                        taskId = Integer.parseInt(query.get("taskId"));
                        dependsOnTaskId = Integer.parseInt(query.get("dependsOnTaskId"));
                    } catch (NumberFormatException ignored) {}
                } else {
                    String body = readRequestBody(exchange);
                    Map<String, String> data = JsonUtil.parseSimpleJson(body);
                    try {
                        taskId = Integer.parseInt(data.get("taskId"));
                        dependsOnTaskId = Integer.parseInt(data.get("dependsOnTaskId"));
                    } catch (Exception ignored) {}
                }

                if (taskId <= 0 || dependsOnTaskId <= 0) {
                    sendJsonResponse(exchange, 400, JsonUtil.errorResponse("taskId and dependsOnTaskId are required."));
                    return;
                }

                boolean removed = taskService.removeDependency(taskId, dependsOnTaskId);
                if (removed) {
                    sendJsonResponse(exchange, 200, JsonUtil.successResponse("Dependency removed successfully"));
                } else {
                    sendJsonResponse(exchange, 404, JsonUtil.errorResponse("Dependency relationship not found."));
                }
            } else {
                sendJsonResponse(exchange, 405, JsonUtil.errorResponse("Method not allowed"));
            }
        }
    }

    // -------------------------------------------------------------
    // Handler: /api/history
    // -------------------------------------------------------------
    private class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, JsonUtil.historiesToJson(taskService.getAllHistory()));
            } else {
                sendJsonResponse(exchange, 405, JsonUtil.errorResponse("Method not allowed"));
            }
        }
    }

    // -------------------------------------------------------------
    // Handler: Static files (frontend)
    // -------------------------------------------------------------
    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Map requested path to frontend directory
            File file = new File("frontend", path.startsWith("/") ? path.substring(1) : path);

            if (!file.exists() || file.isDirectory()) {
                // If not found in frontend/, try falling back to index.html for SPA-style routing
                file = new File("frontend/index.html");
                if (!file.exists()) {
                    String response = "404 Not Found: Dashboard files not found in frontend/";
                    exchange.sendResponseHeaders(404, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes(StandardCharsets.UTF_8));
                    }
                    return;
                }
            }

            String contentType = "text/plain";
            String lower = file.getName().toLowerCase();
            if (lower.endsWith(".html")) contentType = "text/html; charset=UTF-8";
            else if (lower.endsWith(".css")) contentType = "text/css; charset=UTF-8";
            else if (lower.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";
            else if (lower.endsWith(".json")) contentType = "application/json; charset=UTF-8";
            else if (lower.endsWith(".svg")) contentType = "image/svg+xml";
            else if (lower.endsWith(".ico")) contentType = "image/x-icon";

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, file.length());

            try (InputStream is = new FileInputStream(file);
                 OutputStream os = exchange.getResponseBody()) {
                is.transferTo(os);
            }
        }
    }
}
