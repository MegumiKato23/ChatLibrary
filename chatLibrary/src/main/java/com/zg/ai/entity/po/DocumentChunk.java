package com.zg.ai.entity.po;

import com.zg.ai.entity.po.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.relational.core.mapping.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("document_chunk")
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
    private String metadata;
}