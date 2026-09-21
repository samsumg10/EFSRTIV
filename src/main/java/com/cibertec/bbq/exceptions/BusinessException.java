package com.cibertec.bbq.exceptions;

import lombok.Getter;

/** Regla de negocio incumplida → 422. Si field != null el front muestra el mensaje bajo ese campo. */
@Getter
public class BusinessException extends RuntimeException {

    private final String field;

    public BusinessException(String message) {
        this(null, message);
    }

    public BusinessException(String field, String message) {
        super(message);
        this.field = field;
    }
}
