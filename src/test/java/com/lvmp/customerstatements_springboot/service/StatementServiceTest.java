package com.lvmp.customerstatements_springboot.service;

import com.lvmp.customerstatements_springboot.exception.DocumentNotFoundException;
import com.lvmp.customerstatements_springboot.exception.DocumentSaveException;
import com.lvmp.customerstatements_springboot.exception.S3UploadException;
import com.lvmp.customerstatements_springboot.model.request.UploadStatementRequest;
import com.lvmp.customerstatements_springboot.model.response.GetDocumentResponse;
import com.lvmp.customerstatements_springboot.model.response.GetUserDocumentsResponse;
import com.lvmp.customerstatements_springboot.model.response.PageResponse;
import com.lvmp.customerstatements_springboot.model.response.UploadDocumentResponse;
import com.lvmp.customerstatements_springboot.persistence.entity.Document;
import com.lvmp.customerstatements_springboot.persistence.repository.DocumentRepository;
import com.lvmp.customerstatements_springboot.persistence.repository.DocumentRetrievalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatementServiceTest {
    @Mock
    private S3Client s3Client;
    @Mock
    private S3Presigner s3Presigner;
    @Mock
    private RedisService redisService;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentRetrievalRepository documentRetrievalRepository;

    private StatementService statementService;

    @BeforeEach
    void setUp() {
        statementService = new StatementService(
                s3Client, s3Presigner, redisService, documentRepository, documentRetrievalRepository);
        ReflectionTestUtils.setField(statementService, "bucketName", "test-bucket");
    }

    @Test
    void uploadStatement_savesToS3AndDatabase_whenBothSucceed() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UploadStatementRequest request = new UploadStatementRequest();
        request.setFile(new MockMultipartFile("file", "statement.pdf", "application/pdf", "content".getBytes()));

        // When
        ResponseEntity<UploadDocumentResponse> response = statementService.uploadStatement(userId, request);

        // Then
        assertTrue(response.getStatusCode().is2xxSuccessful());

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(documentRepository, times(1)).save(documentCaptor.capture());
        assertEquals(userId, documentCaptor.getValue().getUserId());
        assertNotNull(response.getBody());
        assertEquals(documentCaptor.getValue().getId().toString(), response.getBody().getDocumentId());

    }

    @Test
    void uploadStatement_throwsS3UploadException_andNeverTouchesTheDatabase_whenS3Fails() {
        // Given
        UUID userId = UUID.randomUUID();
        UploadStatementRequest request = new UploadStatementRequest();
        request.setFile(new MockMultipartFile("file", "statement.pdf", "application/pdf", "content".getBytes()));

        when(s3Client.putObject(any(PutObjectRequest.class), (RequestBody) any()))
                .thenThrow(S3Exception.builder().message("s3 is down").build());

        // When
        // Then
        assertThrows(S3UploadException.class, () -> statementService.uploadStatement(userId, request));

        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadStatement_throwsDocumentSaveException_andRollsBackTheS3Object_whenDatabaseSaveFails() {
        // Given
        UUID userId = UUID.randomUUID();
        UploadStatementRequest request = new UploadStatementRequest();
        request.setFile(new MockMultipartFile("file", "statement.pdf", "application/pdf", "content".getBytes()));

        when(documentRepository.save(any(Document.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        // When
        // Then
        assertThrows(DocumentSaveException.class, () -> statementService.uploadStatement(userId, request));

        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void getStatement_throwsDocumentNotFoundException_whenDocumentDoesNotBelongToUser() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        when(documentRepository.existsByIdAndUserId(documentId, userId)).thenReturn(false);

        // When
        // Then
        assertThrows(DocumentNotFoundException.class, () -> statementService.getStatement(userId, documentId));

        verifyNoInteractions(redisService, s3Presigner, documentRetrievalRepository);
    }

    @Test
    void getStatement_returnsCachedResponse_withoutCallingS3_whenAlreadyInRedis() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        GetDocumentResponse cached = GetDocumentResponse.builder()
                .url("https://example-bucket.s3.amazonaws.com/" + documentId)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(4)).plusSeconds(30))
                .build();

        when(documentRepository.existsByIdAndUserId(documentId, userId)).thenReturn(true);
        when(redisService.getPreSignedUrl(documentId)).thenReturn(cached);

        // When
        ResponseEntity<GetDocumentResponse> response = statementService.getStatement(userId, documentId);

        // Then
        InOrder calls = inOrder(documentRepository, redisService);
        calls.verify(documentRepository).existsByIdAndUserId(documentId, userId);
        calls.verify(redisService).getPreSignedUrl(documentId);
        assertEquals(cached, response.getBody());
        verifyNoInteractions(s3Presigner, documentRetrievalRepository);
    }

    @Test
    void getStatement_generatesAndCachesAPresignedUrl_whenNotAlreadyInRedis() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        when(documentRepository.existsByIdAndUserId(documentId, userId)).thenReturn(true);
        when(redisService.getPreSignedUrl(documentId)).thenReturn(null);

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));
        when(presignedRequest.url()).thenReturn(uncheckedUrl(
                "https://example-bucket.s3.amazonaws.com/" + documentId));
        when(presignedRequest.expiration()).thenReturn(expiresAt);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        // When
        ResponseEntity<GetDocumentResponse> response = statementService.getStatement(userId, documentId);

        // Then
        assertNotNull(response.getBody());
        assertEquals(presignedRequest.url().toString(), response.getBody().getUrl());
        assertEquals(expiresAt, response.getBody().getExpiresAt());

        verify(documentRetrievalRepository, times(1)).save(any());
        verify(redisService, times(1)).putPreSignedUrl(any(), any());
    }

    private static URL uncheckedUrl(String url) {
        try {
            return URI.create(url).toURL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getStatements_returnsDocumentsMappedForTheGivenUser() {
        // Given
        UUID userId = UUID.randomUUID();
        int page = 0;
        int size = 10;
        Document document = Document.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .uploadedAt(Instant.now())
                .build();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadedAt"));
        Page<Document> documentPage = new PageImpl<>(List.of(document), pageable, 1);

        when(documentRepository.getDocumentsByUserId(userId, pageable)).thenReturn(documentPage);

        // When
        ResponseEntity<PageResponse<GetUserDocumentsResponse>> response = statementService.getStatements(userId, page, size);

        // Then
        assertNotNull(response.getBody());
        PageResponse<GetUserDocumentsResponse> body = response.getBody();
        assertEquals(1, body.getContent().size());
        assertEquals(document.getId(), body.getContent().getFirst().getDocumentId());
        assertEquals(document.getUploadedAt(), body.getContent().getFirst().getUploadedAt());
        assertEquals(page, body.getPageNumber());
        assertEquals(size, body.getPageSize());
        assertEquals(1, body.getTotalElements());
        assertEquals(1, body.getTotalPages());
        assertTrue(body.isLast());
    }

    @Test
    void getStatements_returnsEmptyPage_whenUserHasNoDocuments() {
        // Given
        UUID userId = UUID.randomUUID();
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadedAt"));
        Page<Document> documentPage = new PageImpl<>(List.of(), pageable, 0);

        when(documentRepository.getDocumentsByUserId(userId, pageable)).thenReturn(documentPage);

        // When
        ResponseEntity<PageResponse<GetUserDocumentsResponse>> response = statementService.getStatements(userId, page, size);

        // Then
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getContent().isEmpty());
        assertEquals(0, response.getBody().getTotalElements());
    }
}
