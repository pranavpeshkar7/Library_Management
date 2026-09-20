package com.pranav.library.model;

public class Student extends User {

    public Student(String id, String name, String email, String passwordHash) {
        super(id, name, email, passwordHash);
    }

    @Override
    public Role getRole() { return Role.STUDENT; }

    @Override
    public int getMaxBorrowLimit() { return 3; }
}
