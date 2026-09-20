package com.pranav.library.http.handlers;

import com.pranav.library.exceptions.AuthenticationException;
import com.pranav.library.model.Role;
import com.pranav.library.model.User;
import com.pranav.library.service.AuthService;
import com.pranav.library.util.HttpUtil;
import com.pranav.library.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.util.LinkedHashMap;
import java.util.Map;

public class AuthHandler {

    private final AuthService authService;

    public AuthHandler(AuthService authService) {
        this.authService = authService;
    }

    public void signup(HttpExchange exchange) throws Exception {
        Map<String, String> body = JsonUtil.parse(HttpUtil.readBody(exchange));
        String name = body.get("name");
        String email = body.get("email");
        String password = body.get("password");
        String roleRaw = body.getOrDefault("role", "STUDENT");

        if (isBlank(name) || isBlank(email) || isBlank(password)) {
            HttpUtil.sendError(exchange, 400, "name, email and password are all required");
            return;
        }

        Role role;
        try {
            role = Role.valueOf(roleRaw.toUpperCase());
        } catch (IllegalArgumentException e) {
            HttpUtil.sendError(exchange, 400, "role must be STUDENT, TEACHER or LIBRARIAN");
            return;
        }

        User user = authService.signup(name, email, password, role);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole().name());
        HttpUtil.sendJson(exchange, 201, JsonUtil.toJson(response));
    }

    public void login(HttpExchange exchange) throws Exception {
        Map<String, String> body = JsonUtil.parse(HttpUtil.readBody(exchange));
        String email = body.get("email");
        String password = body.get("password");

        if (isBlank(email) || isBlank(password)) {
            HttpUtil.sendError(exchange, 400, "email and password are required");
            return;
        }

        String token = authService.login(email, password);
        User user = authService.requireUser(token);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", token);
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("role", user.getRole().name());
        HttpUtil.sendJson(exchange, 200, JsonUtil.toJson(response));
    }

    public void logout(HttpExchange exchange) throws Exception {
        authService.logout(HttpUtil.authToken(exchange));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Logged out");
        HttpUtil.sendJson(exchange, 200, JsonUtil.toJson(response));
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
