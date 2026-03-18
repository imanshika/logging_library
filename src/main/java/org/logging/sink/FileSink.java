package org.logging.sink;

import org.logging.formatter.LogFormatter;
import org.logging.model.LogMessage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FileSink implements Sink {

    private final String filePath;
    private final BufferedWriter writer;

    public FileSink(String filePath) {
        this.filePath = filePath;
        try {
            this.writer = new BufferedWriter(new FileWriter(filePath, true));
        } catch (IOException e) {
            throw new RuntimeException("Failed to open log file: " + filePath, e);
        }
    }

    @Override
    public synchronized void write(LogMessage message, LogFormatter formatter) {
        try {
            writer.write(formatter.format(message));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("Failed to write log to file: " + filePath + " — " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.err.println("Failed to close log file: " + filePath + " — " + e.getMessage());
        }
    }
}
