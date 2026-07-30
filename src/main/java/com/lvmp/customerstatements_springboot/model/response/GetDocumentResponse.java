package com.lvmp.customerstatements_springboot.model.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class GetDocumentResponse {
    private String url;
    private Instant expiresAt;
}
