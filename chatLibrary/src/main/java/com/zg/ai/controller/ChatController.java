package com.zg.ai.controller;

import com.zg.ai.common.Result;
import com.zg.ai.entity.dto.chat.ChatHistoryDTO;
import com.zg.ai.entity.dto.chat.ChatMessageDTO;
import com.zg.ai.entity.dto.chat.ChatRequest;
import com.zg.ai.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Tag(name = "Chat Management", description = "聊天管理接口")
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class ChatController {
    private final ChatService chatService;

    @Operation(summary = "开始聊天")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request) {
        return chatService.chat(request.getPrompt(), request.getChatId(), request.getUserId());
    }

    @Operation(summary = "创建会话")
    @PostMapping("/conversation")
    public Mono<Result<String>> createConversation(@RequestParam String userId,
            @RequestParam(required = false) String title) {
        return chatService.createConversation(userId, title).map(Result::success);
    }

    @Operation(summary = "获取用户会话列表")
    @GetMapping("/conversations")
    public Mono<Result<List<ChatHistoryDTO>>> getConversations(@RequestParam String userId) {
        return chatService.getConversations(userId).collectList().map(Result::success);
    }

    @Operation(summary = "获取聊天历史")
    @GetMapping("/conversation/history/{historyId}")
    public Mono<Result<List<ChatMessageDTO>>> getHistory(@PathVariable String historyId) {
        return chatService.getMessages(historyId).collectList().map(Result::success);
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/conversation/history/{historyId}")
    public Mono<Result<Void>> deleteConversation(@PathVariable String historyId) {
        return chatService.deleteConversation(historyId)
                .thenReturn(Result.<Void>success());
    }
}