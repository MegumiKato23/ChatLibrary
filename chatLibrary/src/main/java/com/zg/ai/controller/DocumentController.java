package com.zg.ai.controller;

import com.zg.ai.entity.Document;
import com.zg.ai.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file, @RequestParam(value = "userId", required = false) String userId) {
        documentService.upload(file, userId);
        return ResponseEntity.ok("File uploaded and processed successfully");
    }

    @GetMapping("/list")
    public ResponseEntity<List<Document>> list(@RequestParam(value = "userId", required = false) String userId) {
        return ResponseEntity.ok(documentService.listDocuments(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok("Document deleted successfully");
    }

    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> preview(@PathVariable String id) throws IOException {
        return documentService.getPreviewResource(id);
    }
}
