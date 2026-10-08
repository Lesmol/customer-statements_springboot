package com.lvmp.userservice.security;

import com.lvmp.userservice.exception.ForbiddenException;
import com.lvmp.userservice.persistence.entity.Role;
import com.lvmp.userservice.persistence.repository.RoleRepository;
import com.lvmp.userservice.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminAccessGuard {
    private static final String ADMIN_ROLE_NAME = "ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public void requireAdmin(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ForbiddenException("Unknown user: " + userId));

        String roleName = roleRepository.findById(user.getRoleId())
                .map(Role::getName)
                .orElseThrow(() -> new ForbiddenException("User " + userId + " has no assigned role"));

        if (!ADMIN_ROLE_NAME.equals(roleName)) {
            throw new ForbiddenException("User " + userId + " does not have the ADMIN role");
        }
    }
}
