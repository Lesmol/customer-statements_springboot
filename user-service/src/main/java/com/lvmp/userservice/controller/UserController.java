package com.lvmp.userservice.controller;

import com.lvmp.userservice.model.response.PageResponse;
import com.lvmp.userservice.model.response.UserResponse;
import com.lvmp.userservice.security.AdminAccessGuard;
import com.lvmp.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("api/users/v1")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AdminAccessGuard adminAccessGuard;

    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> getUsers(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        adminAccessGuard.requireAdmin(userId);
        return userService.getUsers(page, size);
    }
}
