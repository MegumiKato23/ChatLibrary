package com.zg.ai.controller;

import com.zg.ai.common.Result;
import com.zg.ai.entity.po.Document;
import com.zg.ai.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Document Management", description = "文档管理接口")
@RestController
@RequestMapping("/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "上传文档")
    @PostMapping(value = "/upload/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Result<Document>> upload(@RequestPart("file") FilePart filePart,
            @PathVariable("userId") String userId) {
        return documentService.upload(filePart, userId).map(Result::success);
    }

    @Operation(summary = "获取文档列表")
    @GetMapping("/list")
    public Mono<Result<List<Document>>> list(@RequestParam(value = "userId", required = false) String userId) {
        return documentService.listDocuments(userId).collectList().map(Result::success);
    }

    @Operation(summary = "获取所有文档列表")
    @GetMapping("/list/all")
    public Mono<Result<List<Document>>> listAll() {
        return documentService.listAllDocuments().collectList().map(Result::success);
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/{id}")
    public Mono<Result<String>> delete(@PathVariable String id) {
        return documentService.deleteDocument(id)
                .thenReturn(Result.success("Document deleted successfully"));
    }

     @Operation(summary = "预览文档")
     @GetMapping("/preview/{id}")
     public Mono<ResponseEntity<Resource>> preview(@PathVariable String id) {
     return documentService.getPreviewResource(id);
     }

    @Operation(summary = "获取文档预览内容(Text/HTML)")
    @GetMapping("/preview/content/{id}")
    public Mono<Result<String>> getPreviewContent(@PathVariable String id) {
        return documentService.getPreviewContent(id).map(Result::success);
    }
}