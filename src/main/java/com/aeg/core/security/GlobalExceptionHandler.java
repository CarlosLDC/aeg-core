package com.aeg.core.security;

import com.aeg.core.servicecenter.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException e) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Autenticación fallida", "Usuario o contraseña incorrectos");
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UsernameNotFoundException e) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Usuario no encontrado", e.getMessage());
    }

    @ExceptionHandler({ResourceNotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(Exception e) {
        return buildResponse(HttpStatus.NOT_FOUND, "Recurso no encontrado", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, "Validación fallida", null);
        
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());
        
        body.put("details", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException e) {
        return buildResponse(HttpStatus.FORBIDDEN, "Acceso denegado", mapAccessDeniedMessage(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        boolean isConflict = e.getMessage().toLowerCase().contains("exists") || e.getMessage().toLowerCase().contains("existe");
        HttpStatus status = isConflict ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        return buildResponse(status, isConflict ? "Conflicto de datos" : "Petición inválida", e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        if (message.contains("violates foreign key constraint") || message.contains("fk_")) {
            return buildResponse(HttpStatus.BAD_REQUEST, "Error de integridad referencial", 
                "No se puede realizar la operación porque el registro está siendo referenciado o hace referencia a un registro inexistente.");
        }
        if (message.contains("duplicate key value") || message.contains("unique constraint")) {
            return buildResponse(HttpStatus.CONFLICT, "Registro duplicado", 
                "Ya existe un registro con estos datos únicos.");
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Error de integridad de datos", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonError(HttpMessageNotReadableException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, "JSON mal formado", e.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = String.format("Valor inválido '%s' para el parámetro '%s'. Se esperaba tipo: %s", 
                e.getValue(), e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "desconocido");
        return buildResponse(HttpStatus.BAD_REQUEST, "Error de tipo de parámetro", message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception e) {
        e.printStackTrace(); // Keep for debugging in logs
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", e.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(buildBody(status, error, message));
    }

    private static String mapAccessDeniedMessage(String raw) {
        if (raw == null || raw.isBlank()) {
            return "No tienes permiso para realizar esta acción.";
        }
        if (raw.contains("Not allowed to access branch id:")) {
            return "No tienes permiso sobre esa sucursal.";
        }
        if (raw.contains("Branch already assigned to another distributor")) {
            return "Esta sucursal ya es cliente de otra distribuidora.";
        }
        if (raw.contains("Not allowed to create client for this distributor")) {
            return "No puedes registrar clientes para otra distribuidora.";
        }
        return raw;
    }

    private Map<String, Object> buildBody(HttpStatus status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        if (message != null) {
            body.put("message", message);
        }
        return body;
    }
}
