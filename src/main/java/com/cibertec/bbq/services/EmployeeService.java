package com.cibertec.bbq.services;

import com.cibertec.bbq.exceptions.BusinessException;
import com.cibertec.bbq.models.*;
import com.cibertec.bbq.models.DTO.EmployeeRequestDTO;
import com.cibertec.bbq.models.DTO.EmployeeResponseDTO;
import com.cibertec.bbq.models.DTO.RoleResponseDTO;
import com.cibertec.bbq.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/** /company/employees (Company\EmployeeController del sistema Laravel). Solo administradores de empresa. */
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final AccessService access;
    private final EmployeeRepository employeeRepository;
    private final EmployeeFacilityRepository employeeFacilityRepository;
    private final FacilityRepository facilityRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<EmployeeResponseDTO> findAll() {
        access.requireCompanyAdmin();
        Long meId = access.currentEmployee().getId();
        return employeeRepository.findAllByCompanyIdAndDeletedAtIsNullOrderByIdDesc(access.companyId())
            .stream().map(e -> toResponse(e, meId)).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponseDTO findById(Long id) {
        access.requireCompanyAdmin();
        return toResponse(getOrThrow(id), access.currentEmployee().getId());
    }

    @Transactional(readOnly = true)
    public List<RoleResponseDTO> roles() {
        access.requireCompanyAdmin();
        return roleRepository.findAllByDeletedAtIsNullOrderByIdAsc().stream()
            .map(r -> new RoleResponseDTO(r.getId(), r.getName(), r.getType(), 0)).toList();
    }

    @Transactional
    public EmployeeResponseDTO create(EmployeeRequestDTO dto) {
        access.requireCompanyAdmin();
        Employee me = access.currentEmployee();
        String email = Emails.normalize(dto.getEmail());
        if (employeeRepository.existsByEmail(email))
            throw new BusinessException("email", "Ya existe un usuario con este correo.");
        if (dto.getPassword() == null || dto.getPassword().isBlank())
            throw new BusinessException("password", "La contraseña es obligatoria.");
        checkConfirmation(dto);

        Employee e = new Employee();
        e.setCompany(me.getCompany());
        e.setRole(role(dto.getRoleId()));
        apply(e, dto, email);
        employeeRepository.save(e);
        syncFacilities(e, dto.getFacilityIds());
        return toResponse(e, me.getId());
    }

    @Transactional
    public EmployeeResponseDTO update(Long id, EmployeeRequestDTO dto) {
        access.requireCompanyAdmin();
        Employee me = access.currentEmployee();
        Employee e = getOrThrow(id);
        String email = Emails.normalize(dto.getEmail());
        if (employeeRepository.existsByEmailAndIdNot(email, id))
            throw new BusinessException("email", "Ya existe un usuario con este correo.");
        checkConfirmation(dto);

        Role newRole = role(dto.getRoleId());
        boolean losesAdmin = e.getRole().getType() == AppConstants.ROLE_TYPE_COMPANY_ADMIN
            && newRole.getType() != AppConstants.ROLE_TYPE_COMPANY_ADMIN;
        if (losesAdmin && e.getId().equals(me.getId()))
            throw new BusinessException("roleId", "No puedes quitarte a ti mismo el rol de administrador.");
        if (losesAdmin && countAdmins(e) <= 1)
            throw new BusinessException("roleId", "La empresa debe tener al menos un administrador.");

        e.setRole(newRole);
        apply(e, dto, email);
        employeeRepository.save(e);
        syncFacilities(e, dto.getFacilityIds());
        return toResponse(e, me.getId());
    }

    /** RN-13 / RN-14. */
    @Transactional
    public void delete(Long id) {
        access.requireCompanyAdmin();
        Employee me = access.currentEmployee();
        Employee e = getOrThrow(id);
        if (e.getId().equals(me.getId()))
            throw new BusinessException("No puedes eliminar tu propio usuario.");
        if (e.getRole().getType() == AppConstants.ROLE_TYPE_COMPANY_ADMIN && countAdmins(e) <= 1)
            throw new BusinessException("La empresa debe tener al menos un administrador.");
        e.setEmail(Emails.release(e.getEmail(), e.getId()));
        e.markDeleted();
        employeeRepository.save(e);
        syncFacilities(e, List.of());
    }

    private void apply(Employee e, EmployeeRequestDTO dto, String email) {
        e.setName(dto.getName().trim());
        e.setEmail(email);
        e.setPhone(dto.getPhone() == null || dto.getPhone().isBlank() ? null : dto.getPhone().trim());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            e.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
    }

    private void checkConfirmation(EmployeeRequestDTO dto) {
        if (dto.getPassword() != null && !dto.getPassword().isBlank()
                && !Objects.equals(dto.getPassword(), dto.getPasswordConfirmation()))
            throw new BusinessException("passwordConfirmation", "Las contraseñas no coinciden.");
    }

    private long countAdmins(Employee e) {
        return employeeRepository.countByCompanyIdAndRole_TypeAndDeletedAtIsNull(
            e.getCompany().getId(), AppConstants.ROLE_TYPE_COMPANY_ADMIN);
    }

    private Role role(Long roleId) {
        return roleRepository.findByIdAndDeletedAtIsNull(roleId)
            .orElseThrow(() -> new BusinessException("roleId", "Rol no válido."));
    }

    /**
     * Sincroniza employee_facility: marca como eliminadas las quitadas y agrega las nuevas.
     * Un administrador (tipo 1) ve todas, así que no guarda asignaciones.
     */
    private void syncFacilities(Employee e, List<Long> requested) {
        Set<Long> wanted = new LinkedHashSet<>();
        if (e.getDeletedAt() == null && e.getRole().getType() == AppConstants.ROLE_TYPE_EMPLOYEE && requested != null) {
            wanted.addAll(requested);
        }
        Long companyId = e.getCompany().getId();
        List<EmployeeFacility> current = employeeFacilityRepository.findAllByEmployeeIdAndDeletedAtIsNull(e.getId());
        Set<Long> currentIds = new HashSet<>();
        for (EmployeeFacility ef : current) {
            if (wanted.contains(ef.getFacility().getId())) {
                currentIds.add(ef.getFacility().getId());
            } else {
                ef.setDeletedAt(LocalDateTime.now());
            }
        }
        for (Long facilityId : wanted) {
            if (currentIds.contains(facilityId)) continue;
            Facility f = facilityRepository.findActiveById(facilityId)
                .filter(x -> x.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new BusinessException("facilityIds", "Instalación no válida."));
            employeeFacilityRepository.save(new EmployeeFacility(e, f));
        }
    }

    private Employee getOrThrow(Long id) {
        return employeeRepository.findByIdAndCompanyIdAndDeletedAtIsNull(id, access.companyId())
            .orElseThrow(() -> new NoSuchElementException("Empleado no encontrado."));
    }

    private EmployeeResponseDTO toResponse(Employee e, Long meId) {
        List<Facility> facilities = employeeFacilityRepository.findAllByEmployeeIdAndDeletedAtIsNull(e.getId())
            .stream().map(EmployeeFacility::getFacility).filter(f -> f.getDeletedAt() == null).toList();
        return new EmployeeResponseDTO(e.getId(), e.getName(), e.getEmail(), e.getPhone(),
            e.getRole().getId(), e.getRole().getName(), e.getRole().getType(),
            facilities.stream().map(Facility::getId).toList(),
            facilities.stream().map(Facility::getName).toList(),
            e.getCreatedAt(), e.getId().equals(meId));
    }
}
