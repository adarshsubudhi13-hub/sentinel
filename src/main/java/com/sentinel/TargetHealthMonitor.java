package com.sentinel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Periodically polls DemoBank's GET /health endpoint every 2 seconds to monitor target connectivity.
 */
public final class TargetHealthMonitor {
    private final String healthUrl;
    private final HttpClient client;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public TargetHealthMonitor() {
        this("http://127.0.0.1:9090/health");
    }

    public TargetHealthMonitor(String healthUrl) {
        this.healthUrl = healthUrl;
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(1000))
            .build();
    }

    public void start() {
        checkHealth();
        scheduler.scheduleAtFixedRate(this::checkHealth, 2, 2, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    public boolean isConnected() {
        return connected.get();
    }

    private void checkHealth() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(healthUrl))
                .timeout(Duration.ofMillis(1200))
                .GET()
                .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            connected.set(resp.statusCode() == 200);
        } catch (Exception e) {
            connected.set(false);
        }
    }
}
