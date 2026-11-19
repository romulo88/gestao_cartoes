package com.cartoesgestao.common;

/** Vira HTTP 403 no {@link GlobalExceptionHandler}. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
