package com.cibertec.bbq.services;

import com.cibertec.bbq.models.DTO.LocationRequestDTO;
import com.cibertec.bbq.models.DTO.LocationResponseDTO;
import com.cibertec.bbq.models.Facility;
import com.cibertec.bbq.models.Location;
import com.cibertec.bbq.repositories.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/** /company/facilities/{id}/locations. */
@Service
@RequiredArgsConstructor
public class LocationService {

    private final AccessService access;
    private final LocationRepository locationRepository;

    @Transactional(readOnly = true)
    public List<LocationResponseDTO> findAll(Long facilityId) {
        Facility f = access.facility(facilityId);
        return locationRepository.findAllByFacilityIdAndDeletedAtIsNullOrderByIdAsc(f.getId())
            .stream().map(LocationService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LocationResponseDTO findById(Long locationId) {
        return toResponse(access.location(locationId));
    }

    @Transactional
    public LocationResponseDTO create(Long facilityId, LocationRequestDTO dto) {
        Facility f = access.facility(facilityId);
        Location l = new Location();
        l.setFacility(f);
        l.setUuid(UUID.randomUUID().toString());
        apply(l, dto);
        return toResponse(locationRepository.save(l));
    }

    @Transactional
    public LocationResponseDTO update(Long facilityId, Long id, LocationRequestDTO dto) {
        Location l = getInFacility(facilityId, id);
        apply(l, dto);
        return toResponse(locationRepository.save(l));
    }

    @Transactional
    public void delete(Long facilityId, Long id) {
        Location l = getInFacility(facilityId, id);
        l.markDeleted();
        locationRepository.save(l);
    }

    private Location getInFacility(Long facilityId, Long id) {
        Location l = access.location(id);
        if (!l.getFacility().getId().equals(facilityId)) {
            throw new NoSuchElementException("Ubicación no encontrada.");
        }
        return l;
    }

    private void apply(Location l, LocationRequestDTO dto) {
        l.setName(dto.getName().trim());
        l.setDescription(dto.getDescription() == null || dto.getDescription().isBlank() ? null : dto.getDescription().trim());
        l.setMaxPerson(dto.getMaxPerson());
        l.setPrice(dto.getPrice());
    }

    public static LocationResponseDTO toResponse(Location l) {
        return new LocationResponseDTO(l.getId(), l.getFacility().getId(), l.getFacility().getName(), l.getName(),
            l.getDescription(), l.getUuid(), l.getMaxPerson(), l.getPrice());
    }
}
