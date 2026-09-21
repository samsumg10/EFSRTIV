package com.cibertec.bbq.security;

import com.cibertec.bbq.exceptions.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Acceso al usuario del token desde los services. */
public final class CurrentUser {

    private CurrentUser() {}

    public static AuthUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser user) {
            return user;
        }
        throw new UnauthorizedException();
    }
}
