package com.lvmp.customerstatements_springboot.service;

import com.lvmp.customerstatements_springboot.exception.DocumentNotFoundException;
import com.lvmp.customerstatements_springboot.exception.DocumentSaveException;
import com.lvmp.customerstatements_springboot.model.request.UploadStatementRequest;
import com.lvmp.customerstatements_springboot.model.response.GetDocumentResponse;
import com.lvmp.customerstatements_springboot.model.response.GetUserDocumentsResponse;
import com.lvmp.customerstatements_springboot.model.response.UploadDocumentResponse;
import com.lvmp.customerstatements_springboot.exception.S3UploadException;
import com.lvmp.customerstatements_springboot.persistence.entity.Document;
import com.lvmp.customerstatements_springboot.persistence.repository.DocumentRepository;
import com.lvmp.customerstatements_springboot.persistence.entity.DocumentRetrieval;
import com.lvmp.customerstatements_springboot.persistence.repository.DocumentRetrievalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatementService {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final DocumentRepository documentRepository;
    private final DocumentRetrievalRepository documentRetrievalRepository;
    @Value("${app.s3.bucket-name}")
    private String bucketName;

    public ResponseEntity<UploadDocumentResponse> uploadStatement(UUID userId, UploadStatementRequest request) throws IOException {
        UUID documentId = UUID.randomUUID();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(documentId.toString())
                .contentType(request.getFile().getContentType())
                .build();

        try {
            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(
                            request.getFile().getInputStream(),
                            request.getFile().getSize()
                    )
            );

            documentRepository.save(Document.builder()
                    .id(documentId)
                    .userId(userId)
                    .build());

            return ResponseEntity.ok().body(UploadDocumentResponse.builder()
                    .documentId(documentId.toString())
                    .build());
        } catch (SdkClientException | S3Exception e) {
            log.error("Failed to upload statement ({}) to s3", documentId, e);
            throw new S3UploadException("We couldn't upload your statement right now. Please try again later.");
        } catch (DataAccessException | IllegalArgumentException e) {
            log.error("Failed to save {} to database", documentId, e);
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(documentId.toString())
                    .build());
            throw new DocumentSaveException("We couldn't save your statement right now. Please try again later.");
        }
    }

    public ResponseEntity<GetDocumentResponse> getStatement(UUID userID, UUID documentId) {
        if (!documentRepository.existsByIdAndUserId(documentId, userID)) {
            throw new DocumentNotFoundException("No document found with ID: " + documentId);
        }

        Duration expiresAt = Duration.ofMinutes(5);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(documentId.toString())
                .responseContentDisposition("attachment; filename=statement-%s.pdf".formatted(documentId.toString().substring(0, 8)))
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiresAt)
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(presignRequest);

        documentRetrievalRepository.save(DocumentRetrieval.builder()
                .documentId(documentId)
                .expiredAt(Instant.now()
                        .plus(expiresAt))
                .build());

        return ResponseEntity.ok().body(GetDocumentResponse.builder()
                .url(presignedGetObjectRequest.url().toString())
                .expiresAt(presignedGetObjectRequest.expiration())
                .build());
    }

    public ResponseEntity<List<GetUserDocumentsResponse>> getStatements(UUID userID) {
        List<Document> documents = documentRepository.getDocumentsByUserId(userID);
        return ResponseEntity.ok().body(toDocumentResponse(documents));
    }

    private List<GetUserDocumentsResponse> toDocumentResponse(List<Document> documents) {
        return documents.stream()
                .map(doc -> GetUserDocumentsResponse.builder()
                        .documentId(doc.getId())
                        .uploadedAt(doc.getUploadedAt())
                        .build())
                .toList();
    }
}
