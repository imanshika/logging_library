package org.logging;

import org.logging.config.LoggerConfig;
import org.logging.formatter.DefaultLogFormatter;
import org.logging.logger.LogManager;
import org.logging.logger.Logger;
import org.logging.model.LogLevel;
import org.logging.sink.AsyncSinkDecorator;
import org.logging.sink.ConsoleSink;
import org.logging.sink.FileSink;
import org.logging.sink.Sink;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        // Wrap FileSink with AsyncSinkDecorator for non-blocking writes
        Sink asyncFileSink = new AsyncSinkDecorator(new FileSink("/tmp/app.log"));

        LoggerConfig config = LoggerConfig.builder()
                .level(LogLevel.DEBUG)
                .addSink(new ConsoleSink())
                .addSink(asyncFileSink)
                .formatter(new DefaultLogFormatter())
                .build();

        LogManager.setDefaultConfig(config);

        Logger logger = LogManager.getLogger("Main");

        // Simulate 3 threads logging concurrently
        Thread t1 = new Thread(() -> {
            Logger log = LogManager.getLogger("OrderService");
            for (int i = 0; i < 5; i++) {
                log.info("Processing order #" + i);
            }
        }, "order-thread");

        Thread t2 = new Thread(() -> {
            Logger log = LogManager.getLogger("PaymentService");
            for (int i = 0; i < 5; i++) {
                log.warn("Payment attempt #" + i);
            }
        }, "payment-thread");

        Thread t3 = new Thread(() -> {
            Logger log = LogManager.getLogger("InventoryService");
            for (int i = 0; i < 5; i++) {
                log.error("Stock check #" + i);
            }
        }, "inventory-thread");

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        logger.info("All threads completed");

        // Graceful shutdown — drains remaining messages, closes file
        asyncFileSink.close();
    }
}
