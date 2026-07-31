package com.lvmp.customerstatements_springboot.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentRetrievalRepository extends JpaRepository<DocumentRetrieval, UUID> {
}
