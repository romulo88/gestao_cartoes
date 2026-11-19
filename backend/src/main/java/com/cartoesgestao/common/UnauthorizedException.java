package com.cartoesgestao.common;

/** Vira HTTP 401 no {@link GlobalExceptionHandler}. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
