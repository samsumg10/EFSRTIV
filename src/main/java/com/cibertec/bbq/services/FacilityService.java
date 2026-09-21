package com.cibertec.bbq.services;

import com.cibertec.bbq.models.DTO.FacilityRequestDTO;
import com.cibertec.bbq.models.DTO.FacilityResponseDTO;
import com.cibertec.bbq.models.Facility;
import com.cibertec.bbq.repositories.FacilityRepository;
import com.cibertec.bbq.repositories.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** /company/facilities. Empleados (tipo 2): solo lectura de sus instalaciones asignadas. */
@Service
@RequiredArgsConstructor
public class FacilityService {

    private final AccessService access;
    private final FacilityRepository facilityRepository;
    private final LocationRepository locationRepository;

    @Transactional(readOnly = true)
    public List<FacilityResponseDTO> findAll() {
        return access.visibleFacilities().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public FacilityResponseDTO findById(Long id) {
        return toResponse(access.facility(id));
    }

    @Transactional
    public FacilityResponseDTO create(FacilityRequestDTO dto) {
        access.requireCompanyAdmin();
        Facility f = new Facility();
        f.setCompany(access.currentEmployee().getCompany());
        f.setUuid(UUID.randomUUID().toString());
        apply(f, dto);
        return toResponse(facilityRepository.save(f));
    }

    @Transactional
    public FacilityResponseDTO update(Long id, FacilityRequestDTO dto) {
        access.requireCompanyAdmin();
        Facility f = access.facility(id);
        apply(f, dto);
        return toResponse(facilityRepository.save(f));
    }

    @Transactional
    public void delete(Long id) {
        access.requireCompanyAdmin();
        Facility f = access.facility(id);
        f.markDeleted();
        facilityRepository.save(f);
    }

    private void apply(Facility f, FacilityRequestDTO dto) {
        f.setName(dto.getName().trim());
        f.setEmail(dto.getEmail().trim().toLowerCase());
        f.setPhone(dto.getPhone().trim());
        f.setAddress(blankToNull(dto.getAddress()));
        f.setDescription(blankToNull(dto.getDescription()));
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    public FacilityResponseDTO toResponse(Facility f) {
        return new FacilityResponseDTO(f.getId(), f.getName(), f.getEmail(), f.getPhone(), f.getAddress(),
            f.getDescription(), f.getUuid(), locationRepository.countByFacilityIdAndDeletedAtIsNull(f.getId()));
    }
}
