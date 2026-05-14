package com.aeg.core.servicecenter;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildBody(HttpStatus.NOT_FOUND, exception.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(buildBody(HttpStatus.CONFLICT, exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
		Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, "Validation failed");
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			errors.put(fieldError.getField(), fieldError.getDefaultMessage());
		}
		body.put("errors", errors);
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, Object>> handleJsonError(org.springframework.http.converter.HttpMessageNotReadableException exception) {
		return ResponseEntity.badRequest().body(buildBody(HttpStatus.BAD_REQUEST, "Malformed JSON request: " + exception.getMostSpecificCause().getMessage()));
	}

	@ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
	public ResponseEntity<Map<String, Object>> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException exception) {
		String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s", 
				exception.getValue(), exception.getName(), exception.getRequiredType().getSimpleName());
		return ResponseEntity.badRequest().body(buildBody(HttpStatus.BAD_REQUEST, message));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleException(Exception exception) {
		// Log and return the message to help debugging in Postman
		exception.printStackTrace();
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(buildBody(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage() != null ? exception.getMessage() : exception.getClass().getName()));
	}

	private Map<String, Object> buildBody(HttpStatus status, String message) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", OffsetDateTime.now());
		body.put("status", status.value());
		body.put("error", status.getReasonPhrase());
		body.put("message", message);
		return body;
	}
}