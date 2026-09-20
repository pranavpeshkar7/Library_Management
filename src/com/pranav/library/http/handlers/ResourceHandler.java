package com.pranav.library.http.handlers;

import com.pranav.library.model.*;
import com.pranav.library.repository.ResourceRepository;
import com.pranav.library.service.AuthService;
import com.pranav.library.util.HttpUtil;
import com.pranav.library.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.util.*;

/** GET is open to any logged-in user; POST/PUT/DELETE are librarian-only (enforced via AuthService.requireLibrarian). */
public class ResourceHandler {

    private final ResourceRepository resourceRepository;
    private final AuthService authService;

    public ResourceHandler(ResourceRepository resourceRepository, AuthService authService) {
        this.resourceRepository = resourceRepository;
        this.authService = authService;
    }

    public void list(HttpExchange exchange) throws Exception {
        authService.requireUser(HttpUtil.authToken(exchange)); // must be logged in, any role
        List<LibraryResource> resources = resourceRepository.findAll();

        List<Map<String, Object>> json = new ArrayList<>();
        for (LibraryResource r : resources) {
            json.add(toJson(r));
        }
        HttpUtil.sendJson(exchange, 200, JsonUtil.toJsonArray(json));
    }

    public void add(HttpExchange exchange) throws Exception {
        User user = authService.requireUser(HttpUtil.authToken(exchange));
        authService.requireLibrarian(user);

        Map<String, String> body = JsonUtil.parse(HttpUtil.readBody(exchange));
        String type = body.getOrDefault("type", "BOOK").toUpperCase();
        String title = body.get("title");
        String author = body.get("author");
        int totalCopies = parseIntOrDefault(body.get("totalCopies"), 1);

        if (title == null || title.trim().isEmpty()) {
            HttpUtil.sendError(exchange, 400, "title is required");
            return;
        }

        String id = UUID.randomUUID().toString();
        LibraryResource resource = "DVD".equals(type)
                ? new Dvd(id, title, author, totalCopies)
                : new Book(id, title, author, totalCopies);

        resourceRepository.save(resource);
        HttpUtil.sendJson(exchange, 201, JsonUtil.toJson(toJson(resource)));
    }

    public void update(HttpExchange exchange) throws Exception {
        User user = authService.requireUser(HttpUtil.authToken(exchange));
        authService.requireLibrarian(user);

        String id = HttpUtil.queryParams(exchange).get("id");
        if (id == null) {
            HttpUtil.sendError(exchange, 400, "id query parameter is required");
            return;
        }

        Optional<LibraryResource> existing = resourceRepository.findById(id);
        if (!existing.isPresent()) {
            HttpUtil.sendError(exchange, 404, "No resource with id " + id);
            return;
        }

        Map<String, String> body = JsonUtil.parse(HttpUtil.readBody(exchange));
        LibraryResource resource = existing.get();
        if (body.containsKey("title")) resource.setTitle(body.get("title"));
        if (body.containsKey("author")) resource.setAuthor(body.get("author"));
        if (body.containsKey("totalCopies")) resource.setTotalCopies(parseIntOrDefault(body.get("totalCopies"), resource.getTotalCopies()));

        resourceRepository.save(resource);
        HttpUtil.sendJson(exchange, 200, JsonUtil.toJson(toJson(resource)));
    }

    public void remove(HttpExchange exchange) throws Exception {
        User user = authService.requireUser(HttpUtil.authToken(exchange));
        authService.requireLibrarian(user);

        String id = HttpUtil.queryParams(exchange).get("id");
        if (id == null) {
            HttpUtil.sendError(exchange, 400, "id query parameter is required");
            return;
        }
        resourceRepository.deleteById(id);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Resource removed");
        HttpUtil.sendJson(exchange, 200, JsonUtil.toJson(response));
    }

    private Map<String, Object> toJson(LibraryResource r) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", r.getId());
        json.put("type", r.getType());
        json.put("title", r.getTitle());
        json.put("author", r.getAuthor());
        json.put("totalCopies", r.getTotalCopies());
        json.put("availableCopies", r.getAvailableCopies());
        json.put("borrowDurationDays", r.getBorrowDurationDays());
        return json;
    }

    private int parseIntOrDefault(String s, int fallback) {
        try {
            return s == null ? fallback : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
