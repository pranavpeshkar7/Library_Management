package com.pranav.library.http.handlers;

import com.pranav.library.model.User;
import com.pranav.library.repository.UserRepository;
import com.pranav.library.service.AuthService;
import com.pranav.library.util.HttpUtil;
import com.pranav.library.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.util.*;

/** All routes here require the caller's session to belong to a Librarian. */
public class AdminHandler {

    private final UserRepository userRepository;
    private final AuthService authService;

    public AdminHandler(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    public void listUsers(HttpExchange exchange) throws Exception {
        User caller = authService.requireUser(HttpUtil.authToken(exchange));
        authService.requireLibrarian(caller);

        List<User> users = userRepository.findAll();
        List<Map<String, Object>> json = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", u.getId());
            entry.put("name", u.getName());
            entry.put("email", u.getEmail());
            entry.put("role", u.getRole().name());
            entry.put("active", userRepository.isActiveById(u.getId()));
            json.add(entry);
        }
        HttpUtil.sendJson(exchange, 200, JsonUtil.toJsonArray(json));
    }

    public void revokeAccess(HttpExchange exchange) throws Exception {
        setActive(exchange, false);
    }

    public void restoreAccess(HttpExchange exchange) throws Exception {
        setActive(exchange, true);
    }

    private void setActive(HttpExchange exchange, boolean active) throws Exception {
        User caller = authService.requireUser(HttpUtil.authToken(exchange));
        authService.requireLibrarian(caller);

        String id = HttpUtil.queryParams(exchange).get("id");
        if (id == null) {
            HttpUtil.sendError(exchange, 400, "id query parameter is required");
            return;
        }
        userRepository.setActive(id, active);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", active ? "Access restored" : "Access revoked");
        HttpUtil.sendJson(exchange, 200, JsonUtil.toJson(response));
    }
}
