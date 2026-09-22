package com.cibertec.bbq.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static com.cibertec.bbq.security.Constants.*;

@Component
public class JWTAuthorizationFilter extends OncePerRequestFilter {

    private Claims parseClaims(String header) {
        String token = header.substring(TOKEN_PREFIX.length());
        return Jwts.parserBuilder().setSigningKey(getSigningKey(SUPER_SECRET_TEXT))
            .build().parseClaimsJws(token).getBody();
    }

    private static Long toLong(Object value) {
        return value instanceof Number n ? n.longValue() : null;
    }

    private static Integer toInteger(Object value) {
        return value instanceof Number n ? n.intValue() : null;
    }

    /** El login no revisa el token: uno vencido no debe impedir iniciar sesión de nuevo. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String uri = req.getRequestURI();
        return "POST".equals(req.getMethod())
            && (uri.equals("/api/admin/login") || uri.equals("/api/company/login"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader(HEADER_AUTHORIZATION);
        if (header == null || !header.startsWith(TOKEN_PREFIX)) {
            SecurityContextHolder.clearContext();
            chain.doFilter(req, res);
            return;
        }
        try {
            Claims claims = parseClaims(header);
            AuthUser user = new AuthUser(
                Long.valueOf(claims.getId()),
                claims.get("name", String.class),
                claims.getSubject(),
                claims.get("area", String.class),
                toLong(claims.get("companyId")),
                claims.get("companyName", String.class),
                toInteger(claims.get("roleType")));
            @SuppressWarnings("unchecked")
            List<String> auths = claims.get("authorities", List.class);
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null,
                    auths == null ? List.of() : auths.stream().map(SimpleGrantedAuthority::new).toList()));
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"message\":\"Sesión expirada o inválida. Vuelve a iniciar sesión.\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
