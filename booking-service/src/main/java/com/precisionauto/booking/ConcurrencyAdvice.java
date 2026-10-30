package com.precisionauto.booking;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ConcurrencyAdvice {
    /** Two clients raced for the same bay; the loser gets a clean 409 instead of a 500. */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<Map<String, String>> conflict(ConcurrencyFailureException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Slot was just taken by another booking, please pick another"));
    }
}
