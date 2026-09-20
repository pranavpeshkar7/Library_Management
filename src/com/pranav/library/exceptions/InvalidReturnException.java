package com.pranav.library.exceptions;

public class InvalidReturnException extends LibraryException {
    public InvalidReturnException(String userId, String resourceId) {
        super("User " + userId + " has no active borrow record for resource " + resourceId);
    }
}
