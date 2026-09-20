package com.pranav.library.http.handlers;

import com.pranav.library.model.BorrowRecord;
import com.pranav.library.model.User;
import com.pranav.library.repository.BorrowRecordRepository;
import com.pranav.library.service.AuthService;
import com.pranav.library.service.ResourceManager;
import com.pranav.library.util.HttpUtil;
import com.pranav.library.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.util.*;

public class BorrowHandler {

    private final ResourceManager resourceManager;
    private final BorrowRecordRepository borrowRecordRepository;
    private final AuthService authService;

    public BorrowHandler(ResourceManager resourceManager, BorrowRecordRepository borrowRecordRepository, AuthService authService) {
        this.resourceManager = resourceManager;
        this.borrowRecordRepository = borrowRecordRepository;
        this.authService = authService;
    }

    public void borrow(HttpExchange exchange) throws Exception {
        User user = authService.requireUser(HttpUtil.authToken(exchange));
        Map<String, String> body = JsonUtil.parse(HttpUtil.readBody(exchange));
        String resourceId = body.get("resourceId");
        if (resourceId == null) {
            HttpUtil.sendError(exchange, 400, "resourceId is required");
            return;
        }

        BorrowRecord record = resourceManager.borrow(user, resourceId);
        HttpUtil.sendJson(exchange, 201, JsonUtil.toJson(toJson(record)));
    }

    public void returnResource(HttpExchange exchange) throws Exception {
        User user = authService.requireUser(HttpUtil.authToken(exchange));
        Map<String, String> body = JsonUtil.parse(HttpUtil.readBody(exchange));
        String resourceId = body.get("resourceId");
        if (resourceId == null) {
            HttpUtil.sendError(exchange, 400, "resourceId is required");
            return;
        }

        BorrowRecord record = resourceManager.returnResource(user, resourceId);
        HttpUtil.sendJson(exchange, 200, JsonUtil.toJson(toJson(record)));
    }

    public void myBorrows(HttpExchange exchange) throws Exception {
        User user = authService.requireUser(HttpUtil.authToken(exchange));
        List<BorrowRecord> records = borrowRecordRepository.findByUser(user.getId());

        List<Map<String, Object>> json = new ArrayList<>();
        for (BorrowRecord r : records) json.add(toJson(r));
        HttpUtil.sendJson(exchange, 200, JsonUtil.toJsonArray(json));
    }

    private Map<String, Object> toJson(BorrowRecord r) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", r.getId());
        json.put("resourceId", r.getResourceId());
        json.put("borrowedOn", r.getBorrowedOn().toString());
        json.put("dueDate", r.getDueDate().toString());
        json.put("returnedOn", r.getReturnedOn() != null ? r.getReturnedOn().toString() : null);
        json.put("active", r.isActive());
        return json;
    }
}
