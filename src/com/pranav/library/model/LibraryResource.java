package com.pranav.library.model;

/**
 * Abstract base for every borrowable item in the catalog.
 * availableCopies is mutated under a lock held by the service layer
 * (ResourceManager) — this class only tracks state, it does not
 * synchronize itself, to keep the locking strategy in one place.
 */
public abstract class LibraryResource {

    private final String id;
    private String title;
    private String author;
    private int totalCopies;
    private int availableCopies;

    protected LibraryResource(String id, String title, String author, int totalCopies) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
    }

    public abstract String getType();

    public abstract int getBorrowDurationDays();

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getTotalCopies() { return totalCopies; }
    public int getAvailableCopies() { return availableCopies; }

    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }

    /** Package-private-style mutation, only ResourceManager should call these. */
    public void decrementAvailable() { this.availableCopies--; }
    public void incrementAvailable() {
        if (this.availableCopies < this.totalCopies) {
            this.availableCopies++;
        }
    }
}
