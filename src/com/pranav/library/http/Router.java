package com.pranav.library.http;

import com.pranav.library.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * A router built by hand, in place of what @GetMapping/@PostMapping would
 * give you in Spring: routes are registered as "METHOD path" -> handler,
 * and this single HttpHandler looks up the right one for each request.
 * Deliberately exact-match (no path variables / wildcards) since every
 * route this API needs is a fixed path with query-string parameters for
 * anything variable (e.g. DELETE /api/resources?id=...).
 */
public class Router implements HttpHandler {

    private final Map<String, RouteHandler> routes = new HashMap<>();

    public void register(String method, String path, RouteHandler handler) {
        routes.put(key(method, path), handler);
    }

    private String key(String method, String path) {
        return method.toUpperCase() + " " + path;
    }

    @Override
    public void handle(HttpExchange exchange) throws java.io.IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // Every response needs the same CORS/JSON headers, and OPTIONS preflight
        // requests need a fast, uniform 204 regardless of which route it targets.
        HttpUtil.applyCommonHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        RouteHandler handler = routes.get(key(method, path));
        if (handler == null) {
            HttpUtil.sendError(exchange, 404, "No such route: " + method + " " + path);
            return;
        }

        try {
            handler.handle(exchange);
        } catch (com.pranav.library.exceptions.AuthenticationException e) {
            HttpUtil.sendError(exchange, 401, e.getMessage());
        } catch (com.pranav.library.exceptions.AuthorizationException e) {
            HttpUtil.sendError(exchange, 403, e.getMessage());
        } catch (com.pranav.library.exceptions.DuplicateUserException
                 | com.pranav.library.exceptions.MaxBorrowLimitExceededException
                 | com.pranav.library.exceptions.InvalidReturnException
                 | com.pranav.library.exceptions.ResourceNotAvailableException
                 | com.pranav.library.exceptions.AlreadyBorrowedException
                 | com.pranav.library.exceptions.ResourceInUseException e) {
            HttpUtil.sendError(exchange, 409, e.getMessage());
        } catch (com.pranav.library.exceptions.UserNotFoundException e) {
            HttpUtil.sendError(exchange, 404, e.getMessage());
        } catch (IllegalArgumentException e) {
            // Malformed JSON, bad numbers, invalid copy counts, etc. are the CLIENT's fault -> 400, not 500.
            HttpUtil.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(); // full detail stays in the server console...
            HttpUtil.sendError(exchange, 500, "Internal server error"); // ...not in the HTTP response
        }
    }
}
