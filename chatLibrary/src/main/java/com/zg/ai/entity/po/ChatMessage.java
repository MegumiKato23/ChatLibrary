package com.zg.ai.entity.po;

import com.zg.ai.entity.po.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.relational.core.mapping.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("chat_message")
public class ChatMessage extends BaseEntity {
    /**
     * 会话ID
     */
    private String historyId;

    /**
     * 消息类型 (USER, ASSISTANT)
     */
    private String messageType;

    /**
     * 消息内容
     */
    private String content;
}