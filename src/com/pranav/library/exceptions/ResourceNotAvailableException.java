package com.pranav.library.exceptions;

public class ResourceNotAvailableException extends LibraryException {
    public ResourceNotAvailableException(String resourceId) {
        super("Resource not available: " + resourceId);
    }
}
