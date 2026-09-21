package com.cibertec.bbq.services;

import com.cibertec.bbq.exceptions.BusinessException;
import com.cibertec.bbq.models.AppConstants;
import com.cibertec.bbq.models.DTO.RoleRequestDTO;
import com.cibertec.bbq.models.DTO.RoleResponseDTO;
import com.cibertec.bbq.models.Role;
import com.cibertec.bbq.repositories.EmployeeRepository;
import com.cibertec.bbq.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public List<RoleResponseDTO> findAll() {
        return roleRepository.findAllByDeletedAtIsNullOrderByIdAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RoleResponseDTO findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public RoleResponseDTO create(RoleRequestDTO dto) {
        Role r = new Role();
        r.setName(dto.getName().trim());
        r.setType(dto.getType());
        return toResponse(roleRepository.save(r));
    }

    @Transactional
    public RoleResponseDTO update(Long id, RoleRequestDTO dto) {
        Role r = getOrThrow(id);
        if (!r.getType().equals(dto.getType())) {
            if (employeeRepository.countByRoleIdAndDeletedAtIsNull(id) > 0) {
                throw new BusinessException("type", "No se puede cambiar el tipo de un rol asignado a empleados.");
            }
            ensureNotLastCompanyAdminRole(r);
        }
        r.setName(dto.getName().trim());
        r.setType(dto.getType());
        return toResponse(roleRepository.save(r));
    }

    /** RN-13: no se borra un rol en uso ni el último de tipo 1 (se necesita para crear empresas). */
    @Transactional
    public void delete(Long id) {
        Role r = getOrThrow(id);
        long inUse = employeeRepository.countByRoleIdAndDeletedAtIsNull(id);
        if (inUse > 0) {
            throw new BusinessException("No se puede eliminar: " + inUse + " empleado(s) tienen este rol.");
        }
        ensureNotLastCompanyAdminRole(r);
        r.markDeleted();
        roleRepository.save(r);
    }

    private void ensureNotLastCompanyAdminRole(Role r) {
        if (r.getType() == AppConstants.ROLE_TYPE_COMPANY_ADMIN) {
            long admins = roleRepository.findAllByDeletedAtIsNullOrderByIdAsc().stream()
                .filter(x -> x.getType() == AppConstants.ROLE_TYPE_COMPANY_ADMIN).count();
            if (admins <= 1) {
                throw new BusinessException("Debe existir al menos un rol de tipo Administrador de empresa.");
            }
        }
    }

    private Role getOrThrow(Long id) {
        return roleRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NoSuchElementException("Rol no encontrado."));
    }

    public RoleResponseDTO toResponse(Role r) {
        return new RoleResponseDTO(r.getId(), r.getName(), r.getType(),
            employeeRepository.countByRoleIdAndDeletedAtIsNull(r.getId()));
    }
}
