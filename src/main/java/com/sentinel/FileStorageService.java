package com.sentinel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/**
 * File-backed StorageService using JSONL format.
 * Demonstrates try-with-resources and NIO Path APIs.
 */
public final class FileStorageService implements StorageService {
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Instant.class,
            (JsonSerializer<Instant>) (src, t, ctx) -> ctx.serialize(src.toString()))
        .registerTypeAdapter(Instant.class,
            (JsonDeserializer<Instant>) (json, t, ctx) -> {
                try {
                    return Instant.parse(json.getAsString());
                } catch (Exception e) {
                    return Instant.ofEpochMilli(json.getAsLong());
                }
            })
        .create();

    @Override
    public void saveAlert(Alert a) { appendJsonl(Defaults.ALERTS_FILE, GSON.toJson(a)); }

    @Override
    public List<Alert> loadAlerts() { return readJsonl(Defaults.ALERTS_FILE, Alert.class); }

    @Override
    public void saveTimeline(TimelineEntry e) { appendJsonl(Defaults.TIMELINE_FILE, GSON.toJson(e)); }

    @Override
    public List<TimelineEntry> loadTimeline() { return readJsonl(Defaults.TIMELINE_FILE, TimelineEntry.class); }

    @Override
    public void saveBlocklist(Set<String> ips) {
        try { Files.write(Path.of(Defaults.BLOCKLIST_FILE), ips); }
        catch (IOException e) { System.err.println("saveBlocklist: " + e.getMessage()); }
    }

    @Override
    public Set<String> loadBlocklist() {
        try {
            Path p = Path.of(Defaults.BLOCKLIST_FILE);
            if (!Files.exists(p)) return new HashSet<>();
            return new HashSet<>(Files.readAllLines(p));
        } catch (IOException e) { return new HashSet<>(); }
    }

    @Override
    public void clearAll() {
        for (String file : List.of(Defaults.ALERTS_FILE, Defaults.TIMELINE_FILE, Defaults.BLOCKLIST_FILE)) {
            try { Files.deleteIfExists(Path.of(file)); }
            catch (IOException ignored) {}
        }
    }

    private static void appendJsonl(String file, String json) {
        try (BufferedWriter w = Files.newBufferedWriter(Path.of(file),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(json); w.newLine();
        } catch (IOException e) { System.err.println("appendJsonl: " + e.getMessage()); }
    }

    private static <T> List<T> readJsonl(String file, Class<T> cls) {
        List<T> list = new ArrayList<>();
        Path p = Path.of(file);
        if (!Files.exists(p)) return list;
        try (BufferedReader r = Files.newBufferedReader(p)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.isBlank()) list.add(GSON.fromJson(line, cls));
            }
        } catch (IOException e) { System.err.println("readJsonl: " + e.getMessage()); }
        return list;
    }
}
