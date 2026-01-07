package com.zg.ai.service;

import com.zg.ai.entity.dto.chat.ChatMessageDTO;
import com.zg.ai.entity.dto.chat.ChatHistoryDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatService {
    Flux<String> chat(String prompt, String chatId, String userId);

    Mono<String> createConversation(String userId, String title);

    Flux<ChatHistoryDTO> getConversations(String userId);

    Flux<ChatMessageDTO> getMessages(String historyId);

    Mono<Void> deleteConversation(String historyId);
}