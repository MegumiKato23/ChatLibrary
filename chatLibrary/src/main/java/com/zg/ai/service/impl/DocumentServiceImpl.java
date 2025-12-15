package com.zg.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zg.ai.entity.Document;
import com.zg.ai.entity.DocumentChunk;
import com.zg.ai.enums.DocumentStatus;
import com.zg.ai.mapper.DocumentChunkMapper;
import com.zg.ai.mapper.DocumentMapper;
import com.zg.ai.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements DocumentService {

    private final VectorStore vectorStore;
    private final DocumentChunkMapper documentChunkMapper;
    private final String STORAGE_DIR = "data/uploads";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upload(MultipartFile file, String userId) {
        String originalFilename = file.getOriginalFilename();
        String fileType = originalFilename != null && originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1) : "";
        String newFilename = UUID.randomUUID().toString() + (fileType.isEmpty() ? "" : "." + fileType);
        Path storagePath = Paths.get(STORAGE_DIR).toAbsolutePath().normalize();
        
        try {
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }
            
            Path destination = storagePath.resolve(newFilename);
            file.transferTo(destination.toFile());

            Document document = new Document();
            document.setDocumentName(originalFilename);
            document.setOriginalFilename(originalFilename);
            document.setFilePath(destination.toString());
            document.setFileType(fileType);
            document.setFileSize(file.getSize());
            document.setContentType(file.getContentType());
            document.setStatus(DocumentStatus.PROCESSING);
            document.setUploadedAt(LocalDateTime.now());
            if (userId != null) {
                document.setUserId(userId);
            }
            this.save(document);

            try {
                // Parse and vectorize
                DocumentReader reader = new TikaDocumentReader(new FileSystemResource(destination));
                List<org.springframework.ai.document.Document> documents = reader.get();
                
                TokenTextSplitter splitter = new TokenTextSplitter();
                List<org.springframework.ai.document.Document> splitDocuments = splitter.apply(documents);
                
                List<DocumentChunk> chunksToSave = new ArrayList<>();
                int chunkIndex = 0;
                
                // Add metadata to each chunk and prepare for MySQL persistence
                for (org.springframework.ai.document.Document doc : splitDocuments) {
                    doc.getMetadata().put("documentId", document.getId());
                    doc.getMetadata().put("filename", originalFilename);
                    doc.getMetadata().put("vectorId", doc.getId());
                    // Add content type to metadata for filtering if needed
                    doc.getMetadata().put("contentType", file.getContentType());
                    
                    // Create DocumentChunk entity
                    DocumentChunk chunk = new DocumentChunk();
                    chunk.setDocumentId(document.getId());
                    chunk.setChunkIndex(chunkIndex++);
                    chunk.setContent(doc.getText());
                    // Rough token count estimate or get from metadata if available (omitted here as optional)
                    chunk.setTokenCount(doc.getText().length() / 4); 
                    chunk.setMetadata(doc.getMetadata());
                    
                    documentChunkMapper.insert(chunk);
                }

                // Add to Vector Store (Qdrant)
                vectorStore.add(splitDocuments);

                document.setStatus(DocumentStatus.PROCESSED);
                document.setProcessedAt(LocalDateTime.now());
                document.setTotalChunks(splitDocuments.size());
                this.updateById(document);

            } catch (Exception e) {
                log.error("Error processing document", e);
                document.setStatus(DocumentStatus.FAILED);
                document.setErrorMessage(e.getMessage());
                this.updateById(document);
                throw new RuntimeException("Document processing failed", e);
            }

        } catch (IOException e) {
            log.error("Error uploading file", e);
            throw new RuntimeException("File upload failed", e);
        }
    }

    @Override
    public void upload(MultipartFile file) {
        this.upload(file, null);
    }

    @Override
    public List<Document> listDocuments(String userId) {
        QueryWrapper<Document> queryWrapper = new QueryWrapper<>();
        if (userId != null) {
            queryWrapper.eq("user_id", userId);
        }
        queryWrapper.orderByDesc("create_at");
        return this.list(queryWrapper);
    }

    @Override
    public ResponseEntity<Resource> getPreviewResource(String id) throws IOException {
        Document document = this.getById(id);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = Paths.get(document.getFilePath());
        Resource resource;
        try {
            resource = new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error reading file", e);
        }

        if (resource.exists() || resource.isReadable()) {
            String contentType = document.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            // For PDF, TXT, MD, we want to display inline
            // For others, maybe download or let frontend handle
            // Actually, we'll try to serve everything inline if possible, or let browser decide
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getOriginalFilename() + "\"")
                    .body(resource);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(String id) {
        // 1. Get all chunks to find vector IDs
        List<DocumentChunk> chunks = documentChunkMapper.selectList(new QueryWrapper<DocumentChunk>().eq("document_id", id));
        
        if (chunks != null && !chunks.isEmpty()) {
            List<String> vectorIds = new ArrayList<>();
            for (DocumentChunk chunk : chunks) {
                if (chunk.getMetadata() != null && chunk.getMetadata().containsKey("vectorId")) {
                    vectorIds.add((String) chunk.getMetadata().get("vectorId"));
                }
            }
            // 2. Delete from Vector Store
            if (!vectorIds.isEmpty()) {
                vectorStore.delete(vectorIds);
            }
            // 3. Delete chunks from DB
            documentChunkMapper.delete(new QueryWrapper<DocumentChunk>().eq("document_id", id));
        }

        // 4. Delete file
        Document document = this.getById(id);
        if (document != null && document.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(document.getFilePath()));
            } catch (IOException e) {
                log.warn("Failed to delete file: " + document.getFilePath(), e);
            }
        }

        // 5. Delete document record
        this.removeById(id);
    }
}
