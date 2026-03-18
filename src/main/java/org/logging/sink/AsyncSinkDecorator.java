package org.logging.sink;

import org.logging.formatter.LogFormatter;
import org.logging.model.LogMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class AsyncSinkDecorator implements Sink {

    private record LogTask(LogMessage message, LogFormatter formatter) {}

    private final Sink wrappedSink;
    private final BlockingQueue<LogTask> queue;
    private final Thread workerThread;
    private volatile boolean running = true;

    public AsyncSinkDecorator(Sink wrappedSink) {
        this(wrappedSink, 1024);
    }

    public AsyncSinkDecorator(Sink wrappedSink, int queueCapacity) {
        this.wrappedSink = wrappedSink;
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        this.workerThread = new Thread(this::processQueue, "async-sink-worker");
        this.workerThread.setDaemon(true);
        this.workerThread.start();
    }

    @Override
    public void write(LogMessage message, LogFormatter formatter) {
        if (!running) {
            return;
        }
        queue.offer(new LogTask(message, formatter));
    }

    private void processQueue() {
        while (running) {
            try {
                LogTask task = queue.take();
                wrappedSink.write(task.message(), task.formatter());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void close() {
        running = false;
        workerThread.interrupt();

        List<LogTask> remaining = new ArrayList<>();
        queue.drainTo(remaining);
        for (LogTask task : remaining) {
            wrappedSink.write(task.message(), task.formatter());
        }

        wrappedSink.close();
    }
}
