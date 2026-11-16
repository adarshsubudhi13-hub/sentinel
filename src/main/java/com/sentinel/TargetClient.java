package com.sentinel;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sends real HTTP authentication requests to DemoBank with spoofed X-Forwarded-For IPs.
 */
public final class TargetClient {
    private static final String ALLOWED_TARGET = "http://127.0.0.1:9090";
    private final String baseUrl;
    private final HttpClient client;

    public TargetClient() {
        this(ALLOWED_TARGET);
    }

    public TargetClient(String baseUrl) {
        validateTarget(baseUrl);
        this.baseUrl = baseUrl;
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(1500))
            .build();
    }

    public static void validateTarget(String url) {
        if (url == null || (!url.equals("http://127.0.0.1:9090") && !url.equals("http://localhost:9090"))) {
            throw new SentinelException.InvalidTargetException(url);
        }
    }

    public int sendLogin(String username, String password, String ip) {
        String json = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/login"))
                .timeout(Duration.ofMillis(2000))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", ip)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode();
        } catch (IOException | InterruptedException e) {
            return 0; // Connection failed / target down
        }
    }

    public String getBaseUrl() { return baseUrl; }
}
