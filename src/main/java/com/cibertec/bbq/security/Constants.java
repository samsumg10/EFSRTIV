package com.cibertec.bbq.security;

import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;

public class Constants {
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String TOKEN_PREFIX         = "Bearer ";
    public static final String SUPER_SECRET_TEXT    = "Bbq!Reservas#Cibertec2026$SecureKey_Must32Chars!";
    public static final long   EXPIRATION_TIME      = 8 * 3_600_000L;   // 8 horas

    public static final String AREA_ADMIN   = "ADMIN";
    public static final String AREA_COMPANY = "COMPANY";

    public static final String ROLE_ADMIN         = "ROLE_ADMIN";
    public static final String ROLE_COMPANY_ADMIN = "ROLE_COMPANY_ADMIN";
    public static final String ROLE_EMPLOYEE      = "ROLE_EMPLOYEE";

    public static Key getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
