package com.lvmp.userservice.service;

import com.lvmp.userservice.config.properties.ApplicationConfigurationProperties;
import com.lvmp.userservice.exception.UserDoesNotExist;
import com.lvmp.userservice.model.response.PageResponse;
import com.lvmp.userservice.model.response.UserResponse;
import com.lvmp.userservice.model.response.UserView;
import com.lvmp.userservice.persistence.entity.Role;
import com.lvmp.userservice.persistence.entity.User;
import com.lvmp.userservice.persistence.repository.RoleRepository;
import com.lvmp.userservice.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ApplicationConfigurationProperties configurationProperties;

    public ResponseEntity<PageResponse<UserResponse>> getUsers(int page, int size) {
        int pageNumber = Math.max(configurationProperties.pagination().minPageNumber(), page);
        int pageSize = Math.clamp(size, configurationProperties.pagination().minPageSize(), configurationProperties.pagination().maxPageSize());
        Pageable pagination = PageRequest.of(pageNumber, pageSize, Sort.by("email"));
        Page<User> users = userRepository.findAll(pagination);
        log.info("Found {} user(s)", users.getNumberOfElements());
        return ResponseEntity.ok().body(PageResponse.response(users.map(this::toUserResponse)));
    }

    public UserView getByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserDoesNotExist("User with email %s does not exist".formatted(email)));
        return toUserView(user);
    }

    public UserView getById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserDoesNotExist("User with id %s does not exist".formatted(id)));
        return toUserView(user);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .username(user.getEmail())
                .build();
    }

    private UserView toUserView(User user) {
        String role = roleRepository.findById(user.getRoleId())
                .map(Role::getName)
                .orElse(null);
        return new UserView(user.getId(), user.getEmail(), role);
    }
}
