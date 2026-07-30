package com.lvmp.customerstatements_springboot.service;

import com.lvmp.customerstatements_springboot.model.request.UploadStatementRequest;
import com.lvmp.customerstatements_springboot.model.response.GetDocumentResponse;
import com.lvmp.customerstatements_springboot.model.response.UploadDocumentResponse;
import com.lvmp.customerstatements_springboot.exception.S3UploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatementService {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    @Value("${app.s3.bucket-name}")
    private String bucketName;

    public ResponseEntity<UploadDocumentResponse> uploadStatement(UploadStatementRequest request) throws IOException {
        String documentId = UUID.randomUUID().toString();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(documentId)
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
        } catch (S3Exception e) {
            log.error("Failed to upload statement ({}) to s3", documentId, e);
            throw new S3UploadException("We couldn't upload your statement right now. Please try again later.");
        }

        return ResponseEntity.ok().body(UploadDocumentResponse.builder()
                .documentId(documentId)
                .build());
    }

    public ResponseEntity<GetDocumentResponse> getStatements(String documentId) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(documentId)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(presignRequest);

        return ResponseEntity.ok().body(GetDocumentResponse.builder()
                .url(presignedGetObjectRequest.url().toString())
                .build());
    }
}
