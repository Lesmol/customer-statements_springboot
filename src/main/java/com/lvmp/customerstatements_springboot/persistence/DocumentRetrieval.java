package com.lvmp.customerstatements_springboot.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRetrieval {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "document_id", nullable = false, updatable = false)
    private UUID documentId;

    @Column(name = "retrieved_at", nullable = false, updatable = false)
    private Instant retrievedAt;

    @Column(name = "expired_at", nullable = false, updatable = false)
    private Instant expiredAt;
}