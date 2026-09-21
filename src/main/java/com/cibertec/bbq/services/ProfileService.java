package com.cibertec.bbq.services;

import com.cibertec.bbq.exceptions.BusinessException;
import com.cibertec.bbq.models.Company;
import com.cibertec.bbq.models.DTO.ProfileRequestDTO;
import com.cibertec.bbq.models.DTO.ProfileResponseDTO;
import com.cibertec.bbq.models.Employee;
import com.cibertec.bbq.repositories.CompanyRepository;
import com.cibertec.bbq.repositories.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/** /company/profile (Company\CompanyController del sistema Laravel). Solo administradores de empresa. */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AccessService access;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public ProfileResponseDTO get() {
        access.requireCompanyAdmin();
        return toResponse(access.currentEmployee());
    }

    @Transactional
    public ProfileResponseDTO update(ProfileRequestDTO dto) {
        access.requireCompanyAdmin();
        Employee me = access.currentEmployee();
        Company c = me.getCompany();

        String email = Emails.normalize(dto.getEmail());
        String accountEmail = Emails.normalize(dto.getAccountEmail());
        if (companyRepository.existsByEmailAndIdNot(email, c.getId()))
            throw new BusinessException("email", "Ya existe una empresa con este correo.");
        if (employeeRepository.existsByEmailAndIdNot(accountEmail, me.getId()))
            throw new BusinessException("accountEmail", "Ya existe un usuario con este correo.");
        boolean changePassword = dto.getAccountPassword() != null && !dto.getAccountPassword().isBlank();
        if (changePassword && !Objects.equals(dto.getAccountPassword(), dto.getAccountPasswordConfirmation()))
            throw new BusinessException("accountPasswordConfirmation", "Las contraseñas no coinciden.");

        c.setName(dto.getName().trim());
        c.setEmail(email);
        c.setPhone(dto.getPhone().trim());
        c.setAddress(blankToNull(dto.getAddress()));
        c.setUrl(blankToNull(dto.getUrl()));
        companyRepository.save(c);

        me.setName(dto.getAccountName().trim());
        me.setEmail(accountEmail);
        me.setPhone(blankToNull(dto.getAccountPhone()));
        if (changePassword) me.setPassword(passwordEncoder.encode(dto.getAccountPassword()));
        employeeRepository.save(me);
        return toResponse(me);
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private ProfileResponseDTO toResponse(Employee me) {
        Company c = me.getCompany();
        return new ProfileResponseDTO(c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.getAddress(),
            c.getUrl(), c.getUuid(), me.getId(), me.getName(), me.getEmail(), me.getPhone(), me.getRole().getName());
    }
}
