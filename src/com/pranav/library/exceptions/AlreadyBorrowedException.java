package com.pranav.library.exceptions;

public class AlreadyBorrowedException extends LibraryException {
    public AlreadyBorrowedException(String resourceId) {
        super("You already have this item borrowed. Return it before borrowing it again.");
    }
}
