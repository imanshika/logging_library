package org.logging.logger;

import org.logging.config.LoggerConfig;
import org.logging.model.LogLevel;
import org.logging.model.LogMessage;
import org.logging.sink.Sink;

public class Logger {

    private final String name;
    private volatile LoggerConfig config;

    public Logger(String name, LoggerConfig config) {
        this.name = name;
        this.config = config;
    }

    public void log(LogLevel level, String message) {
        if (!level.isAtLeast(config.getLevel())) {
            return;
        }

        LogMessage logMessage = new LogMessage(level, message, name);

        for (Sink sink : config.getSinks()) {
            sink.write(logMessage, config.getFormatter());
        }
    }

    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    public void warn(String message) {
        log(LogLevel.WARN, message);
    }

    public void error(String message) {
        log(LogLevel.ERROR, message);
    }

    public void fatal(String message) {
        log(LogLevel.FATAL, message);
    }

    public String getName() {
        return name;
    }

    public void setConfig(LoggerConfig config) {
        this.config = config;
    }
}
