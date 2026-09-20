package com.pranav.library.model;

public class Book extends LibraryResource {

    public Book(String id, String title, String author, int totalCopies) {
        super(id, title, author, totalCopies);
    }

    @Override
    public String getType() { return "BOOK"; }

    @Override
    public int getBorrowDurationDays() { return 14; }
}
