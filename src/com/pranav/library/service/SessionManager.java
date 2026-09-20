package com.pranav.library.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal in-memory token store, standing in for what a session/JWT
 * library would give you in a framework. A ConcurrentHashMap is enough
 * here since tokens are simple opaque lookups, not a resource under
 * contention the way borrow counts are.
 */
public class SessionManager {

    private final Map<String, String> tokenToUserId = new ConcurrentHashMap<>();

    public String createSession(String userId) {
        String token = UUID.randomUUID().toString();
        tokenToUserId.put(token, userId);
        return token;
    }

    public String getUserId(String token) {
        return token == null ? null : tokenToUserId.get(token);
    }

    public void invalidate(String token) {
        if (token != null) tokenToUserId.remove(token);
    }
}
