package com.zg.ai.service;

import com.zg.ai.entity.ChatHistory;
import com.zg.ai.entity.ChatMessage;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatService {
    Flux<String> chat(String prompt, String chatId, String userId);

    List<ChatMessage> getHistory(String chatId);

    List<ChatHistory> getUserSessions(String userId);

    void deleteSession(String chatId);
}
