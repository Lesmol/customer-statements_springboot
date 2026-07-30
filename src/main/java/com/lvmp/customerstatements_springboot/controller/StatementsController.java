package com.lvmp.customerstatements_springboot.controller;

import com.lvmp.customerstatements_springboot.model.request.UploadStatementRequest;
import com.lvmp.customerstatements_springboot.model.response.GetDocumentResponse;
import com.lvmp.customerstatements_springboot.model.response.UploadDocumentResponse;
import com.lvmp.customerstatements_springboot.service.StatementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("api/statements/v1")
@RequiredArgsConstructor
public class StatementsController {
    private final StatementService statementService;

    @PostMapping("/upload-document")
    public ResponseEntity<UploadDocumentResponse> uploadStatement(@Valid @ModelAttribute UploadStatementRequest request) throws IOException {
        return statementService.uploadStatement(request);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<GetDocumentResponse> getStatement(@PathVariable String documentId) {
        return statementService.getStatements(documentId);
    }
}
