package com.zg.ai.entity.dto.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class DocumentUploadDTO {
    @NotBlank(message = "文档名称不能为空")
    @Size(max = 255, message = "文档名称长度不能超过255")
    private String documentName;

    private String fileName;

    @NotNull(message = "文件不能为空")
    private MultipartFile file;
}
