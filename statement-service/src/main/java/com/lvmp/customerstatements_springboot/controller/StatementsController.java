package com.lvmp.customerstatements_springboot.controller;

import com.lvmp.customerstatements_springboot.model.request.UploadStatementRequest;
import com.lvmp.customerstatements_springboot.model.response.GetDocumentResponse;
import com.lvmp.customerstatements_springboot.model.response.GetUserDocumentsResponse;
import com.lvmp.customerstatements_springboot.model.response.PageResponse;
import com.lvmp.customerstatements_springboot.model.response.UploadDocumentResponse;
import com.lvmp.customerstatements_springboot.security.AdminAccessGuard;
import com.lvmp.customerstatements_springboot.service.StatementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("api/statements/v1")
@RequiredArgsConstructor
public class StatementsController {
    private final StatementService statementService;
    private final AdminAccessGuard adminAccessGuard;

    @PostMapping("/upload-document")
    public ResponseEntity<UploadDocumentResponse> uploadStatement(@RequestHeader("X-User-Id") UUID userId, @Valid @ModelAttribute UploadStatementRequest request) throws NoSuchAlgorithmException, IOException {
        adminAccessGuard.requireAdmin(userId);
        return statementService.uploadStatement(request);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<GetDocumentResponse> getStatement(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID documentId) {
        return statementService.getStatement(userId, documentId);
    }

    @GetMapping("/documents")
    public ResponseEntity<PageResponse<GetUserDocumentsResponse>> getStatements(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return statementService.getStatements(userId, page, size);
    }
}