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

/**
 * 聊天管理控制器：提供 AI 问答、会话创建、历史记录查询及删除接口
 */
@Tag(name = "Chat Management", description = "聊天管理接口")
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class ChatController {
    private final ChatService chatService;

    @Operation(summary = "开始聊天")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request) {
        // 从请求体中获取参数并调用 AI 服务
        return chatService.chat(request.getPrompt(), request.getChatId(), request.getUserId());
    }

    @Operation(summary = "创建会话")
    @PostMapping("/conversation")
    public Mono<Result<String>> createConversation(@RequestParam String userId,
            @RequestParam(required = false) String title) {
        // 从请求参数中获取用户 ID 和会话标题，并调用服务创建会话
        return chatService.createConversation(userId, title).map(Result::success);
    }

    @Operation(summary = "获取用户会话列表")
    @GetMapping("/conversations")
    public Mono<Result<List<ChatHistoryDTO>>> getConversations(@RequestParam String userId) {
        // 从请求参数中获取用户 ID，并调用服务获取会话列表
        return chatService.getConversations(userId).collectList().map(Result::success);
    }

    @Operation(summary = "获取聊天历史")
    @GetMapping("/conversation/history/{historyId}")
    public Mono<Result<List<ChatMessageDTO>>> getHistory(@PathVariable String historyId) {
        // 获取某个会话的所有消息记录
        return chatService.getMessages(historyId).collectList().map(Result::success);
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/conversation/history/{historyId}")
    public Mono<Result<Void>> deleteConversation(@PathVariable String historyId) {
        // 从路径变量中获取会话 ID，并调用服务删除会话
        return chatService.deleteConversation(historyId)
                .thenReturn(Result.<Void>success());
    }
}