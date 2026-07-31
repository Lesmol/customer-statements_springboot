package com.lvmp.customerstatements_springboot.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    boolean existsByIdAndUserId(UUID id, UUID userId);
    List<Document> getDocumentsByUserId(UUID userId);
}
