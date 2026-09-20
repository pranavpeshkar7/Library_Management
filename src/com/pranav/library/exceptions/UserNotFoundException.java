package com.pranav.library.exceptions;

public class UserNotFoundException extends LibraryException {
    public UserNotFoundException(String identifier) {
        super("No user found for: " + identifier);
    }
}
