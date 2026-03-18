# Logging Library — Low-Level Design

## Problem Statement

Design and implement a logging library (similar to Log4j/SLF4J) that supports multiple log levels,
multiple output destinations, configurable formatting, and thread-safe operation.

---

## Entity Models / Class Diagram

```
┌─────────────┐
│  LogLevel    │  (enum)
│─────────────│
│  DEBUG       │
│  INFO        │
│  WARN        │
│  ERROR       │
│  FATAL       │
└─────────────┘

┌──────────────────────────────────┐
│  LogMessage                      │  (model / POJO)
│──────────────────────────────────│
│  - level      : LogLevel         │
│  - message    : String           │
│  - timestamp  : LocalDateTime    │
│  - loggerName : String           │
│  - threadName : String           │
└──────────────────────────────────┘

┌──────────────────────────────────┐
│  <<interface>> LogFormatter      │
│──────────────────────────────────│
│  + format(LogMessage) : String   │
└──────────────┬───────────────────┘
               │
      ┌────────┴─────────┐
      │ DefaultFormatter  │  → "2026-03-18 12:00:00 [INFO] [OrderService] message"
      └──────────────────┘

┌──────────────────────────────────────┐
│  <<interface>> Sink                  │
│──────────────────────────────────────│
│  + write(LogMessage, LogFormatter)   │
└──────────────┬───────────────────────┘
               │
      ┌────────┼────────────────┐
      │        │                │
┌───────────┐ ┌──────────┐ ┌────────────────┐
│ConsoleSink│ │ FileSink  │ │AsyncSinkDecorator│  (wraps any Sink)
└───────────┘ └──────────┘ └────────────────┘

┌──────────────────────────────────────────┐
│  LoggerConfig                            │
│──────────────────────────────────────────│
│  - level      : LogLevel                 │
│  - sinks      : List<Sink>              │
│  - formatter  : LogFormatter             │
│──────────────────────────────────────────│
│  + builder()  : LoggerConfigBuilder      │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  Logger                                  │
│──────────────────────────────────────────│
│  - name       : String                   │
│  - config     : LoggerConfig             │
│──────────────────────────────────────────│
│  + log(LogLevel, String)                 │
│  + debug(String)                         │
│  + info(String)                          │
│  + warn(String)                          │
│  + error(String)                         │
│  + fatal(String)                         │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  LogManager                (Singleton)   │
│──────────────────────────────────────────│
│  - loggers : Map<String, Logger>         │
│  - defaultConfig : LoggerConfig          │
│──────────────────────────────────────────│
│  + getLogger(String name) : Logger       │
│  + setDefaultConfig(LoggerConfig)        │
└──────────────────────────────────────────┘
```

---

## Entities to Create (in implementation order)

### 1. `LogLevel` — Enum
Defines severity. Ordering matters for filtering (`DEBUG < INFO < WARN < ERROR < FATAL`).

### 2. `LogMessage` — Immutable Model
Represents a single log event. Holds level, message text, timestamp, logger name, and thread name.

### 3. `LogFormatter` — Interface
Single method: `String format(LogMessage msg)`. Allows pluggable formatting.

### 4. `DefaultLogFormatter` — Implementation
Produces output like: `2026-03-18 12:00:00.123 [INFO] [OrderService] [main] Order placed`

### 5. `Sink` — Interface
Single method: `void write(LogMessage msg, LogFormatter formatter)`. Represents an output destination.

### 6. `ConsoleSink` — Sink Implementation
Writes formatted message to `System.out`.

### 7. `FileSink` — Sink Implementation
Writes formatted message to a file via `BufferedWriter`.

### 8. `LoggerConfig` — Configuration Object
Holds the log level threshold, list of sinks, and formatter. Use Builder pattern.

### 9. `Logger` — Core Class
Accepts log calls, checks level threshold, builds `LogMessage`, dispatches to all sinks.

### 10. `LogManager` — Singleton Registry
Maintains a `ConcurrentHashMap<String, Logger>`. Factory method `getLogger(name)`.

### Extension Entities (implement if time permits)

### 11. `AsyncSinkDecorator` — Decorator around Sink
Wraps any `Sink` with a `BlockingQueue` + background consumer thread for non-blocking writes.

### 12. `RotatingFileSink` — Decorator around FileSink
Rotates the file when it exceeds a configured size threshold.

---

## Package Structure

```
src/main/java/org/example/
├── model/
│   ├── LogLevel.java
│   └── LogMessage.java
├── formatter/
│   ├── LogFormatter.java
│   └── DefaultLogFormatter.java
├── sink/
│   ├── Sink.java
│   ├── ConsoleSink.java
│   ├── FileSink.java
│   └── AsyncSinkDecorator.java
├── config/
│   └── LoggerConfig.java
├── logger/
│   ├── Logger.java
│   └── LogManager.java
└── Main.java              ← demo / driver
```

---

## Usage Example (Target API)

```java
// Configure
LoggerConfig config = LoggerConfig.builder()
    .level(LogLevel.INFO)
    .addSink(new ConsoleSink())
    .addSink(new FileSink("/tmp/app.log"))
    .formatter(new DefaultLogFormatter())
    .build();

LogManager.setDefaultConfig(config);

// Use
Logger logger = LogManager.getLogger("OrderService");
logger.info("Order placed for userId=123");
logger.debug("This will be filtered out since level is INFO");
logger.error("Payment gateway timeout");
```

---

## Design Patterns Used

| Pattern           | Where                                  |
|-------------------|----------------------------------------|
| **Singleton**     | `LogManager`                           |
| **Strategy**      | `Sink` interface, `LogFormatter`       |
| **Observer**      | Logger → broadcasts to multiple sinks  |
| **Builder**       | `LoggerConfig.builder()`               |
| **Factory**       | `LogManager.getLogger(name)`           |
| **Decorator**     | `AsyncSinkDecorator`, `RotatingFileSink` |
