package com.zg.ai.entity.po;

import com.zg.ai.entity.po.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.relational.core.mapping.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("chat_history")
public class ChatHistory extends BaseEntity {
    /**
     * 用户ID
     */
    private String userId;

    /**
     * 会话标题
     */
    private String title;
}