package com.zg.ai.entity.dto.chat;

import com.zg.ai.entity.dto.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChatMessageDTO extends BaseDTO {
    private String messageType;
    private String content;
}
