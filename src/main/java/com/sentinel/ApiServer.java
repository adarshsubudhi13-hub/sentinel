package com.sentinel;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server wrapper binding to 127.0.0.1:8080 using JDK's built-in HttpServer.
 */
public final class ApiServer {
    private final HttpServer server;

    public ApiServer(int port, Router router) throws IOException {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", router::handle);
        server.setExecutor(Executors.newFixedThreadPool(Defaults.THREAD_POOL));
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(1);
    }
}
