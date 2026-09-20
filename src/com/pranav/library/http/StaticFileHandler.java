package com.pranav.library.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Serves the frontend's static files from disk. Having the same Java
 * process serve both /api/* and the HTML/CSS/JS avoids CORS entirely and
 * means there's exactly one thing to run — no separate XAMPP-style server
 * needed for the frontend.
 */
public class StaticFileHandler implements HttpHandler {

    private final File rootDir;

    public StaticFileHandler(File rootDir) {
        this.rootDir = rootDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";

        File file = new File(rootDir, path).getCanonicalFile();
        // Prevent path traversal outside the frontend directory.
        if (!file.getPath().startsWith(rootDir.getCanonicalPath())) {
            exchange.sendResponseHeaders(403, -1);
            return;
        }

        if (!file.exists() || file.isDirectory()) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", contentType(file.getName()));
        byte[] bytes = Files.readAllBytes(file.toPath());
        exchange.sendResponseHeaders(200, bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String contentType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html; charset=utf-8";
        if (fileName.endsWith(".css")) return "text/css; charset=utf-8";
        if (fileName.endsWith(".js")) return "application/javascript; charset=utf-8";
        return "application/octet-stream";
    }
}
