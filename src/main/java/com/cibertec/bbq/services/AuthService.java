package com.cibertec.bbq.services;

import com.cibertec.bbq.exceptions.UnauthorizedException;
import com.cibertec.bbq.models.AppConstants;
import com.cibertec.bbq.models.Employee;
import com.cibertec.bbq.models.DTO.LoginDTO;
import com.cibertec.bbq.models.DTO.LoginResponseDTO;
import com.cibertec.bbq.models.DTO.MeResponseDTO;
import com.cibertec.bbq.repositories.EmployeeRepository;
import com.cibertec.bbq.security.AuthUser;
import com.cibertec.bbq.security.Constants;
import com.cibertec.bbq.security.JWTAuthenticationConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BAD_CREDENTIALS = "Correo o contraseña incorrectos.";

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTAuthenticationConfig jwtConfig;
    private final AccessService accessService;

    @Value("${app.admin.name}")
    private String adminName;
    @Value("${app.admin.email}")
    private String adminEmail;
    @Value("${app.admin.password}")
    private String adminPassword;

    /** Admin de plataforma: credenciales en application.properties (decisión D5). */
    public LoginResponseDTO adminLogin(LoginDTO dto) {
        boolean emailOk = adminEmail.equalsIgnoreCase(dto.getEmail().trim());
        boolean passwordOk = MessageDigest.isEqual(
            adminPassword.getBytes(StandardCharsets.UTF_8), dto.getPassword().getBytes(StandardCharsets.UTF_8));
        if (!emailOk || !passwordOk) throw new UnauthorizedException(BAD_CREDENTIALS);

        AuthUser user = new AuthUser(0L, adminName, adminEmail, Constants.AREA_ADMIN, null, null, null);
        return new LoginResponseDTO(jwtConfig.getJWTToken(user), 0L, adminName, Constants.AREA_ADMIN, null, null, null);
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO companyLogin(LoginDTO dto) {
        Employee e = employeeRepository.findActiveByEmail(dto.getEmail().trim())
            .orElseThrow(() -> new UnauthorizedException(BAD_CREDENTIALS));
        if (!passwordEncoder.matches(dto.getPassword(), e.getPassword())) {
            throw new UnauthorizedException(BAD_CREDENTIALS);
        }
        if (e.getCompany().getDeletedAt() != null
                || !Objects.equals(e.getCompany().getStatus(), AppConstants.COMPANY_ACTIVE)) {
            throw new UnauthorizedException("La empresa está inactiva. Comunícate con el administrador.");
        }
        AuthUser user = new AuthUser(e.getId(), e.getName(), e.getEmail(), Constants.AREA_COMPANY,
            e.getCompany().getId(), e.getCompany().getName(), e.getRole().getType());
        return new LoginResponseDTO(jwtConfig.getJWTToken(user), e.getId(), e.getName(), Constants.AREA_COMPANY,
            e.getCompany().getId(), e.getCompany().getName(), e.getRole().getType());
    }

    @Transactional(readOnly = true)
    public MeResponseDTO me() {
        Employee e = accessService.currentEmployee();
        return new MeResponseDTO(e.getId(), e.getName(), e.getEmail(), e.getPhone(),
            e.getRole().getType(), e.getRole().getName(),
            e.getCompany().getId(), e.getCompany().getName(), e.getCompany().getUuid());
    }
}
