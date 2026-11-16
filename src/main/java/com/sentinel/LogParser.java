package com.sentinel;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Parses raw JSON Lines from DemoBank's auth.log into immutable LoginEvent domain records.
 */
public final class LogParser {
    private final Gson gson = new Gson();
    private final AtomicLong malformedLines = new AtomicLong();

    public Optional<LoginEvent> parseLine(String line) {
        if (line == null || line.isBlank()) return Optional.empty();
        try {
            JsonObject json = gson.fromJson(line, JsonObject.class);
            if (json == null || !json.has("ts") || !json.has("ip") || !json.has("username") || !json.has("success")) {
                malformedLines.incrementAndGet();
                return Optional.empty();
            }

            Instant ts;
            try {
                ts = Instant.parse(json.get("ts").getAsString());
            } catch (Exception e) {
                ts = Instant.now();
            }

            String ip = json.get("ip").getAsString();
            String username = json.get("username").getAsString();
            boolean success = json.get("success").getAsBoolean();
            String reason = json.has("reason") ? json.get("reason").getAsString() : (success ? "NONE" : "BAD_PASSWORD");
            int status = json.has("status") ? json.get("status").getAsInt() : (success ? 200 : 401);

            return Optional.of(new LoginEvent(IdGen.next(), ts, ip, username, success, reason, status, null));
        } catch (Exception e) {
            malformedLines.incrementAndGet();
            return Optional.empty();
        }
    }

    public long getMalformedCount() {
        return malformedLines.get();
    }
}
