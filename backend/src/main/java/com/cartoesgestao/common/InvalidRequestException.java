package com.cartoesgestao.common;

/** Vira HTTP 400 no {@link GlobalExceptionHandler}. */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
