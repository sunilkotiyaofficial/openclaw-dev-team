package com.example.knowledgehub.exception;

/**
 * Custom exception — thrown when an entity isn't found.
 *
 * <p>Mapped to HTTP 404 by {@link GlobalExceptionHandler}.</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, Object id) {
        super("%s with id %s not found".formatted(resourceType, id));
    }
}
