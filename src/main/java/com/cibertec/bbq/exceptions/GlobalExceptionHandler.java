package com.cibertec.bbq.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.*;

/**
 * Respuestas de error en JSON: {"message": "...", "errors": {"campo": ["mensaje"]}}.
 * 422 para validaciones (mismo formato que el sistema Laravel).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(fe.getField(), k -> new ArrayList<>()).add(fe.getDefaultMessage());
        }
        return body(HttpStatus.UNPROCESSABLE_CONTENT, "Revisa los datos del formulario.", errors);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        Map<String, List<String>> errors = ex.getField() == null
            ? Map.of() : Map.of(ex.getField(), List.of(ex.getMessage()));
        return body(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), errors);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoSuchElementException ex) {
        return body(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<Map<String, Object>> handleForbidden(RuntimeException ex) {
        String message = ex instanceof ForbiddenException ? ex.getMessage() : "No tienes permiso para esta acción.";
        return body(HttpStatus.FORBIDDEN, message, Map.of());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return body(HttpStatus.UNAUTHORIZED, ex.getMessage(), Map.of());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex) {
        return body(HttpStatus.BAD_REQUEST, "Datos con formato inválido.", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        return body(HttpStatus.CONFLICT, "La operación choca con datos existentes (duplicado o registro relacionado).", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        // Excepciones propias de Spring MVC (404 de recurso, 405, 415...): se respeta su código
        if (ex instanceof ErrorResponse er) {
            HttpStatus status = HttpStatus.resolve(er.getStatusCode().value());
            String message = status == HttpStatus.NOT_FOUND ? "Recurso no encontrado." : "Solicitud no válida.";
            return body(status != null ? status : HttpStatus.BAD_REQUEST, message, Map.of());
        }
        log.error("Error no controlado", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado.", Map.of());
    }

    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String message, Map<String, List<String>> errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("errors", errors);
        return ResponseEntity.status(status).body(body);
    }
}
