package com.lvmp.customerstatements_springboot.controller;

import com.lvmp.customerstatements_springboot.model.request.UploadStatementRequest;
import com.lvmp.customerstatements_springboot.model.response.GetDocumentResponse;
import com.lvmp.customerstatements_springboot.model.response.GetUserDocumentsResponse;
import com.lvmp.customerstatements_springboot.model.response.UploadDocumentResponse;
import com.lvmp.customerstatements_springboot.persistence.Document;
import com.lvmp.customerstatements_springboot.service.StatementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
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
    public ResponseEntity<List<GetUserDocumentsResponse>> getStatements(@AuthenticationPrincipal UUID userID) {
        return statementService.getStatements(userID);
    }
}
