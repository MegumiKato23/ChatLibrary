package com.zg.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zg.ai.entity.Document;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;

public interface DocumentService extends IService<Document> {
    void upload(MultipartFile file);

    void upload(MultipartFile file, String userId);
    
    void deleteDocument(String id);

    List<Document> listDocuments(String userId);

    ResponseEntity<Resource> getPreviewResource(String id) throws IOException;
}
