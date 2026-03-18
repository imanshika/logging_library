package org.logging.config;

import org.logging.formatter.DefaultLogFormatter;
import org.logging.formatter.LogFormatter;
import org.logging.model.LogLevel;
import org.logging.sink.Sink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoggerConfig {

    private final LogLevel level;
    private final List<Sink> sinks;
    private final LogFormatter formatter;

    private LoggerConfig(LogLevel level, List<Sink> sinks, LogFormatter formatter) {
        this.level = level;
        this.sinks = Collections.unmodifiableList(sinks);
        this.formatter = formatter;
    }

    public LogLevel getLevel() {
        return level;
    }

    public List<Sink> getSinks() {
        return sinks;
    }

    public LogFormatter getFormatter() {
        return formatter;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LogLevel level = LogLevel.INFO;
        private final List<Sink> sinks = new ArrayList<>();
        private LogFormatter formatter = new DefaultLogFormatter();

        public Builder level(LogLevel level) {
            this.level = level;
            return this;
        }

        public Builder addSink(Sink sink) {
            this.sinks.add(sink);
            return this;
        }

        public Builder formatter(LogFormatter formatter) {
            this.formatter = formatter;
            return this;
        }

        public LoggerConfig build() {
            return new LoggerConfig(level, sinks, formatter);
        }
    }
}
