package com.lvmp.userservice.model.response;

import java.util.UUID;

public record UserView(UUID id, String email, String role) { }
