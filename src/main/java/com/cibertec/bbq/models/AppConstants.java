package com.cibertec.bbq.models;

import java.util.List;

/** Constantes de negocio (equivalente a config/constants.php del sistema Laravel). */
public final class AppConstants {

    private AppConstants() {}

    // roles.type
    public static final int ROLE_TYPE_COMPANY_ADMIN = 1;
    public static final int ROLE_TYPE_EMPLOYEE      = 2;

    // companies.status
    public static final int COMPANY_INACTIVE = 0;
    public static final int COMPANY_ACTIVE   = 1;

    // sales.status
    public static final int SALE_PENDING   = 1;
    public static final int SALE_PAID      = 2;
    public static final int SALE_CANCELLED = 3;
    public static final int SALE_EXPIRED   = 4;
    /** Estados que ocupan capacidad. */
    public static final List<Integer> ACTIVE_SALE_STATUSES = List.of(SALE_PENDING, SALE_PAID);

    // Ventana de reserva (RN-01) y plazos
    public static final int BOOKING_MIN_DAYS_AHEAD      = 2;
    public static final int BOOKING_MAX_DAYS_AHEAD      = 60;
    public static final int PAYMENT_DEADLINE_DAYS       = 3;
    public static final int FULLY_BOOKED_LOOKAHEAD_DAYS = 90;
}
