package com.zg.ai.entity.dto.chat;

import lombok.Data;

@Data
public class ChatRequest {
    private String prompt;
    private String chatId;
    private String userId;
}
