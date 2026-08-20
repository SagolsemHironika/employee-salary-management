package com.acme.salary.common;

/**
 * A request that was valid on its own terms but lost a race against another
 * write. Distinct from {@link InvalidRequestException}: the caller did nothing
 * wrong and retrying may well succeed, so it maps to 409 rather than 400.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
