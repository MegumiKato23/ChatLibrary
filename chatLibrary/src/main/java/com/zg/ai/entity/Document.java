package com.zg.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zg.ai.entity.base.BaseEntity;
import com.zg.ai.enums.DocumentStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document")
public class Document extends BaseEntity {
    /**
     * 文档名称
     */
    private String documentName;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 文件存储路径
     */
    private String filePath;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * MIME类型
     */
    private String contentType;

    /**
     * 处理状态
     */
    private DocumentStatus status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 总块数
     */
    private Integer totalChunks;

    /**
     * 总页数（仅PDF）
     */
    private Integer totalPages;

    /**
     * 使用的嵌入模型
     */
    private String embeddingModel;

    /**
     * 上传时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadedAt;

    /**
     * 处理完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processedAt;

    /**
     * 上传用户ID（预留）
     */
    private String userId;
}
