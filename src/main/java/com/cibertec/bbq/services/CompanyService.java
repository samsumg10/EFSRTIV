package com.cibertec.bbq.services;

import com.cibertec.bbq.exceptions.BusinessException;
import com.cibertec.bbq.models.AppConstants;
import com.cibertec.bbq.models.Company;
import com.cibertec.bbq.models.DTO.CompanyRequestDTO;
import com.cibertec.bbq.models.DTO.CompanyResponseDTO;
import com.cibertec.bbq.models.Employee;
import com.cibertec.bbq.models.Role;
import com.cibertec.bbq.repositories.CompanyRepository;
import com.cibertec.bbq.repositories.EmployeeRepository;
import com.cibertec.bbq.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/** Empresas desde el panel admin (Admin\CompanyController del sistema Laravel). */
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<CompanyResponseDTO> findAll() {
        return companyRepository.findAllByDeletedAtIsNullOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CompanyResponseDTO findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    /** RN-12: la empresa nace con su representante (empleado con el primer rol de tipo 1). */
    @Transactional
    public CompanyResponseDTO create(CompanyRequestDTO dto) {
        String email = Emails.normalize(dto.getEmail());
        String repEmail = Emails.normalize(dto.getRepresentativeEmail());
        if (companyRepository.existsByEmail(email))
            throw new BusinessException("email", "Ya existe una empresa con este correo.");
        if (employeeRepository.existsByEmail(repEmail))
            throw new BusinessException("representativeEmail", "Ya existe un usuario con este correo.");
        if (isBlank(dto.getRepresentativePassword()))
            throw new BusinessException("representativePassword", "La contraseña es obligatoria.");
        Role adminRole = roleRepository.findFirstByTypeAndDeletedAtIsNullOrderByIdAsc(AppConstants.ROLE_TYPE_COMPANY_ADMIN)
            .orElseThrow(() -> new BusinessException("Primero crea un rol de tipo Administrador de empresa."));

        Company c = new Company();
        c.setUuid(UUID.randomUUID().toString());
        apply(c, dto, email);
        companyRepository.save(c);

        Employee rep = new Employee();
        rep.setCompany(c);
        rep.setRole(adminRole);
        applyRepresentative(rep, dto, repEmail);
        employeeRepository.save(rep);
        return toResponse(c);
    }

    @Transactional
    public CompanyResponseDTO update(Long id, CompanyRequestDTO dto) {
        Company c = getOrThrow(id);
        String email = Emails.normalize(dto.getEmail());
        String repEmail = Emails.normalize(dto.getRepresentativeEmail());
        if (companyRepository.existsByEmailAndIdNot(email, id))
            throw new BusinessException("email", "Ya existe una empresa con este correo.");

        Employee rep = representative(c).orElse(null);
        if (rep == null) {
            // La empresa se quedó sin representante: se crea uno nuevo
            if (employeeRepository.existsByEmail(repEmail))
                throw new BusinessException("representativeEmail", "Ya existe un usuario con este correo.");
            if (isBlank(dto.getRepresentativePassword()))
                throw new BusinessException("representativePassword", "La contraseña es obligatoria.");
            rep = new Employee();
            rep.setCompany(c);
            rep.setRole(roleRepository.findFirstByTypeAndDeletedAtIsNullOrderByIdAsc(AppConstants.ROLE_TYPE_COMPANY_ADMIN)
                .orElseThrow(() -> new BusinessException("Primero crea un rol de tipo Administrador de empresa.")));
        } else if (employeeRepository.existsByEmailAndIdNot(repEmail, rep.getId())) {
            throw new BusinessException("representativeEmail", "Ya existe un usuario con este correo.");
        }

        apply(c, dto, email);
        companyRepository.save(c);
        applyRepresentative(rep, dto, repEmail);
        employeeRepository.save(rep);
        return toResponse(c);
    }

    /** RN-14: soft delete de la empresa y sus empleados liberando los correos. */
    @Transactional
    public void delete(Long id) {
        Company c = getOrThrow(id);
        c.setEmail(Emails.release(c.getEmail(), c.getId()));
        c.markDeleted();
        companyRepository.save(c);
        for (Employee e : employeeRepository.findAllByCompanyId(id)) {
            if (e.getDeletedAt() == null) {
                e.setEmail(Emails.release(e.getEmail(), e.getId()));
                e.markDeleted();
            }
        }
    }

    private void apply(Company c, CompanyRequestDTO dto, String email) {
        c.setName(dto.getName().trim());
        c.setEmail(email);
        c.setPhone(dto.getPhone().trim());
        c.setAddress(trimToNull(dto.getAddress()));
        c.setUrl(trimToNull(dto.getUrl()));
        c.setStatus(dto.getStatus());
    }

    private void applyRepresentative(Employee rep, CompanyRequestDTO dto, String email) {
        rep.setName(dto.getRepresentativeName().trim());
        rep.setEmail(email);
        rep.setPhone(trimToNull(dto.getRepresentativePhone()));
        if (!isBlank(dto.getRepresentativePassword())) {
            rep.setPassword(passwordEncoder.encode(dto.getRepresentativePassword()));
        }
    }

    private java.util.Optional<Employee> representative(Company c) {
        return employeeRepository.findFirstByCompanyIdAndRole_TypeAndDeletedAtIsNullOrderByIdAsc(
            c.getId(), AppConstants.ROLE_TYPE_COMPANY_ADMIN);
    }

    private Company getOrThrow(Long id) {
        return companyRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NoSuchElementException("Empresa no encontrada."));
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static String trimToNull(String s) { return isBlank(s) ? null : s.trim(); }

    public CompanyResponseDTO toResponse(Company c) {
        Employee rep = representative(c).orElse(null);
        return new CompanyResponseDTO(c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.getAddress(),
            c.getUrl(), c.getStatus(), c.getUuid(), c.getCreatedAt(),
            rep != null ? rep.getId() : null, rep != null ? rep.getName() : null,
            rep != null ? rep.getEmail() : null, rep != null ? rep.getPhone() : null);
    }
}
