package com.cibertec.bbq.security;

import com.cibertec.bbq.models.AppConstants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.context.annotation.Configuration;

import java.util.Date;
import java.util.List;

import static com.cibertec.bbq.security.Constants.*;

@Configuration
public class JWTAuthenticationConfig {

    /** Devuelve el token sin el prefijo "Bearer ". */
    public String getJWTToken(AuthUser user) {
        String roleName;
        if (AREA_ADMIN.equals(user.area())) {
            roleName = ROLE_ADMIN;
        } else if (user.roleType() != null && user.roleType() == AppConstants.ROLE_TYPE_COMPANY_ADMIN) {
            roleName = ROLE_COMPANY_ADMIN;
        } else {
            roleName = ROLE_EMPLOYEE;
        }
        return Jwts.builder()
            .setId(String.valueOf(user.id()))
            .setSubject(user.email())
            .claim("name", user.name())
            .claim("area", user.area())
            .claim("companyId", user.companyId())
            .claim("companyName", user.companyName())
            .claim("roleType", user.roleType())
            .claim("authorities", List.of(roleName))
            .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
            .signWith(getSigningKey(SUPER_SECRET_TEXT), SignatureAlgorithm.HS256)
            .compact();
    }
}
