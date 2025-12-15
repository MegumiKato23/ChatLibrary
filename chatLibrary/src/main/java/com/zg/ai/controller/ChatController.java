package com.zg.ai.controller;

import com.zg.ai.entity.ChatHistory;
import com.zg.ai.entity.ChatMessage;
import com.zg.ai.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class ChatController {
    private final ChatService chatService;

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(String prompt, String chatId, @RequestParam(required = false) String userId) {
        return chatService.chat(prompt, chatId, userId);
    }

    @GetMapping("/history/{chatId}")
    public List<ChatMessage> getHistory(@PathVariable String chatId) {
        return chatService.getHistory(chatId);
    }

    @GetMapping("/sessions")
    public List<ChatHistory> getSessions(@RequestParam String userId) {
        return chatService.getUserSessions(userId);
    }

    @DeleteMapping("/session/{chatId}")
    public void deleteSession(@PathVariable String chatId) {
        chatService.deleteSession(chatId);
    }
}
