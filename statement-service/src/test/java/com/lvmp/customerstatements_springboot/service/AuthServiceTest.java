package com.lvmp.customerstatements_springboot.service;

import com.lvmp.customerstatements_springboot.exception.AuthenticationException;
import com.lvmp.customerstatements_springboot.exception.UserAlreadyExistsException;
import com.lvmp.customerstatements_springboot.model.request.LoginRequest;
import com.lvmp.customerstatements_springboot.model.response.AuthResponse;
import com.lvmp.customerstatements_springboot.persistence.entity.User;
import com.lvmp.customerstatements_springboot.persistence.repository.UserRepository;
import com.lvmp.customerstatements_springboot.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, jwtService, userRepository, passwordEncoder);
    }

    @Test
    void login_returnsToken_whenSuccessful() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername("user1");
        request.setPassword("Password");
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("user1")
                .password("skhbww729ebaueg137g")
                .createdAt(Instant.now())
                .build();

        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user.getId().toString())).thenReturn("signed-token");

        // When
        ResponseEntity<AuthResponse> response = authService.login(request);

        // Then
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertEquals("signed-token", response.getBody().getToken());

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager, times(1)).authenticate(authCaptor.capture());
        assertEquals("user1", authCaptor.getValue().getPrincipal());
        assertEquals("Password", authCaptor.getValue().getCredentials());

        verify(userRepository, times(1)).findByUsername("user1");
        verify(jwtService, times(1)).generateToken(user.getId().toString());
    }

    @Test
    void login_throwsBadCredentialsException_andNeverQueriesUser_whenAuthenticationFails() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername("user1");
        request.setPassword("wrong-password");

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        // When
        // Then
        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        verifyNoInteractions(userRepository, jwtService);
    }

    @Test
    void login_throwsAuthenticationException_whenAuthenticatedUserIsMissingFromDatabase() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername("user1");
        request.setPassword("wrong-password");

        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());

        // When
        // Then
        assertThrows(AuthenticationException.class, () -> authService.login(request));

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void create_returns201_whenSuccessful() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername("user6");
        request.setPassword("hbkdjswkhbw");

        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");

        // When
        ResponseEntity<Void> response = authService.create(request);

        // Then
        assertTrue(response.getStatusCode().is2xxSuccessful());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertEquals("user6", userCaptor.getValue().getUsername());
    }

    @Test
    void create_throwsUserAlreadyExistsException_andNeverSaves_whenUsernameIsTaken() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername("user6");
        request.setPassword("hbkdjswkhbw");

        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .username("user6")
                .password("encoded-password")
                .createdAt(Instant.now())
                .build();

        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.of(existingUser));

        // When
        // Then
        assertThrows(UserAlreadyExistsException.class, () -> authService.create(request));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void create_throwsAuthenticationException_whenSaveFails() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername("user6");
        request.setPassword("hbkdjswkhbw");

        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(any())).thenThrow(new RuntimeException("db is down"));

        // When
        // Then
        assertThrows(AuthenticationException.class, () -> authService.create(request));
    }
}