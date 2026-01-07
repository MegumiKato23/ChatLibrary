package com.zg.ai.service.impl;

import com.zg.ai.entity.dto.chat.ChatHistoryDTO;
import com.zg.ai.entity.dto.chat.ChatMessageDTO;
import com.zg.ai.entity.po.ChatHistory;
import com.zg.ai.entity.po.ChatMessage;
import com.zg.ai.repository.ChatHistoryRepository;
import com.zg.ai.repository.ChatMessageRepository;
import com.zg.ai.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ChatHistoryRepository chatHistoryRepository;
    private final ChatMessageRepository chatMessageRepository;
    
    // Simple in-memory cache for retrieval results (key: prompt, value: documents)
    private final Map<String, List<Document>> retrievalCache = new ConcurrentHashMap<>();

    @Override
    public Flux<String> chat(String prompt, String chatId, String userId) {
        // 1. Ensure Chat History exists
        return chatHistoryRepository.findById(chatId)
                .switchIfEmpty(Mono.defer(() -> {
                    ChatHistory newHistory = new ChatHistory();
                    newHistory.setId(chatId); // Use provided ID or generate? Client usually sends ID.
                    newHistory.setUserId(userId);
                    newHistory.setTitle(prompt.length() > 20 ? prompt.substring(0, 20) + "..." : prompt);
                    newHistory.setUpdateAt(LocalDateTime.now());
                    return chatHistoryRepository.save(newHistory);
                }))
                .flatMap(history -> {
                    // 2. Save User Message
                    ChatMessage userMessage = new ChatMessage();
                    userMessage.setHistoryId(chatId);
                    userMessage.setMessageType("USER");
                    userMessage.setContent(prompt);
                    userMessage.setUpdateAt(LocalDateTime.now());
                    return chatMessageRepository.save(userMessage).thenReturn(history);
                })
                .flatMapMany(history -> {
                    // 3. RAG Retrieval (Blocking call to VectorStore, wrap in boundedElastic)
                    return Mono.fromCallable(() -> {
                        return retrievalCache.computeIfAbsent(prompt, k -> {
                            SearchRequest searchRequest = SearchRequest.builder()
                                    .query(k)
                                    .topK(4)
                                    .similarityThreshold(0.6)
                                    .build();
                            return vectorStore.similaritySearch(searchRequest);
                        });
                    }).subscribeOn(Schedulers.boundedElastic())
                    .flatMapMany(similarDocuments -> {
                        String context = similarDocuments.stream()
                                .map(Document::getText)
                                .collect(Collectors.joining("\n\n"));

                        // 4. Construct Prompt
                        String systemPrompt = String.format("""
                                你是本地知识库问答机器人，你的名字叫加藤惠。
                                请根据以下参考文档回答用户的问题。
                                如果参考文档中没有相关信息，请根据你的知识回答，但要说明"文档中未找到相关信息"。
                                
                                参考文档：
                                %s
                                """, context);

                        // 5. Call ChatClient
                        StringBuilder fullResponse = new StringBuilder();
                        return chatClient.prompt()
                                .system(systemPrompt)
                                .user(prompt)
                                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                                .stream()
                                .content()
                                .doOnNext(fullResponse::append)
                                .doOnComplete(() -> {
                                    // 6. Save Assistant Message
                                    ChatMessage aiMessage = new ChatMessage();
                                    aiMessage.setHistoryId(chatId);
                                    aiMessage.setMessageType("ASSISTANT");
                                    aiMessage.setContent(fullResponse.toString());
                                    // Async save
                                    chatMessageRepository.save(aiMessage)
                                            .subscribe(
                                                    saved -> log.debug("Saved AI message"),
                                                    err -> log.error("Failed to save AI message", err)
                                            );
                                    
                                    if (retrievalCache.size() > 1000) {
                                        retrievalCache.clear();
                                    }
                                })
                                .doOnError(e -> log.error("Error in chat stream", e));
                    });
                });
    }

    @Override
    public Mono<String> createConversation(String userId, String title) {
        ChatHistory chatHistory = new ChatHistory();
        chatHistory.setUserId(userId);
        chatHistory.setTitle(title != null && !title.isEmpty() ? title : "New Chat");
        return chatHistoryRepository.save(chatHistory).map(ChatHistory::getId);
    }

    @Override
    public Flux<ChatHistoryDTO> getConversations(String userId) {
        return chatHistoryRepository.findByUserIdOrderByUpdateAtDesc(userId)
                .map(this::convertToDTO);
    }

    @Override
    public Flux<ChatMessageDTO> getMessages(String historyId) {
        return chatMessageRepository.findByHistoryIdOrderByCreateAtAsc(historyId)
                .map(this::convertToDTO);
    }

    @Override
    public Mono<Void> deleteConversation(String historyId) {
        return chatMessageRepository.findByHistoryId(historyId) // Find messages to delete? Or deleteAll by criteria?
                // R2DBC repository doesn't support delete by criteria directly without custom query.
                // But we can delete by ID or find and delete.
                // Let's iterate and delete or use custom query if available.
                // For simplicity:
                .flatMap(msg -> chatMessageRepository.delete(msg))
                .then(chatHistoryRepository.deleteById(historyId));
    }

    private ChatHistoryDTO convertToDTO(ChatHistory history) {
        ChatHistoryDTO dto = new ChatHistoryDTO();
        dto.setId(history.getId());
        dto.setTitle(history.getTitle());
        dto.setCreateTime(history.getCreateAt());
        dto.setUpdateTime(history.getUpdateAt());
        return dto;
    }

    private ChatMessageDTO convertToDTO(ChatMessage message) {
        ChatMessageDTO dto = new ChatMessageDTO();
        BeanUtils.copyProperties(message, dto);
        return dto;
    }
}