package com.sentinel;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.sun.net.httpserver.HttpExchange;

/**
 * Server-Sent Events (SSE) broadcaster and AlertListener implementation.
 */
public final class SseBroadcaster implements AlertListener {
    private final List<HttpExchange> clients = new CopyOnWriteArrayList<>();

    public void addClient(HttpExchange exchange) {
        try {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Connection", "keep-alive");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, 0);
            clients.add(exchange);
            sendRaw(exchange, "event: connected\ndata: {\"status\":\"ok\"}\n\n");
        } catch (IOException e) {
            // connection dropped
        }
    }

    @Override
    public void onAlert(Alert alert) {
        String data = String.format("{\"type\":\"ALERT\",\"id\":\"%s\",\"rule\":\"%s\",\"severity\":\"%s\",\"ip\":\"%s\",\"mitre\":\"%s\"}",
            alert.id(), alert.ruleName(), alert.severity(), alert.ip(), alert.mitreTag());
        broadcast("alert", data);
    }

    public void broadcast(String eventType, String jsonData) {
        String msg = "event: " + eventType + "\ndata: " + jsonData + "\n\n";
        for (HttpExchange client : clients) {
            if (!sendRaw(client, msg)) {
                clients.remove(client);
            }
        }
    }

    private boolean sendRaw(HttpExchange exchange, String message) {
        try {
            OutputStream os = exchange.getResponseBody();
            os.write(message.getBytes(StandardCharsets.UTF_8));
            os.flush();
            return true;
        } catch (IOException e) {
            try { exchange.close(); } catch (Exception ignored) {}
            return false;
        }
    }

    public int getClientCount() {
        return clients.size();
    }
}
