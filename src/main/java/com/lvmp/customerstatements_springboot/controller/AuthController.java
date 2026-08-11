package com.lvmp.customerstatements_springboot.controller;

import com.lvmp.customerstatements_springboot.model.request.LoginRequest;
import com.lvmp.customerstatements_springboot.model.response.AuthResponse;
import com.lvmp.customerstatements_springboot.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/auth/v1")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/create")
    public ResponseEntity<Void> create(@Valid @RequestBody LoginRequest request) {
        return authService.create(request);
    }
}