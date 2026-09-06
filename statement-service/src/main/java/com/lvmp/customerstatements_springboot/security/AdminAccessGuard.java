package com.lvmp.customerstatements_springboot.security;

import com.lvmp.customerstatements_springboot.client.UserClient;
import com.lvmp.customerstatements_springboot.exception.ForbiddenException;
import com.lvmp.customerstatements_springboot.model.response.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminAccessGuard {
    private static final String ADMIN_ROLE_NAME = "ADMIN";

    private final UserClient userClient;

    public void requireAdmin(UUID userId) {
        UserView user;
        try {
            user = userClient.getById(userId);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ForbiddenException("Unknown user: " + userId);
        }

        if (!ADMIN_ROLE_NAME.equals(user.role())) {
            throw new ForbiddenException("User " + userId + " does not have the ADMIN role");
        }
    }
}
