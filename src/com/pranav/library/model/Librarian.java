package com.pranav.library.model;

/** The admin role: manages the catalog and user access, does not borrow resources. */
public class Librarian extends User {

    public Librarian(String id, String name, String email, String passwordHash) {
        super(id, name, email, passwordHash);
    }

    @Override
    public Role getRole() { return Role.LIBRARIAN; }

    @Override
    public int getMaxBorrowLimit() { return 0; }

    @Override
    public boolean canManageLibrary() { return true; }
}
