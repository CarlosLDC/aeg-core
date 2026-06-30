package com.aeg.core.security;

import com.aeg.core.enajenacion.mqtt.EnajenacionProtocolException;
import com.aeg.core.servicecenter.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import org.hibernate.PropertyAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
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
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
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
        String message = mapBusinessMessage(e.getMessage());
        boolean isConflict = message.toLowerCase().contains("ya ")
                || (e.getMessage() != null && (e.getMessage().toLowerCase().contains("exists")
                        || e.getMessage().toLowerCase().contains("already")));
        HttpStatus status = isConflict ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        return buildResponse(status, isConflict ? "Conflicto de datos" : "Petición inválida", message);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        String message = e.getMessage() != null && !e.getMessage().isBlank()
                ? e.getMessage()
                : "El servicio no está configurado correctamente.";
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Servicio no disponible", message);
    }

    @ExceptionHandler(EnajenacionProtocolException.class)
    public ResponseEntity<Map<String, Object>> handleEnajenacionProtocol(EnajenacionProtocolException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Petición inválida", e.getMessage());
    }

    @ExceptionHandler({PersistenceException.class, JpaSystemException.class, PropertyAccessException.class})
    public ResponseEntity<Map<String, Object>> handlePersistence(Exception e) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "Conflicto de datos",
                mapPersistenceMessage(e));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        if (message.contains("distribuidores_id_empleado_fkey")) {
            return buildResponse(HttpStatus.CONFLICT, "Conflicto de datos",
                    "No se puede eliminar el empleado porque está registrado como persona distribuidor. Quita ese rol antes de eliminarlo.");
        }
        if (message.contains("tecnicos_id_empleado_fkey")) {
            return buildResponse(HttpStatus.CONFLICT, "Conflicto de datos",
                    "No se puede eliminar el empleado porque está registrado como técnico. Quita ese rol antes de eliminarlo.");
        }
        if (message.contains("inspecciones_anuales_id_empleado_fkey")) {
            return buildResponse(HttpStatus.CONFLICT, "Conflicto de datos",
                    "No se puede eliminar el empleado porque tiene inspecciones anuales registradas.");
        }
        if (message.contains("fk_users_branch") || message.contains("users_branch_id_fkey")) {
            return buildResponse(HttpStatus.CONFLICT, "Conflicto de datos",
                    "No se puede eliminar la sucursal porque tiene usuarios asociados. Reasigna o elimina esos usuarios primero.");
        }
        if (message.contains("modification_requests_requested_by_fkey")) {
            return buildResponse(HttpStatus.CONFLICT, "Conflicto de datos",
                    "No se puede eliminar el usuario porque tiene solicitudes de modificación registradas.");
        }
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

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return buildResponse(status, status.getReasonPhrase(), message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception e) {
        log.error("Unhandled exception", e);
        String friendly = mapPersistenceMessage(e);
        if (friendly.contains("sucursal")) {
            return buildResponse(HttpStatus.CONFLICT, "Conflicto de datos", friendly);
        }
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor",
                "No se pudo completar la operación. Inténtalo de nuevo más tarde.");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(buildBody(status, error, message));
    }

    private static String mapBusinessMessage(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Petición inválida.";
        }
        if (raw.contains("branch already linked to another distributor")) {
            return "Esta sucursal ya es cliente de otra distribuidora.";
        }
        if (raw.contains("rif already exists")) {
            return raw;
        }
        if (raw.contains("employee has annual inspections and cannot be deleted")) {
            return "No se puede eliminar el empleado porque tiene inspecciones anuales registradas.";
        }
        if (raw.contains("employee has a pending review request")) {
            return "El empleado tiene una solicitud pendiente de aprobación.";
        }
        if (raw.contains("modification request is no longer pending")) {
            return "La solicitud ya fue procesada.";
        }
        if (raw.contains("employee is not in pending review state")) {
            return "El empleado no está bloqueado para revisión.";
        }
        if (raw.contains("client has printers and cannot be deleted")) {
            return "No se puede eliminar el cliente porque tiene impresoras vinculadas.";
        }
        if (raw.contains("client has a pending review request")) {
            return "El cliente tiene una solicitud pendiente de aprobación.";
        }
        if (raw.contains("client is not in pending review state")) {
            return "El cliente no está bloqueado para revisión.";
        }
        if (raw.contains("client updates must be requested for review")) {
            return "Los cambios de clientes requieren solicitud de revisión.";
        }
        if (raw.contains("branch has linked users and cannot be deleted")) {
            return "No se puede eliminar la sucursal porque tiene usuarios asociados. Reasigna o elimina esos usuarios primero.";
        }
        if (raw.contains("branch has linked employees and cannot be deleted")) {
            return "No se puede eliminar la sucursal porque tiene empleados asociados.";
        }
        if (raw.contains("branch is not registered as distributor")) {
            return "La sucursal no tiene rol de distribuidor. Asígnalo en Sucursales antes de vincular el usuario.";
        }
        if (raw.contains("distributorId does not match distributor on branch")) {
            return "El distribuidor seleccionado no corresponde a la sucursal.";
        }
        if (raw.contains("branch is not registered as service center")) {
            return "La sucursal no tiene rol de centro de servicio. Asígnalo en Sucursales.";
        }
        if (raw.contains("role requires branch")) {
            return "Selecciona una sucursal para este rol.";
        }
        return raw;
    }

    private static String mapPersistenceMessage(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String raw = current.getMessage();
            if (raw != null && raw.contains("Binding property is null")) {
                return "La sucursal ya existe pero falta completar el vínculo como cliente. Pulsa «Registrar» de nuevo.";
            }
            current = current.getCause();
        }
        return "No se pudo completar la operación. Revisa los datos o inténtalo de nuevo.";
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
