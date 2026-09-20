package com.pranav.library.exceptions;

/**
 * Base checked exception for all library domain errors.
 * Checked (not RuntimeException) on purpose: callers at the HTTP boundary
 * are forced to handle it and turn it into a proper error response,
 * rather than letting a domain failure surface as a raw 500 / stack trace.
 */
public class LibraryException extends Exception {
    public LibraryException(String message) {
        super(message);
    }
}
