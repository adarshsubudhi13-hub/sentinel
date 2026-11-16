package com.sentinel;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Executes administrative containment actions on DemoBank via authenticated admin API.
 */
public final class DefenseClient {
    private final String baseUrl;
    private final String adminToken;
    private final HttpClient client;

    public DefenseClient() {
        this("http://127.0.0.1:9090", "demo-admin-token");
    }

    public DefenseClient(String baseUrl, String adminToken) {
        TargetClient.validateTarget(baseUrl);
        this.baseUrl = baseUrl;
        this.adminToken = adminToken;
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(1500))
            .build();
    }

    public boolean blockIp(String ip) {
        return postAdmin("/admin/block", String.format("{\"ip\":\"%s\"}", ip));
    }

    public boolean unblockIp(String ip) {
        return postAdmin("/admin/unblock", String.format("{\"ip\":\"%s\"}", ip));
    }

    public boolean lockUser(String username, int seconds) {
        return postAdmin("/admin/lock-user", String.format("{\"username\":\"%s\",\"seconds\":%d}", username, seconds));
    }

    public boolean unlockUser(String username) {
        return postAdmin("/admin/unlock-user", String.format("{\"username\":\"%s\"}", username));
    }

    public boolean resetTarget() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/admin/reset"))
                .timeout(Duration.ofMillis(2000))
                .header("X-Admin-Token", adminToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean postAdmin(String endpoint, String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .timeout(Duration.ofMillis(2000))
                .header("Content-Type", "application/json")
                .header("X-Admin-Token", adminToken)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
