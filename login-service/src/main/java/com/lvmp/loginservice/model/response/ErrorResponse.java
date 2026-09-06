package com.lvmp.loginservice.model.response;

import lombok.Builder;

@Builder
public record ErrorResponse(String message, String description) { }