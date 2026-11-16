package com.sentinel;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduled log tailer that tracks byte offsets via RandomAccessFile and ingests new log lines every 100ms.
 */
public final class LogTailer {
    private final String logPath;
    private final LogParser parser;
    private final DetectionEngine engine;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private long fileOffset = 0L;

    public LogTailer(String logPath, LogParser parser, DetectionEngine engine) {
        this.logPath = resolvePath(logPath);
        this.parser = parser;
        this.engine = engine;
    }

    private static String resolvePath(String path) {
        if (Files.exists(Paths.get(path))) return path;
        if (Files.exists(Paths.get("../" + path))) return "../" + path;
        if (Files.exists(Paths.get("demobank/data/auth.log"))) return "demobank/data/auth.log";
        if (Files.exists(Paths.get("../demobank/data/auth.log"))) return "../demobank/data/auth.log";
        return path;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            // Check if file exists, start from end or 0
            File file = new File(logPath);
            if (file.exists()) {
                fileOffset = file.length();
            }
            scheduler.scheduleWithFixedDelay(this::pollLog, 100, 100, TimeUnit.MILLISECONDS);
        }
    }

    public void stop() {
        running.set(false);
        scheduler.shutdownNow();
    }

    public synchronized void resetOffset() {
        fileOffset = 0L;
    }

    private synchronized void pollLog() {
        File file = new File(logPath);
        if (!file.exists()) return;

        long length = file.length();
        if (length < fileOffset) {
            // File was truncated or rotated
            fileOffset = 0L;
        }

        if (length == fileOffset) return;

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(fileOffset);
            String line;
            while ((line = readCompleteLine(raf)) != null) {
                parser.parseLine(line).ifPresent(engine::submit);
                fileOffset = raf.getFilePointer();
            }
        } catch (IOException ignored) {}
    }

    private String readCompleteLine(RandomAccessFile raf) throws IOException {
        long startPos = raf.getFilePointer();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int b;
        boolean hasNewline = false;

        while ((b = raf.read()) != -1) {
            if (b == '\n') {
                hasNewline = true;
                break;
            } else if (b == '\r') {
                // Peek next byte
                int next = raf.read();
                if (next == '\n') {
                    hasNewline = true;
                    break;
                } else if (next != -1) {
                    raf.seek(raf.getFilePointer() - 1);
                }
                hasNewline = true;
                break;
            } else {
                buffer.write(b);
            }
        }

        if (!hasNewline) {
            // Incomplete line, rewind pointer to start of this line
            raf.seek(startPos);
            return null;
        }

        return buffer.toString(StandardCharsets.UTF_8);
    }

    public long getFileOffset() { return fileOffset; }
}
