package com.cibertec.bbq.services;

import com.cibertec.bbq.exceptions.ForbiddenException;
import com.cibertec.bbq.exceptions.UnauthorizedException;
import com.cibertec.bbq.models.*;
import com.cibertec.bbq.repositories.*;
import com.cibertec.bbq.security.AuthUser;
import com.cibertec.bbq.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Aislamiento por empresa en un solo lugar (el original lo repetía en cada controlador):
 * carga el recurso, verifica que sea de la empresa del token y, si el rol es tipo 2,
 * que la instalación esté asignada al empleado (employee_facility).
 * Los métodos deben llamarse dentro de una transacción del service que los usa.
 */
@Service
@RequiredArgsConstructor
public class AccessService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeFacilityRepository employeeFacilityRepository;
    private final FacilityRepository facilityRepository;
    private final LocationRepository locationRepository;
    private final SaleRepository saleRepository;

    public AuthUser user() {
        return CurrentUser.get();
    }

    /** Equivale al middleware CheckCompanyExists: el empleado y su empresa deben seguir activos. */
    public Employee currentEmployee() {
        AuthUser user = user();
        Employee e = employeeRepository.findByIdAndCompanyIdAndDeletedAtIsNull(user.id(), user.companyId())
            .orElseThrow(() -> new UnauthorizedException("Tu usuario ya no existe. Vuelve a iniciar sesión."));
        Company c = e.getCompany();
        if (c.getDeletedAt() != null || !Objects.equals(c.getStatus(), AppConstants.COMPANY_ACTIVE)) {
            throw new UnauthorizedException("La empresa está inactiva.");
        }
        return e;
    }

    public Long companyId() {
        return currentEmployee().getCompany().getId();
    }

    public boolean isCompanyAdmin() {
        return currentEmployee().getRole().getType() == AppConstants.ROLE_TYPE_COMPANY_ADMIN;
    }

    public void requireCompanyAdmin() {
        if (!isCompanyAdmin()) throw new ForbiddenException("Solo un administrador de la empresa puede hacer esto.");
    }

    /** Instalaciones visibles: todas las de la empresa (tipo 1) o las asignadas (tipo 2). */
    public List<Facility> visibleFacilities() {
        Employee e = currentEmployee();
        if (e.getRole().getType() == AppConstants.ROLE_TYPE_COMPANY_ADMIN) {
            return facilityRepository.findAllByCompanyIdAndDeletedAtIsNullOrderByIdDesc(e.getCompany().getId());
        }
        List<Long> ids = employeeFacilityRepository.findFacilityIds(e.getId());
        return ids.isEmpty() ? List.of() : facilityRepository.findAllByIdInAndDeletedAtIsNullOrderByIdDesc(ids);
    }

    public Facility facility(Long facilityId) {
        Employee e = currentEmployee();
        Facility f = facilityRepository.findActiveById(facilityId)
            .orElseThrow(() -> new NoSuchElementException("Instalación no encontrada."));
        if (!f.getCompany().getId().equals(e.getCompany().getId())) {
            throw new ForbiddenException();
        }
        if (e.getRole().getType() != AppConstants.ROLE_TYPE_COMPANY_ADMIN
                && !employeeFacilityRepository.existsByEmployeeIdAndFacilityIdAndDeletedAtIsNull(e.getId(), f.getId())) {
            throw new ForbiddenException("No tienes acceso a esta instalación.");
        }
        return f;
    }

    public Location location(Long locationId) {
        Location l = locationRepository.findActiveById(locationId)
            .orElseThrow(() -> new NoSuchElementException("Ubicación no encontrada."));
        facility(l.getFacility().getId());
        return l;
    }

    /** Reserva (vía su venta 1:1) de una instalación accesible. */
    public Sale booking(Long bookingId) {
        Sale s = saleRepository.findByBookingId(bookingId)
            .orElseThrow(() -> new NoSuchElementException("Reserva no encontrada."));
        facility(s.getBooking().getLocation().getFacility().getId());
        return s;
    }
}
