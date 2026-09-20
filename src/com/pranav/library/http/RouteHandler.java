package com.pranav.library.http;

import com.sun.net.httpserver.HttpExchange;

/** Functional interface for a single route's logic — lets Router stay a plain dispatch table. */
@FunctionalInterface
public interface RouteHandler {
    void handle(HttpExchange exchange) throws Exception;
}
