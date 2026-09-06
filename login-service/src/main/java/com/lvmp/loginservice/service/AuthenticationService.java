package com.lvmp.loginservice.service;

import com.lvmp.loginservice.model.request.LoginRequest;
import com.lvmp.loginservice.model.response.LoginResponse;
import org.springframework.http.ResponseEntity;

public interface AuthenticationService {
    ResponseEntity<LoginResponse> login(LoginRequest request);
}