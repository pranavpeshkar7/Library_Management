package com.pranav.library.model;

public class Teacher extends User {

    public Teacher(String id, String name, String email, String passwordHash) {
        super(id, name, email, passwordHash);
    }

    @Override
    public Role getRole() { return Role.TEACHER; }

    @Override
    public int getMaxBorrowLimit() { return 6; }
}
