package com.lvmp.customerstatements_springboot.persistence.repository;

import com.lvmp.customerstatements_springboot.persistence.entity.DocumentRetrieval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentRetrievalRepository extends JpaRepository<DocumentRetrieval, UUID> {
}
