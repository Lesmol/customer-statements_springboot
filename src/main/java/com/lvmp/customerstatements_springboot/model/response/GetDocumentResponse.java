package com.lvmp.customerstatements_springboot.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetDocumentResponse {
    private String url;
    private Instant expiresAt;
}
