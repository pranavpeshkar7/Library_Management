package com.pranav.library.exceptions;

/** Thrown when an authenticated user attempts an action their role does not permit. */
public class AuthorizationException extends LibraryException {
    public AuthorizationException(String message) {
        super(message);
    }
}
