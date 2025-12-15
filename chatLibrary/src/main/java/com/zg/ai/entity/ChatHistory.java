package com.zg.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zg.ai.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_history")
public class ChatHistory extends BaseEntity {
    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 会话标题
     */
    private String title;

    // Use createAt from BaseEntity, removed redundant createdAt field
}
