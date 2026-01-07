package com.zg.ai.repository;

import com.zg.ai.entity.po.ChatHistory;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ChatHistoryRepository extends R2dbcRepository<ChatHistory, String> {
    Flux<ChatHistory> findByUserIdOrderByUpdateAtDesc(String userId);
}