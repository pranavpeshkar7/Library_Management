package com.pranav.library.model;

import java.time.LocalDate;

public class BorrowRecord {
    private final String id;
    private final String userId;
    private final String resourceId;
    private final LocalDate borrowedOn;
    private final LocalDate dueDate;
    private LocalDate returnedOn; // null while still borrowed

    public BorrowRecord(String id, String userId, String resourceId, LocalDate borrowedOn, LocalDate dueDate) {
        this.id = id;
        this.userId = userId;
        this.resourceId = resourceId;
        this.borrowedOn = borrowedOn;
        this.dueDate = dueDate;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getResourceId() { return resourceId; }
    public LocalDate getBorrowedOn() { return borrowedOn; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnedOn() { return returnedOn; }
    public boolean isActive() { return returnedOn == null; }

    public void markReturned(LocalDate date) { this.returnedOn = date; }
}
