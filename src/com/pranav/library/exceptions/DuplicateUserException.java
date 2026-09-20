package com.pranav.library.exceptions;

public class DuplicateUserException extends LibraryException {
    public DuplicateUserException(String email) {
        super("An account with email " + email + " already exists");
    }
}
