package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serves static web assets (HTML, CSS, JS, PWA manifest, service worker).
 */
public class StaticFileHandler implements HttpHandler {

    private final Path staticDir;

    public StaticFileHandler() {
        Path p = Paths.get("static").toAbsolutePath();
        if (!Files.exists(p)) {
            p = Paths.get("campusflow_web/static").toAbsolutePath();
        }
        this.staticDir = p;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path == null || path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        // Prevent path traversal
        if (path.contains("..")) {
            send404(exchange);
            return;
        }

        // Relative file path inside staticDir
        String relative = path.startsWith("/") ? path.substring(1) : path;
        Path target = staticDir.resolve(relative);

        byte[] bytes = null;
        String filename = relative;

        if (Files.exists(target) && !Files.isDirectory(target)) {
            bytes = Files.readAllBytes(target);
            filename = target.getFileName().toString();
        } else {
            // Check classpath resource inside JAR
            InputStream is = getClass().getResourceAsStream("/static/" + relative);
            if (is == null && !relative.contains(".")) {
                is = getClass().getResourceAsStream("/static/index.html");
                filename = "index.html";
            }
            if (is != null) {
                try (InputStream stream = is) {
                    bytes = stream.readAllBytes();
                }
            } else if (!relative.contains(".") && Files.exists(staticDir.resolve("index.html"))) {
                target = staticDir.resolve("index.html");
                bytes = Files.readAllBytes(target);
                filename = "index.html";
            }
        }

        if (bytes == null) {
            send404(exchange);
            return;
        }

        String mimeType = getMimeType(filename);
        exchange.getResponseHeaders().set("Content-Type", mimeType);

        // PWA service worker header
        if (relative.equals("sw.js")) {
            exchange.getResponseHeaders().set("Service-Worker-Allowed", "/");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        } else {
            exchange.getResponseHeaders().set("Cache-Control", "public, max-age=3600");
        }

        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".js")) return "text/javascript; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".webmanifest")) return "application/manifest+json; charset=UTF-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    private void send404(HttpExchange exchange) throws IOException {
        String msg = "404 Not Found";
        byte[] bytes = msg.getBytes("UTF-8");
        exchange.sendResponseHeaders(404, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
