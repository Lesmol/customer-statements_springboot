package com.lvmp.customerstatements_springboot.config;

import com.lvmp.customerstatements_springboot.persistence.entity.User;
import com.lvmp.customerstatements_springboot.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Profile("local")
public class UserSeeder implements CommandLineRunner {
    private static final String DEFAULT_PASSWORD = "Test@123";
    private static final List<String> USERNAMES = List.of("user1", "user2", "user3");
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String @NonNull ... args) {
        USERNAMES.stream()
                .filter(username -> !userRepository.existsByUsername(username))
                .forEach(username -> {
                    userRepository.save(User.builder()
                            .username(username)
                            .password(passwordEncoder
                                    .encode(DEFAULT_PASSWORD))
                            .createdAt(Instant.now())
                            .build());
                });
    }
}