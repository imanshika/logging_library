package org.logging.sink;

import org.logging.formatter.LogFormatter;
import org.logging.model.LogMessage;

public interface Sink {
    void write(LogMessage message, LogFormatter formatter);

    default void close() {}
}
