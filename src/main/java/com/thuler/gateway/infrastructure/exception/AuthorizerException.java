package com.thuler.gateway.infrastructure.exception;

public class AuthorizerException extends RuntimeException {

    public AuthorizerException(String message) {
        super(message);
    }

    public AuthorizerException(String message, Throwable cause) {
        super(message, cause);
    }
}
