package org.logging.logger;

import org.logging.config.LoggerConfig;
import org.logging.formatter.DefaultLogFormatter;
import org.logging.model.LogLevel;
import org.logging.sink.ConsoleSink;

import java.util.concurrent.ConcurrentHashMap;

public class LogManager {

    private static final ConcurrentHashMap<String, Logger> loggers = new ConcurrentHashMap<>();

    private static volatile LoggerConfig defaultConfig = LoggerConfig.builder()
            .level(LogLevel.INFO)
            .addSink(new ConsoleSink())
            .formatter(new DefaultLogFormatter())
            .build();

    private LogManager() {}

    public static Logger getLogger(String name) {
        return loggers.computeIfAbsent(name, n -> new Logger(n, defaultConfig));
    }

    public static void setDefaultConfig(LoggerConfig config) {
        defaultConfig = config;
    }

    public static LoggerConfig getDefaultConfig() {
        return defaultConfig;
    }
}
