package com.lvmp.customerstatements_springboot.service;

import com.lvmp.customerstatements_springboot.config.properties.ApplicationConfigurationProperties;
import com.lvmp.customerstatements_springboot.exception.DocumentNotFoundException;
import com.lvmp.customerstatements_springboot.exception.DocumentSaveException;
import com.lvmp.customerstatements_springboot.exception.UserDoesNotExist;
import com.lvmp.customerstatements_springboot.model.request.UploadStatementRequest;
import com.lvmp.customerstatements_springboot.model.response.GetDocumentResponse;
import com.lvmp.customerstatements_springboot.model.response.GetUserDocumentsResponse;
import com.lvmp.customerstatements_springboot.model.response.PageResponse;
import com.lvmp.customerstatements_springboot.model.response.UploadDocumentResponse;
import com.lvmp.customerstatements_springboot.exception.S3UploadException;
import com.lvmp.customerstatements_springboot.client.UserClient;
import com.lvmp.customerstatements_springboot.model.response.UserView;
import com.lvmp.customerstatements_springboot.persistence.entity.Document;
import com.lvmp.customerstatements_springboot.persistence.repository.DocumentRepository;
import com.lvmp.customerstatements_springboot.persistence.entity.DocumentRetrieval;
import com.lvmp.customerstatements_springboot.persistence.repository.DocumentRetrievalRepository;
import com.lvmp.customerstatements_springboot.utils.PdfHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
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
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatementService {
    private final S3Client s3Client;
    private final UserClient userClient;
    private final S3Presigner s3Presigner;
    private final RedisService redisService;
    private final DocumentRepository documentRepository;
    private final DocumentRetrievalRepository documentRetrievalRepository;
    private final ApplicationConfigurationProperties configurationProperties;

    public ResponseEntity<UploadDocumentResponse> uploadStatement(UploadStatementRequest request) throws NoSuchAlgorithmException, IOException {
        UserView user;
        try {
            user = userClient.getByEmail(request.getUsername());
        } catch (HttpClientErrorException.NotFound e) {
            throw new UserDoesNotExist("User with email %s does not exist".formatted(request.getUsername()));
        }

        UUID userId = user.id();
        String fileHash = PdfHasher.fileHash(request.getFile());
        Optional<Document> document = documentRepository.getDocumentByFileHashAndUserId(fileHash, userId);

        if (document.isPresent()) {
            return ResponseEntity.ok(UploadDocumentResponse.builder()
                    .documentId(document.get().getId().toString())
                    .build());
        }

        UUID documentId = UUID.randomUUID();
        String bucketName = configurationProperties.s3().bucketName();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(documentId.toString())
                .contentType(request.getFile().getContentType())
                .build();

        try (InputStream inputStream = request.getFile().getInputStream()) {
            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(
                            inputStream,
                            request.getFile().getSize()
                    )
            );
            log.info("Uploaded statement ({}) to s3 bucket {}", documentId, bucketName);

            documentRepository.save(Document.builder()
                    .id(documentId)
                    .userId(userId)
                    .fileName(request.getFile().getOriginalFilename())
                    .fileHash(fileHash)
                    .build());
            log.info("Successfully uploaded statement ({}) for user {}", documentId, userId);

            return ResponseEntity.status(HttpStatus.CREATED).body(UploadDocumentResponse.builder()
                    .documentId(documentId.toString())
                    .build());
        } catch (SdkClientException | S3Exception | IOException e) {
            log.error("Failed to upload statement ({}) to s3", documentId);
            throw new S3UploadException("We couldn't upload your statement right now. Please try again later.", e);
        } catch (DataAccessException | IllegalArgumentException e) {
            log.error("Failed to save {} to database", documentId);
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(documentId.toString())
                    .build());
            throw new DocumentSaveException("We couldn't save your statement right now. Please try again later.", e);
        }
    }

    public ResponseEntity<GetDocumentResponse> getStatement(UUID userId, UUID documentId) {
        if (!documentRepository.existsByIdAndUserId(documentId, userId)) {
            log.warn("No document found with ID: {} for user {}", documentId, userId);
            throw new DocumentNotFoundException("No document found with ID: " + documentId);
        }

        GetDocumentResponse cachedResponse = redisService.getPreSignedUrl(documentId);

        if (cachedResponse != null) {
            log.info("Returning cached pre-signed URL for statement ({})", documentId);
            return ResponseEntity.ok().body(cachedResponse);
        }

        Duration expiresAt = Duration.ofSeconds(configurationProperties.s3().presignUrlExpirationSeconds());

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(configurationProperties.s3().bucketName())
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

        GetDocumentResponse response = GetDocumentResponse.builder()
                .url(presignedGetObjectRequest.url().toString())
                .expiresAt(presignedGetObjectRequest.expiration())
                .build();

        redisService.putPreSignedUrl(documentId, response);
        log.info("Generated and cached pre-signed URL for statement ({}), expiring at {}", documentId, response.getExpiresAt());

        return ResponseEntity.ok().body(response);
    }

    public ResponseEntity<PageResponse<GetUserDocumentsResponse>> getStatements(UUID userId, int page, int size) {
        int pageNumber = Math.max(configurationProperties.pagination().minPageNumber(), page);
        int pageSize = Math.clamp(size, configurationProperties.pagination().minPageSize(), configurationProperties.pagination().maxPageSize());
        Pageable pagination = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "uploadedAt"));
        Page<Document> documents = documentRepository.getDocumentsByUserId(userId, pagination);
        log.info("Found {} statement(s) for user {}", documents.getNumberOfElements(), userId);
        return ResponseEntity.ok().body(PageResponse.response(documents.map(this::toDocumentResponse)));
    }

    private GetUserDocumentsResponse toDocumentResponse(Document document) {
        return GetUserDocumentsResponse.builder()
                .documentId(document.getId())
                .filename(document.getFileName())
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}
