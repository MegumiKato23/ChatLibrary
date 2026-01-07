package com.zg.ai.repository;

import com.zg.ai.entity.po.DocumentChunk;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface DocumentChunkRepository extends R2dbcRepository<DocumentChunk, String> {
    Flux<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(String documentId);
    Flux<DocumentChunk> findByDocumentId(String documentId);
}