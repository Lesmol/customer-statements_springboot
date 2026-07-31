package com.lvmp.customerstatements_springboot.model.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class GetUserDocumentsResponse {
    private UUID documentId;
    private Instant uploadedAt;
}
