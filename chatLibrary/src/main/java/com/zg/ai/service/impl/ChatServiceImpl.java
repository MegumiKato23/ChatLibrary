package com.zg.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zg.ai.entity.ChatHistory;
import com.zg.ai.entity.ChatMessage;
import com.zg.ai.mapper.ChatHistoryMapper;
import com.zg.ai.mapper.ChatMessageMapper;
import com.zg.ai.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ChatHistoryMapper chatHistoryMapper;
    private final ChatMessageMapper chatMessageMapper;

    @Override
    public Flux<String> chat(String prompt, String chatId, String userId) {
        // 1. Ensure Chat History exists
        ChatHistory chatHistory = chatHistoryMapper.selectOne(new QueryWrapper<ChatHistory>().eq("session_id", chatId));
        if (chatHistory == null) {
            chatHistory = new ChatHistory();
            chatHistory.setSessionId(chatId);
            chatHistory.setUserId(userId);
            chatHistory.setTitle(prompt.length() > 20 ? prompt.substring(0, 20) + "..." : prompt);
            chatHistoryMapper.insert(chatHistory);
        }

        // 2. Save User Message
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(chatId);
        userMessage.setMessageType("USER");
        userMessage.setContent(prompt);
        chatMessageMapper.insert(userMessage);

        // 3. RAG Retrieval
        List<org.springframework.ai.document.Document> similarDocuments = vectorStore.similaritySearch(prompt);
        String context = similarDocuments.stream()
                .map(org.springframework.ai.document.Document::getText)
                .collect(Collectors.joining("\n"));

        // 4. Construct Prompt
        String systemPrompt = "你是本地知识库问答机器人，你的名字叫加藤惠。请根据以下参考文档回答用户的问题。如果参考文档中没有相关信息，请根据你的知识回答。\n\n参考文档：\n" + context;

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
                    aiMessage.setSessionId(chatId);
                    aiMessage.setMessageType("ASSISTANT");
                    aiMessage.setContent(fullResponse.toString());
                    chatMessageMapper.insert(aiMessage);
                });
    }

    @Override
    public List<ChatMessage> getHistory(String chatId) {
        return chatMessageMapper.selectList(new QueryWrapper<ChatMessage>()
                .eq("session_id", chatId)
                .orderByAsc("create_at"));
    }

    @Override
    public List<ChatHistory> getUserSessions(String userId) {
        return chatHistoryMapper.selectList(new QueryWrapper<ChatHistory>()
                .eq("user_id", userId)
                .orderByDesc("create_at"));
    }

    @Override
    public void deleteSession(String chatId) {
        chatHistoryMapper.delete(new QueryWrapper<ChatHistory>().eq("session_id", chatId));
        chatMessageMapper.delete(new QueryWrapper<ChatMessage>().eq("session_id", chatId));
    }
}
