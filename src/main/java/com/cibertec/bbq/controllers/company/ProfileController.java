package com.cibertec.bbq.controllers.company;

import com.cibertec.bbq.models.DTO.ProfileRequestDTO;
import com.cibertec.bbq.models.DTO.ProfileResponseDTO;
import com.cibertec.bbq.services.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileResponseDTO> get() {
        return ResponseEntity.ok(profileService.get());
    }

    @PutMapping
    public ResponseEntity<ProfileResponseDTO> update(@RequestBody @Valid ProfileRequestDTO dto) {
        return ResponseEntity.ok(profileService.update(dto));
    }
}
