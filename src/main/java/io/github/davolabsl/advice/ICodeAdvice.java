package io.github.davolabsl.advice;

import io.github.davolabsl.advice.exception.ExceptionResponse;
import io.github.davolabsl.advice.exception.FasteException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author - Shehara
 */

@ControllerAdvice
public class ICodeAdvice {

    private static final Map<String, Object> response = new HashMap<>();

    @ExceptionHandler(FasteException.class)
    public ResponseEntity<?> handleICodeException(FasteException exception) {
        String message = exception.getMessage();
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(message);
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<?> handleEmptyResultDataAccessException(EmptyResultDataAccessException exception) {
        String message = exception.getMessage();
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(message);
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }
}
