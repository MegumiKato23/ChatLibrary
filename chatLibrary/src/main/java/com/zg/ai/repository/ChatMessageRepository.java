package com.zg.ai.repository;

import com.zg.ai.entity.po.ChatMessage;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ChatMessageRepository extends R2dbcRepository<ChatMessage, String> {
    Flux<ChatMessage> findByHistoryIdOrderByCreateAtAsc(String historyId);
    Flux<ChatMessage> findByHistoryId(String historyId);
}