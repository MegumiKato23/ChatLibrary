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

/**
 * 聊天服务实现类：处理 RAG 检索、对话历史管理及 AI 流式响应
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ChatHistoryRepository chatHistoryRepository;
    private final ChatMessageRepository chatMessageRepository;
    
    // 缓存 RAG 检索结果，键为用户输入，值为文档列表
    private final Map<String, List<Document>> retrievalCache = new ConcurrentHashMap<>();

    @Override
    public Flux<String> chat(String prompt, String chatId, String userId) {
        // 1. 确保对话历史存在，若不存在则创建新历史记录
        return chatHistoryRepository.findById(chatId)
                .switchIfEmpty(Mono.defer(() -> {
                    ChatHistory newHistory = new ChatHistory();
                    newHistory.setId(chatId); 
                    newHistory.setUserId(userId);
                    newHistory.setTitle(prompt.length() > 20 ? prompt.substring(0, 20) + "..." : prompt);
                    newHistory.setUpdateAt(LocalDateTime.now());
                    return chatHistoryRepository.save(newHistory);
                }))
                .flatMap(history -> {
                    // 2. 保存用户消息
                    ChatMessage userMessage = new ChatMessage();
                    userMessage.setHistoryId(chatId);
                    userMessage.setMessageType("USER");
                    userMessage.setContent(prompt);
                    userMessage.setUpdateAt(LocalDateTime.now());
                    return chatMessageRepository.save(userMessage).thenReturn(history);
                })
                .flatMapMany(history -> {
                    // 3. RAG 检索（阻塞式调用 VectorStore，在 boundedElastic 线程池执行）
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

                        // 4. 构造系统提示词
                        String systemPrompt = String.format("""
                                你是本地知识库问答机器人，你的名字叫加藤惠。
                                请根据以下参考文档回答用户的问题。
                                如果参考文档中没有相关信息，请根据你的知识回答，但要说明"文档中未找到相关信息"。
                                
                                参考文档：
                                %s
                                """, context);

                        // 5. 调用 ChatClient 生成流式响应
                        StringBuilder fullResponse = new StringBuilder();
                        return chatClient.prompt()
                                .system(systemPrompt)
                                .user(prompt)
                                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                                .stream()
                                .content()
                                .doOnNext(fullResponse::append)
                                .doOnComplete(() -> {
                                    // 6. 保存助手消息
                                    ChatMessage aiMessage = new ChatMessage();
                                    aiMessage.setHistoryId(chatId);
                                    aiMessage.setMessageType("ASSISTANT");
                                    aiMessage.setContent(fullResponse.toString());

                                    chatMessageRepository.save(aiMessage)
                                            .subscribe(
                                                    saved -> log.debug("成功保存 AI 消息: {}", saved),
                                                    err -> log.error("保存 AI 消息失败: {}", err.getMessage(), err)
                                            );
                                    
                                    if (retrievalCache.size() > 1000) {
                                        retrievalCache.clear();
                                    }
                                })
                                .doOnError(e -> log.error("聊天流处理出错: {}", e.getMessage(), e));
                    });
                });
    }

    // 创建新对话
    @Override
    public Mono<String> createConversation(String userId, String title) {
        ChatHistory chatHistory = new ChatHistory();
        chatHistory.setUserId(userId);
        chatHistory.setTitle(title != null && !title.isEmpty() ? title : "新对话");
        return chatHistoryRepository.save(chatHistory).map(ChatHistory::getId);
    }

    // 获取用户对话列表
    @Override
    public Flux<ChatHistoryDTO> getConversations(String userId) {
        return chatHistoryRepository.findByUserIdOrderByUpdateAtDesc(userId)
                .map(this::convertToDTO);
    }

    // 获取对话消息列表
    @Override
    public Flux<ChatMessageDTO> getMessages(String historyId) {
        return chatMessageRepository.findByHistoryIdOrderByCreateAtAsc(historyId)
                .map(this::convertToDTO);
    }

    // 删除对话
    @Override
    public Mono<Void> deleteConversation(String historyId) {
        return chatMessageRepository.findByHistoryId(historyId) 
                .flatMap(msg -> chatMessageRepository.delete(msg))
                .then(chatHistoryRepository.deleteById(historyId));
    }

    private ChatHistoryDTO convertToDTO(ChatHistory history) {
        ChatHistoryDTO dto = new ChatHistoryDTO();
        BeanUtils.copyProperties(history, dto);
        return dto;
    }

    private ChatMessageDTO convertToDTO(ChatMessage message) {
        ChatMessageDTO dto = new ChatMessageDTO();
        BeanUtils.copyProperties(message, dto);
        return dto;
    }
}