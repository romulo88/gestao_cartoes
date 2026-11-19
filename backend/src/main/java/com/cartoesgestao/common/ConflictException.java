package com.cartoesgestao.common;

/** Vira HTTP 409 no {@link GlobalExceptionHandler}. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
