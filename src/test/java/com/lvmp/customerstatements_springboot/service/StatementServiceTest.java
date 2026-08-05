package com.lvmp.customerstatements_springboot.service;

import com.lvmp.customerstatements_springboot.exception.DocumentNotFoundException;
import com.lvmp.customerstatements_springboot.exception.DocumentSaveException;
import com.lvmp.customerstatements_springboot.exception.S3UploadException;
import com.lvmp.customerstatements_springboot.model.request.UploadStatementRequest;
import com.lvmp.customerstatements_springboot.model.response.GetDocumentResponse;
import com.lvmp.customerstatements_springboot.model.response.GetUserDocumentsResponse;
import com.lvmp.customerstatements_springboot.model.response.UploadDocumentResponse;
import com.lvmp.customerstatements_springboot.persistence.entity.Document;
import com.lvmp.customerstatements_springboot.persistence.repository.DocumentRepository;
import com.lvmp.customerstatements_springboot.persistence.repository.DocumentRetrievalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
        UUID userId = UUID.randomUUID();
        UploadStatementRequest request = new UploadStatementRequest();
        request.setFile(new MockMultipartFile("file", "statement.pdf", "application/pdf", "content".getBytes()));

        ResponseEntity<UploadDocumentResponse> response = statementService.uploadStatement(userId, request);

        assertTrue(response.getStatusCode().is2xxSuccessful());

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(documentCaptor.capture());
        assertEquals(userId, documentCaptor.getValue().getUserId());
        assertNotNull(response.getBody());
        assertEquals(documentCaptor.getValue().getId().toString(), response.getBody().getDocumentId());

    }

    @Test
    void uploadStatement_throwsS3UploadException_andNeverTouchesTheDatabase_whenS3Fails() {
        UUID userId = UUID.randomUUID();
        UploadStatementRequest request = new UploadStatementRequest();
        request.setFile(new MockMultipartFile("file", "statement.pdf", "application/pdf", "content".getBytes()));

        when(s3Client.putObject(any(PutObjectRequest.class), (RequestBody) any()))
                .thenThrow(S3Exception.builder().message("s3 is down").build());

        assertThrows(S3UploadException.class, () -> statementService.uploadStatement(userId, request));

        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadStatement_throwsDocumentSaveException_andRollsBackTheS3Object_whenDatabaseSaveFails() {
        UUID userId = UUID.randomUUID();
        UploadStatementRequest request = new UploadStatementRequest();
        request.setFile(new MockMultipartFile("file", "statement.pdf", "application/pdf", "content".getBytes()));

        when(documentRepository.save(any(Document.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(DocumentSaveException.class, () -> statementService.uploadStatement(userId, request));

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void getStatement_throwsDocumentNotFoundException_whenDocumentDoesNotBelongToUser() {
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        when(documentRepository.existsByIdAndUserId(documentId, userId)).thenReturn(false);

        assertThrows(DocumentNotFoundException.class, () -> statementService.getStatement(userId, documentId));

        verifyNoInteractions(redisService, s3Presigner, documentRetrievalRepository);
    }

    @Test
    void getStatement_returnsCachedResponse_withoutCallingS3_whenAlreadyInRedis() {
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        GetDocumentResponse cached = GetDocumentResponse.builder()
                .url("https://example-bucket.s3.amazonaws.com/" + documentId)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(4)).plusSeconds(30))
                .build();

        when(documentRepository.existsByIdAndUserId(documentId, userId)).thenReturn(true);
        when(redisService.getPreSignedUrl(documentId)).thenReturn(cached);

        ResponseEntity<GetDocumentResponse> response = statementService.getStatement(userId, documentId);

        assertEquals(cached, response.getBody());
        verifyNoInteractions(s3Presigner, documentRetrievalRepository);
    }

    @Test
    void getStatement_generatesAndCachesAPresignedUrl_whenNotAlreadyInRedis() {
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

        ResponseEntity<GetDocumentResponse> response = statementService.getStatement(userId, documentId);

        assertNotNull(response.getBody());
        assertEquals(presignedRequest.url().toString(), response.getBody().getUrl());
        assertEquals(expiresAt, response.getBody().getExpiresAt());

        verify(documentRetrievalRepository).save(any());
        verify(redisService).putPreSignedUrl(any(), any());
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
        UUID userId = UUID.randomUUID();
        Document document = Document.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .uploadedAt(Instant.now())
                .build();

        when(documentRepository.getDocumentsByUserId(userId)).thenReturn(List.of(document));

        ResponseEntity<List<GetUserDocumentsResponse>> response = statementService.getStatements(userId);

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(document.getId(), response.getBody().getFirst().getDocumentId());
        assertEquals(document.getUploadedAt(), response.getBody().getFirst().getUploadedAt());
    }

    @Test
    void getStatements_returnsEmptyList_whenUserHasNoDocuments() {
        UUID userId = UUID.randomUUID();
        when(documentRepository.getDocumentsByUserId(userId)).thenReturn(List.of());

        ResponseEntity<List<GetUserDocumentsResponse>> response = statementService.getStatements(userId);

        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }
}
