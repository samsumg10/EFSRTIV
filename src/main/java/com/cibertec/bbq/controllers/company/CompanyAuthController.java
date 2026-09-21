package com.cibertec.bbq.controllers.company;

import com.cibertec.bbq.models.DTO.LoginDTO;
import com.cibertec.bbq.models.DTO.LoginResponseDTO;
import com.cibertec.bbq.models.DTO.MeResponseDTO;
import com.cibertec.bbq.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyAuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginDTO dto) {
        return ResponseEntity.ok(authService.companyLogin(dto));
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponseDTO> me() {
        return ResponseEntity.ok(authService.me());
    }
}
