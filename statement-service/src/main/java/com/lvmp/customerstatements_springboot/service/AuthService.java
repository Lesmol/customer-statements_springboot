package com.lvmp.customerstatements_springboot.service;

import com.lvmp.customerstatements_springboot.exception.AuthenticationException;
import com.lvmp.customerstatements_springboot.exception.UserAlreadyExistsException;
import com.lvmp.customerstatements_springboot.model.request.LoginRequest;
import com.lvmp.customerstatements_springboot.model.response.AuthResponse;
import com.lvmp.customerstatements_springboot.persistence.entity.User;
import com.lvmp.customerstatements_springboot.persistence.repository.UserRepository;
import com.lvmp.customerstatements_springboot.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public ResponseEntity<AuthResponse> login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found: {}", request.getUsername());
                    return new AuthenticationException("We couldn't sign you in right now. Please try again later.");
                });

        String token = jwtService.generateToken(user.getId().toString());

        return ResponseEntity.ok().body(AuthResponse.builder()
                .token(token)
                .expiresIn(Duration.ofMillis(expirationMs).getSeconds())
                .build());
    }

    public ResponseEntity<Void> create(LoginRequest request) {
        Optional<User> user = userRepository.findByUsername(request.getUsername());

        if (user.isPresent()) {
            throw new UserAlreadyExistsException("%s already exists".formatted(request.getUsername()));
        }

        try {
            userRepository.save(User.builder()
                    .username(request.getUsername())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .build());
            log.info("Successfully created {}", request.getUsername());

            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            throw new AuthenticationException("We couldn't create your user right now. Please try again later.", e);
        }
    }
}