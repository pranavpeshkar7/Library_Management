package com.pranav.library.model;

/**
 * Abstract base for every account in the system.
 * Kept abstract (not a single class with a Role field) because the three
 * subtypes genuinely differ in behaviour, not just in a label:
 * getMaxBorrowLimit() and canManageLibrary() are different per role,
 * which is exactly the case where subclassing earns its place over a flag.
 */
public abstract class User {

    private final String id;
    private String name;
    private final String email;
    private String passwordHash;

    protected User(String id, String name, String email, String passwordHash) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public abstract Role getRole();

    /** Max number of resources this user type may hold borrowed at once. */
    public abstract int getMaxBorrowLimit();

    /** Whether this user type has librarian/admin privileges. */
    public boolean canManageLibrary() {
        return false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }

    public void setName(String name) { this.name = name; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
