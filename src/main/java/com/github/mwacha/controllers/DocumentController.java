package com.github.mwacha.controllers;

import com.github.mwacha.services.DocumentStoreService;
import com.github.mwacha.services.QueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class DocumentController {

    private final DocumentStoreService documentStoreService;
    private final QueryService queryService;

    public DocumentController(DocumentStoreService documentStoreService, QueryService queryService) {
        this.documentStoreService = documentStoreService;
        this.queryService = queryService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        documentStoreService.storeDocument(file);
        return ResponseEntity.ok("Documento armazenado com sucesso.");
    }

    @GetMapping("/ask")
    public ResponseEntity<String> ask(@RequestParam("q") String question) {
        String answer = queryService.ask(question);
        return ResponseEntity.ok(answer);
    }
}
