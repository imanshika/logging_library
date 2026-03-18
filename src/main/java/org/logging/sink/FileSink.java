package org.logging.sink;

import org.logging.formatter.LogFormatter;
import org.logging.model.LogMessage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FileSink implements Sink {

    private final String filePath;

    public FileSink(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void write(LogMessage message, LogFormatter formatter) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(formatter.format(message));
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Failed to write log to file: " + filePath + " — " + e.getMessage());
        }
    }
}
