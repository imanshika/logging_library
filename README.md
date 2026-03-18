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
│─────────────│
│  + isAtLeast(LogLevel) : boolean  │
└─────────────┘

┌──────────────────────────────────┐
│  LogMessage        (immutable)   │
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
      ┌────────┴──────────────┐
      │ DefaultLogFormatter    │  → "2026-03-18 12:00:00.123 [INFO] [OrderService] [main] message"
      └───────────────────────┘

┌──────────────────────────────────────┐
│  <<interface>> Sink                  │
│──────────────────────────────────────│
│  + write(LogMessage, LogFormatter)   │
│  + close()          (default no-op)  │
└──────────────┬───────────────────────┘
               │
      ┌────────┼──────────────────┐
      │        │                  │
┌───────────┐ ┌──────────────┐ ┌──────────────────┐
│ConsoleSink│ │   FileSink   │ │AsyncSinkDecorator│
│           │ │ (synchronized│ │ (Decorator)      │
│           │ │  + shared    │ │ wraps any Sink   │
│           │ │  writer)     │ │ with BlockingQ   │
└───────────┘ └──────────────┘ └──────────────────┘

┌──────────────────────────────────────────┐
│  LoggerConfig             (Builder)      │
│──────────────────────────────────────────│
│  - level      : LogLevel                 │
│  - sinks      : List<Sink> (unmodifiable)│
│  - formatter  : LogFormatter             │
│──────────────────────────────────────────│
│  + builder()  : Builder                  │
│    .level(LogLevel)      → Builder       │
│    .addSink(Sink)        → Builder       │
│    .formatter(LogFormatter) → Builder    │
│    .build()              → LoggerConfig  │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  Logger                                  │
│──────────────────────────────────────────│
│  - name       : String         (final)   │
│  - config     : LoggerConfig   (volatile)│
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
│  - loggers : ConcurrentHashMap           │
│  - defaultConfig : LoggerConfig (volatile)│
│──────────────────────────────────────────│
│  + getLogger(String name) : Logger       │
│  + setDefaultConfig(LoggerConfig)        │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  AsyncSinkDecorator       (Decorator)    │
│──────────────────────────────────────────│
│  - wrappedSink : Sink                    │
│  - queue  : ArrayBlockingQueue<LogTask>  │
│  - workerThread : Thread  (daemon)       │
│  - running : volatile boolean            │
│──────────────────────────────────────────│
│  + write()   → queue.offer(LogTask)      │
│  + close()   → drain queue + close sink  │
│──────────────────────────────────────────│
│  LogTask (record): message + formatter   │
│  → captures context at write-time        │
└──────────────────────────────────────────┘
```

---

## Entities (in implementation order)

### 1. `LogLevel` — Enum
Defines severity. Ordering matters for filtering (`DEBUG < INFO < WARN < ERROR < FATAL`).
Uses ordinal comparison via `isAtLeast()`.

### 2. `LogMessage` — Immutable Model
Represents a single log event. All fields `final` — thread-safe by design.
Timestamp and thread name captured automatically at construction.

### 3. `LogFormatter` — Interface (Strategy)
Single method: `String format(LogMessage msg)`. Pluggable formatting.

### 4. `DefaultLogFormatter` — Implementation
Produces: `2026-03-18 12:00:00.123 [INFO] [OrderService] [main] Order placed`

### 5. `Sink` — Interface (Strategy)
`void write(LogMessage, LogFormatter)` + `default void close()`.
Represents an output destination. Adding a new sink = implement this interface.

### 6. `ConsoleSink` — Sink Implementation
Writes to `System.out.println()`. Already thread-safe (println is synchronized internally).

### 7. `FileSink` — Sink Implementation
- Shared `BufferedWriter` opened once in constructor
- `write()` is `synchronized` to prevent interleaved writes from multiple threads
- `close()` flushes and closes the writer

### 8. `LoggerConfig` — Configuration (Fluent Builder)
Bundles level threshold + sinks + formatter. Built via `LoggerConfig.builder().level().addSink().build()`.
Sinks list is `unmodifiableList` — immutable after construction.

### 9. `Logger` — Core Class
- `config` is `volatile` for cross-thread visibility
- `log()` filters by level, creates `LogMessage`, dispatches to all sinks

### 10. `LogManager` — Singleton Registry (Factory)
- `ConcurrentHashMap` for thread-safe logger storage
- `defaultConfig` is `volatile` for cross-thread visibility
- `computeIfAbsent` for atomic logger creation

### 11. `AsyncSinkDecorator` — Decorator
- Wraps any `Sink` with `ArrayBlockingQueue(1024)` + daemon background thread
- `write()` calls `queue.offer()` — non-blocking for callers
- `LogTask` record captures both message and formatter at write-time (snapshot)
- `close()` sets `running=false`, interrupts worker, drains remaining queue, closes wrapped sink

---

## Thread Safety Summary

| Component | Mechanism | Why |
|-----------|-----------|-----|
| `LogManager.loggers` | `ConcurrentHashMap` | Thread-safe registry, atomic `computeIfAbsent` |
| `LogManager.defaultConfig` | `volatile` | Simple reference swap — visibility only |
| `Logger.config` | `volatile` | Simple reference swap — visibility only |
| `LogMessage` | All fields `final` | Immutable — inherently thread-safe |
| `LoggerConfig` | Unmodifiable list, final fields | Immutable after build |
| `ConsoleSink` | `System.out.println` | Internally synchronized by JVM |
| `FileSink.write()` | `synchronized` method | Prevents interleaved file writes |
| `AsyncSinkDecorator` | `BlockingQueue` + single consumer thread | Producer-consumer — writes are serial |
| `AsyncSinkDecorator.running` | `volatile boolean` | Shutdown flag visible across threads |

---

## Package Structure

```
src/main/java/org/logging/
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

## Usage Example

```java
// Wrap FileSink with async decorator for non-blocking writes
Sink asyncFileSink = new AsyncSinkDecorator(new FileSink("/tmp/app.log"));

LoggerConfig config = LoggerConfig.builder()
    .level(LogLevel.DEBUG)
    .addSink(new ConsoleSink())
    .addSink(asyncFileSink)
    .formatter(new DefaultLogFormatter())
    .build();

LogManager.setDefaultConfig(config);

Logger logger = LogManager.getLogger("OrderService");
logger.info("Order placed for userId=123");
logger.debug("Debug details visible at DEBUG level");
logger.error("Payment gateway timeout");

// Graceful shutdown — drains remaining messages, closes file
asyncFileSink.close();
```

---

## Design Patterns Used

| Pattern           | Where                                    |
|-------------------|------------------------------------------|
| **Singleton**     | `LogManager` — static methods, private constructor |
| **Strategy**      | `Sink` interface, `LogFormatter` interface |
| **Observer**      | Logger → broadcasts to multiple sinks    |
| **Builder**       | `LoggerConfig.builder().level().addSink().build()` |
| **Factory**       | `LogManager.getLogger(name)`             |
| **Decorator**     | `AsyncSinkDecorator` wraps any `Sink`    |
| **Producer-Consumer** | `AsyncSinkDecorator` — BlockingQueue + worker thread |

---

## Possible Extensions

| Extension | Description |
|-----------|-------------|
| **Sink-level filtering** | Different log levels per sink (e.g., ERROR+ to file, DEBUG+ to console) |
| **RotatingFileSink** | Rotate log file when it exceeds a size threshold |
| **Parameterized messages** | `logger.info("Order {} by user {}", orderId, userId)` — avoid string concat if filtered |
| **Structured logging (MDC)** | `ThreadLocal` key-value context attached to every log message |
| **JSON formatter** | `LogFormatter` impl that outputs structured JSON |
