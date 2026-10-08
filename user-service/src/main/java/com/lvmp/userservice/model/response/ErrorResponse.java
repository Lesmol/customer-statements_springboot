package com.lvmp.userservice.model.response;

import lombok.Builder;

@Builder
public record ErrorResponse(String message, String description) { }
