package com.pulseping;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PingWorker {

    static class Service {
        String id;
        String name;
        String url;

        Service(String id, String name, String url) {
            this.id = id;
            this.name = name;
            this.url = url;
        }
    }

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) throws IOException {
        List<Service> services = List.of(
                new Service("api-gateway", "API Gateway", "https://api.github.com"),
                new Service("web-frontend", "Web Frontend", "https://google.com")
        );

        // Start Monitoring Thread
        new Thread(() -> runMonitoringLoop(services)).start();

        // Start Health Check Server
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/", (t) -> {
            String response = "PulsePing Java Worker is active!";
            t.sendResponseHeaders(200, response.length());
            try (OutputStream os = t.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        server.createContext("/health", (t) -> {
            String response = "OK";
            t.sendResponseHeaders(200, response.length());
            try (OutputStream os = t.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        System.out.println("🌍 Web server started on port " + port);
        server.start();
    }

    private static void runMonitoringLoop(List<Service> services) {
        System.out.println("🚀 PulsePing Java Worker started. Monitoring " + services.size() + " services...");
        ExecutorService executor = Executors.newFixedThreadPool(services.size());

        while (true) {
            for (Service service : services) {
                executor.submit(() -> ping(service));
            }

            try {
                System.out.println("--------------------------------------------------");
                TimeUnit.SECONDS.sleep(15);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void ping(Service service) {
        long start = System.currentTimeMillis();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(service.url))
                .header("User-Agent", "PulsePing-Monitor/1.0")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - start;

            if (response.statusCode() < 400) {
                System.out.printf("[%s] %s: online (%dms)%n", dtf.format(LocalDateTime.now()), service.id, latency);
            } else {
                System.out.printf("[%s] %s: offline (HTTP %d)%n", dtf.format(LocalDateTime.now()), service.id, response.statusCode());
            }
        } catch (Exception e) {
            System.out.printf("[%s] %s: offline (Error: %s)%n", dtf.format(LocalDateTime.now()), service.id, e.getMessage());
        }
    }
}
