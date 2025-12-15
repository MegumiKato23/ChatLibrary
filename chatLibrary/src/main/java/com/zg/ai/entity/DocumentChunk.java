package com.zg.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.zg.ai.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "document_chunk", autoResultMap = true)
public class DocumentChunk extends BaseEntity {

    /**
     * 关联的文档ID
     */
    private String documentId;

    /**
     * 块索引（顺序）
     */
    private Integer chunkIndex;

    /**
     * 块内容
     */
    private String content;

    /**
     * token数量
     */
    private Integer tokenCount;

    /**
     * 元数据（JSON存储）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;
}
