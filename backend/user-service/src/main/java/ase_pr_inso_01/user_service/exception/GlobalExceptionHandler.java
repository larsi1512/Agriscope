package ase_pr_inso_01.user_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
  /**
   * Handles any exception that extends ErrorListException (ValidationException, ConflictException, etc.)
   */
  @ExceptionHandler(ErrorListException.class)
  public ResponseEntity<Map<String, Object>> handleErrorListException(ErrorListException ex) {

    Map<String, Object> body = new HashMap<>();

    body.put("message", ex.summary());

    body.put("errors", ex.getErrors());

    HttpStatus status = HttpStatus.BAD_REQUEST;

    if (ex instanceof ConflictException) {
      status = HttpStatus.CONFLICT;
    } else if (ex instanceof ValidationException) {
      status = HttpStatus.UNPROCESSABLE_ENTITY;
    }

    return ResponseEntity.status(status).body(body);
  }
}