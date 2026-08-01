package com.lvmp.customerstatements_springboot.service;

import com.lvmp.customerstatements_springboot.model.request.LoginRequest;
import com.lvmp.customerstatements_springboot.model.response.AuthResponse;
import com.lvmp.customerstatements_springboot.persistence.repository.UserRepository;
import com.lvmp.customerstatements_springboot.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public ResponseEntity<AuthResponse> login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + request.getUsername()));

        String token = jwtService.generateToken(user.getId().toString());

        return ResponseEntity.ok().body(AuthResponse.builder()
                .token(token)
                .build());
    }
}