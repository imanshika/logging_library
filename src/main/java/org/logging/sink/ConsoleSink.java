package org.logging.sink;

import org.logging.formatter.LogFormatter;
import org.logging.model.LogMessage;

public class ConsoleSink implements Sink {

    @Override
    public void write(LogMessage message, LogFormatter formatter) {
        System.out.println(formatter.format(message));
    }
}
