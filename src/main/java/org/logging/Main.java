package org.logging;

import org.logging.config.LoggerConfig;
import org.logging.formatter.DefaultLogFormatter;
import org.logging.logger.LogManager;
import org.logging.logger.Logger;
import org.logging.model.LogLevel;
import org.logging.sink.ConsoleSink;
import org.logging.sink.FileSink;

public class Main {
    public static void main(String[] args) {

        // 1. Use default config (INFO + ConsoleSink)
        Logger logger = LogManager.getLogger("Main");
        logger.info("App started with default config");
        logger.debug("This won't appear — default level is INFO");

        // 2. Custom config: DEBUG level + Console + File
        LoggerConfig customConfig = LoggerConfig.builder()
                .level(LogLevel.DEBUG)
                .addSink(new ConsoleSink())
                .addSink(new FileSink("/tmp/app.log"))
                .formatter(new DefaultLogFormatter())
                .build();

        LogManager.setDefaultConfig(customConfig);

        Logger orderLogger = LogManager.getLogger("OrderService");
        orderLogger.debug("Fetching order details for orderId=42");
        orderLogger.info("Order placed successfully");
        orderLogger.warn("Inventory running low for SKU-1001");
        orderLogger.error("Payment gateway timeout after 30s");
        orderLogger.fatal("Database connection pool exhausted");
    }
}
