package com.pranav.library;

import com.pranav.library.http.Router;
import com.pranav.library.http.StaticFileHandler;
import com.pranav.library.http.handlers.*;
import com.pranav.library.repository.BorrowRecordRepository;
import com.pranav.library.repository.ResourceRepository;
import com.pranav.library.repository.UserRepository;
import com.pranav.library.service.AuthService;
import com.pranav.library.service.ResourceManager;
import com.pranav.library.service.SessionManager;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Entry point. Wires repositories -> services -> handlers -> routes by hand
 * (no @Autowired) and starts com.sun.net.httpserver.HttpServer, the JDK's
 * built-in HTTP server, with a fixed thread pool so each request runs on
 * its own worker thread — this is what makes the concurrent-borrow race
 * condition real rather than simulated.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        String frontendDir = System.getenv().getOrDefault("FRONTEND_DIR", "../frontend");

        // Repositories
        UserRepository userRepository = new UserRepository();
        ResourceRepository resourceRepository = new ResourceRepository();
        BorrowRecordRepository borrowRecordRepository = new BorrowRecordRepository();

        // Services
        SessionManager sessionManager = new SessionManager();
        AuthService authService = new AuthService(userRepository, sessionManager);
        ResourceManager resourceManager = new ResourceManager(resourceRepository, borrowRecordRepository);

        // Handlers
        AuthHandler authHandler = new AuthHandler(authService);
        ResourceHandler resourceHandler = new ResourceHandler(resourceRepository, authService);
        BorrowHandler borrowHandler = new BorrowHandler(resourceManager, borrowRecordRepository, authService);
        AdminHandler adminHandler = new AdminHandler(userRepository, authService);

        // Routes
        Router router = new Router();
        router.register("POST", "/api/auth/signup", authHandler::signup);
        router.register("POST", "/api/auth/login", authHandler::login);
        router.register("POST", "/api/auth/logout", authHandler::logout);

        router.register("GET", "/api/resources", resourceHandler::list);
        router.register("POST", "/api/resources", resourceHandler::add);
        router.register("PUT", "/api/resources", resourceHandler::update);
        router.register("DELETE", "/api/resources", resourceHandler::remove);

        router.register("POST", "/api/borrow", borrowHandler::borrow);
        router.register("POST", "/api/return", borrowHandler::returnResource);
        router.register("GET", "/api/my-borrows", borrowHandler::myBorrows);

        router.register("GET", "/api/admin/users", adminHandler::listUsers);
        router.register("PUT", "/api/admin/users/revoke", adminHandler::revokeAccess);
        router.register("PUT", "/api/admin/users/restore", adminHandler::restoreAccess);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", router);
        server.createContext("/", new StaticFileHandler(new File(frontendDir)));
        server.setExecutor(Executors.newFixedThreadPool(16));
        server.start();

        System.out.println("Library server running on http://localhost:" + port);
        System.out.println("Serving frontend from: " + new File(frontendDir).getAbsolutePath());
    }
}
