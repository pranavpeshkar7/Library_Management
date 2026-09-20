package com.pranav.library.exceptions;

public class MaxBorrowLimitExceededException extends LibraryException {
    public MaxBorrowLimitExceededException(String userId, int limit) {
        super("User " + userId + " has reached their borrow limit of " + limit);
    }
}
