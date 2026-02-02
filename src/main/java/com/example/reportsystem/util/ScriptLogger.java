package com.example.reportsystem.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Groovy 脚本执行日志收集器
 * 用于在页面显示 Groovy 脚本的执行日志和错误信息
 */
public class ScriptLogger {

    private final List<LogEntry> logs = new ArrayList<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static class LogEntry {
        private final String timestamp;
        private final String level;
        private final String message;
        private final String exception;

        public LogEntry(String level, String message, String exception) {
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            this.level = level;
            this.message = message;
            this.exception = exception;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }

        public String getException() {
            return exception;
        }

        @Override
        public String toString() {
            String result = String.format("[%s] [%s] %s", timestamp, level, message);
            if (exception != null && !exception.isEmpty()) {
                result += "\n" + exception;
            }
            return result;
        }
    }

    public void info(String message) {
        logs.add(new LogEntry("INFO", message, null));
    }

    public void warn(String message) {
        logs.add(new LogEntry("WARN", message, null));
    }

    public void error(String message) {
        logs.add(new LogEntry("ERROR", message, null));
    }

    public void error(String message, Throwable throwable) {
        String exceptionInfo = throwable.getClass().getName() + ": " + throwable.getMessage();
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        StringBuilder sb = new StringBuilder(exceptionInfo);
        sb.append("\n");
        for (int i = 0; i < Math.min(stackTrace.length, 10); i++) {
            sb.append("    at ").append(stackTrace[i]).append("\n");
        }
        if (stackTrace.length > 10) {
            sb.append("    ... ").append(stackTrace.length - 10).append(" more").append("\n");
        }
        logs.add(new LogEntry("ERROR", message, sb.toString()));
    }

    public void debug(String message) {
        logs.add(new LogEntry("DEBUG", message, null));
    }

    public List<LogEntry> getLogs() {
        return new ArrayList<>(logs);
    }

    public String getFullLog() {
        StringBuilder sb = new StringBuilder();
        for (LogEntry entry : logs) {
            sb.append(entry.toString()).append("\n");
        }
        return sb.toString();
    }

    public boolean hasErrors() {
        return logs.stream().anyMatch(entry -> "ERROR".equals(entry.getLevel()));
    }

    public int getErrorCount() {
        return (int) logs.stream().filter(entry -> "ERROR".equals(entry.getLevel())).count();
    }

    public void clear() {
        logs.clear();
    }
}
