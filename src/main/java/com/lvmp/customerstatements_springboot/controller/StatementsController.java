package com.lvmp.customerstatements_springboot.controller;

import com.lvmp.customerstatements_springboot.model.request.UploadStatementRequest;
import com.lvmp.customerstatements_springboot.model.response.GetDocumentResponse;
import com.lvmp.customerstatements_springboot.model.response.GetUserDocumentsResponse;
import com.lvmp.customerstatements_springboot.model.response.PageResponse;
import com.lvmp.customerstatements_springboot.model.response.UploadDocumentResponse;
import com.lvmp.customerstatements_springboot.service.StatementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("api/statements/v1")
@RequiredArgsConstructor
public class StatementsController {
    private final StatementService statementService;

    @PostMapping("/upload-document")
    public ResponseEntity<UploadDocumentResponse> uploadStatement(@AuthenticationPrincipal UUID userID, @Valid @ModelAttribute UploadStatementRequest request) throws IOException {
        return statementService.uploadStatement(userID, request);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<GetDocumentResponse> getStatement(@AuthenticationPrincipal UUID userID, @PathVariable UUID documentId) {
        return statementService.getStatement(userID, documentId);
    }

    @GetMapping("/documents")
    public ResponseEntity<PageResponse<GetUserDocumentsResponse>> getStatements(
            @AuthenticationPrincipal UUID userID,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return statementService.getStatements(userID, page, size);
    }
}
