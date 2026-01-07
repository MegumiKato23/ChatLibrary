package com.zg.ai.service;

import com.zg.ai.entity.po.Document;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DocumentService {
    Mono<Document> upload(FilePart filePart, String userId);

    Mono<Void> deleteDocument(String id);

    Flux<Document> listDocuments(String userId);

    Flux<Document> listAllDocuments();

     Mono<ResponseEntity<Resource>> getPreviewResource(String id);

    Mono<String> getPreviewContent(String id);
}