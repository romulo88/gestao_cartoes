package com.cartoesgestao.common;

/** Vira HTTP 404 no {@link GlobalExceptionHandler}. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
