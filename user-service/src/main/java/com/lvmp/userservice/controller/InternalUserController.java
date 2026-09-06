package com.lvmp.userservice.controller;

import com.lvmp.userservice.model.response.UserView;
import com.lvmp.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("internal/v1/users")
@RequiredArgsConstructor
public class InternalUserController {
    private final UserService userService;

    @GetMapping("/by-email/{email}")
    public ResponseEntity<UserView> getByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getByEmail(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserView> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }
}
