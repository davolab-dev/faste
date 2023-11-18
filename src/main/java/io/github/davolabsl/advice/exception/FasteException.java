package io.github.davolabsl.advice.exception;

public class FasteException extends RuntimeException {

    public FasteException(String message) {
        super(message);
    }

    public FasteException(String message, Throwable cause) {
        super(message, cause);
    }
}
