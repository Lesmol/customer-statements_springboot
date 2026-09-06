package com.lvmp.loginservice.service;

import com.lvmp.loginservice.config.properties.DexProperties;
import com.lvmp.loginservice.model.request.LoginRequest;
import com.lvmp.loginservice.model.response.DexTokenResponse;
import com.lvmp.loginservice.model.response.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class DexAuthenticationService implements AuthenticationService {
    private final DexProperties dexProperties;
    private final RestClient dexRestClient;

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("username", request.getUsername());
        form.add("password", request.getPassword());
        form.add("scope", "openid profile email offline_access");
        form.add("client_id", dexProperties.clientId());
        form.add("client_secret", dexProperties.clientSecret());

        DexTokenResponse token = dexRestClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(DexTokenResponse.class);

        return ResponseEntity.ok(LoginResponse.from(token));
    }
}