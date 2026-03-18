package org.logging.formatter;

import org.logging.model.LogMessage;

public interface LogFormatter {
    String format(LogMessage message);
}
