package org.logging.formatter;

import org.logging.model.LogMessage;

import java.time.format.DateTimeFormatter;

public class DefaultLogFormatter implements LogFormatter {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public String format(LogMessage message) {
        return String.format("%s [%s] [%s] [%s] %s",
                message.getTimestamp().format(TIMESTAMP_FORMAT),
                message.getLevel(),
                message.getLoggerName(),
                message.getThreadName(),
                message.getMessage());
    }
}
