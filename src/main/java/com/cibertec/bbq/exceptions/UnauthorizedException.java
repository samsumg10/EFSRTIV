package com.cibertec.bbq.exceptions;

/** Sin sesión válida (o la empresa/empleado ya no existe o está inactiva) → 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException() {
        super("Debes iniciar sesión.");
    }

    public UnauthorizedException(String message) {
        super(message);
    }
}
