package com.zg.ai.entity.po;

import com.zg.ai.entity.po.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.relational.core.mapping.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("document")
public class Document extends BaseEntity {
    /**
     * 用户ID
     */
    private String userId;

    /**
     * 文档名称
     */
    private String documentName;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 内容类型 (MIME type)
     */
    private String contentType;

    /**
     * 状态 (0:UPLOADED, 1:PROCESSING, 2:PROCESSED, 3:FAILED, 4:DELETED)
     */
    private Integer status;

    /**
     * 总块数
     */
    private Integer totalChunks;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 嵌入模型名称
     */
    private String embeddingModel;
}
