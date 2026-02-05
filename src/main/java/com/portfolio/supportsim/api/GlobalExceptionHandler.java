package com.portfolio.supportsim.api;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
    List<Map<String, Object>> errors =
        ex.getConstraintViolations().stream()
            .map(this::toViolationItem)
            .collect(Collectors.toList());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiError.of("validation_error", "Request validation failed", Map.of("errors", errors)));
  }

  private Map<String, Object> toViolationItem(ConstraintViolation<?> violation) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("field", violation.getPropertyPath().toString());
    item.put("message", violation.getMessage());
    return item;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    List<Map<String, Object>> errors =
        ex.getBindingResult().getAllErrors().stream()
            .map(
                error -> {
                  Map<String, Object> item = new LinkedHashMap<>();
                  if (error instanceof FieldError fieldError) {
                    item.put("field", fieldError.getField());
                  }
                  item.put("message", error.getDefaultMessage());
                  return item;
                })
            .collect(Collectors.toList());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiError.of("validation_error", "Request validation failed", Map.of("errors", errors)));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiError.of("invalid_json", "Malformed JSON request"));
  }
}
