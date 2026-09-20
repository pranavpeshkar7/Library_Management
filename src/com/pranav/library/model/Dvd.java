package com.pranav.library.model;

/** Shorter loan window than a Book — the differing behaviour is why this is a subclass, not a flag on LibraryResource. */
public class Dvd extends LibraryResource {

    public Dvd(String id, String title, String author, int totalCopies) {
        super(id, title, author, totalCopies);
    }

    @Override
    public String getType() { return "DVD"; }

    @Override
    public int getBorrowDurationDays() { return 5; }
}
