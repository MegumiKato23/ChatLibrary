package com.zg.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zg.ai.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message")
public class ChatMessage extends BaseEntity {
    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 消息类型 (USER, ASSISTANT)
     */
    private String messageType;

    /**
     * 消息内容
     */
    private String content;
}
