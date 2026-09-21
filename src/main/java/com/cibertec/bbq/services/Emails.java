package com.cibertec.bbq.services;

/** RN-14: al eliminar se renombra el email ("correo-id") para que el UNIQUE permita reutilizarlo. */
final class Emails {

    private static final int MAX_LENGTH = 150;

    private Emails() {}

    static String release(String email, Long id) {
        String suffix = "-" + id;
        String base = email.length() + suffix.length() > MAX_LENGTH
            ? email.substring(0, MAX_LENGTH - suffix.length()) : email;
        return base + suffix;
    }

    static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
