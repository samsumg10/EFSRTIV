package com.cibertec.bbq.exceptions;

/** Recurso de otra empresa o instalación no asignada → 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException() {
        super("No tienes acceso a este recurso.");
    }

    public ForbiddenException(String message) {
        super(message);
    }
}
