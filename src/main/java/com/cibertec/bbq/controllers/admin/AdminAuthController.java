package com.cibertec.bbq.controllers.admin;

import com.cibertec.bbq.models.DTO.LoginDTO;
import com.cibertec.bbq.models.DTO.LoginResponseDTO;
import com.cibertec.bbq.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginDTO dto) {
        return ResponseEntity.ok(authService.adminLogin(dto));
    }
}
