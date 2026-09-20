package com.pranav.library.exceptions;

public class ResourceInUseException extends LibraryException {
    public ResourceInUseException(String resourceId, int onLoan) {
        super("Cannot remove this item: " + onLoan + " cop" + (onLoan == 1 ? "y is" : "ies are") + " currently on loan");
    }
}
