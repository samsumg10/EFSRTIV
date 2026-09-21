package com.cibertec.bbq.security;

/**
 * Usuario autenticado (principal del SecurityContext), armado desde los claims del JWT.
 * Para el admin de plataforma: id = 0, companyId = null, roleType = null.
 */
public record AuthUser(Long id, String name, String email, String area,
                       Long companyId, String companyName, Integer roleType) {

    public boolean isCompanyAdmin() {
        return roleType != null && roleType == com.cibertec.bbq.models.AppConstants.ROLE_TYPE_COMPANY_ADMIN;
    }
}
