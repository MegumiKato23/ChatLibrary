package com.zg.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zg.ai.entity.po.Document;
import com.zg.ai.entity.po.DocumentChunk;
import com.zg.ai.enums.DocumentStatus;
import com.zg.ai.repository.DocumentChunkRepository;
import com.zg.ai.repository.DocumentRepository;
import com.zg.ai.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.PicturesManager;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.hwpf.usermodel.PictureType;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * 文档服务实现类：处理文件上传、解析、分块及向量化存储
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final VectorStore vectorStore;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ObjectMapper objectMapper;
    private final String STORAGE_DIR = "data/uploads";
    private final Set<String> SUPPORTED_FILE_TYPES = Set.of(
            "pdf", "docx", "doc", "txt", "pptx", "ppt", "md");

    @Override
    @Transactional
    public Mono<Document> upload(FilePart filePart, String userId) {
        // 获取文件名和类型
        String fileName = filePart.filename();
        String fileType = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".") + 1) : "";

        // 校验文件类型
        if (!SUPPORTED_FILE_TYPES.contains(fileType.toLowerCase())) {
            return Mono.error(new IllegalArgumentException("不支持该文件类型:" + fileType));
        }

        // 生成存储路径
        String newFilename = UUID.randomUUID().toString() + (fileType.isEmpty() ? "" : "." + fileType);
        Path storagePath = Paths.get(STORAGE_DIR);
        Path filePath = storagePath.resolve(newFilename);

        // 保存文件到指定路径
        return Mono.fromCallable(() -> {
            Files.createDirectories(storagePath);
            return filePath;
        }).subscribeOn(Schedulers.boundedElastic())
                .flatMap(path -> {
                    // 初始化文档记录
                    Document document = new Document();
                    document.setDocumentName(newFilename); 
                    document.setOriginalFilename(fileName); 
                    document.setUserId(userId);
                    document.setStatus(DocumentStatus.PROCESSING.getCode());
                    document.setFileType(fileType);
                    document.setFilePath(filePath.toString());

                    // 流式写入文件并保存记录
                    return DataBufferUtils.write(filePart.content(), filePath,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.TRUNCATE_EXISTING)
                            .then(Mono.defer(() -> {
                                try {
                                    document.setFileSize(Files.size(filePath));
                                    String contentType = Files.probeContentType(filePath);
                                    document.setContentType(contentType);
                                } catch (IOException e) {
                                    log.error("获取文件信息失败", e);
                                }
                                return documentRepository.save(document);
                            }))
                            .doOnSuccess(doc -> {
                                // 异步处理文档
                                Mono.fromRunnable(() -> processDocument(doc))
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .subscribe();
                            });
                });
    }

    // 异步解析文档：提取文本、分块、存储到数据库和向量库
    private void processDocument(Document document) {
        try {
            // 1.使用 Tika 提取文本内容
            Resource resource = new FileSystemResource(document.getFilePath());
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<org.springframework.ai.document.Document> aiDocuments = reader.get();

            if (aiDocuments.isEmpty()) {
                updateStatus(document, DocumentStatus.FAILED).subscribe();
                return;
            }

            // 2. 文本分块处理
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<org.springframework.ai.document.Document> chunks = splitter.apply(aiDocuments);

            // 3. 保存分块到数据库和向量库
            List<DocumentChunk> dbChunks = new ArrayList<>();
            List<org.springframework.ai.document.Document> docsToStore = new ArrayList<>();

            int index = 0;
            for (org.springframework.ai.document.Document chunk : chunks) {
                if (chunk.getText() == null) {
                    continue;
                }
                DocumentChunk dbChunk = new DocumentChunk();
                dbChunk.setDocumentId(document.getId());
                dbChunk.setChunkIndex(index++);
                dbChunk.setContent(chunk.getText());
                dbChunk.setTokenCount(chunk.getText().length());

                Map<String, Object> metadata = chunk.getMetadata();
                metadata.put("originalFilename", document.getDocumentName());
                metadata.put("userId", document.getUserId());
                String vectorId = UUID.randomUUID().toString();
                metadata.put("vectorId", vectorId);

                try {
                    dbChunk.setMetadata(objectMapper.writeValueAsString(metadata));
                } catch (JsonProcessingException e) {
                    log.error("文档分块元数据序列化失败", e);
                    dbChunk.setMetadata("{}");
                }

                dbChunks.add(dbChunk);

                // 构建 Spring AI 向量库文档对象
                org.springframework.ai.document.Document docToStore = new org.springframework.ai.document.Document(
                        vectorId, chunk.getText(), metadata);
                docsToStore.add(docToStore);
            }

            // 批量保存到数据库
            documentChunkRepository.saveAll(dbChunks).collectList().block();

            // 4. 保存到向量库
            vectorStore.add(docsToStore);

            // 5. 更新文档状态
            document.setTotalChunks(chunks.size());
            updateStatus(document, DocumentStatus.PROCESSED).subscribe();

        } catch (Exception e) {
            log.error("文档处理失败: " + document.getId(), e);
            updateStatus(document, DocumentStatus.FAILED).subscribe();
        }
    }

    // 更新文档状态
    private Mono<Document> updateStatus(Document document, DocumentStatus status) {
        document.setStatus(status.getCode());
        return documentRepository.save(document);
    }

    // 获取用户文档列表
    @Override
    public Flux<Document> listDocuments(String userId) {
        return documentRepository.findByUserId(userId);
    }

    // 获取所有文档列表
    @Override
    public Flux<Document> listAllDocuments() {
        return documentRepository.findAll();
    }

    // 获取文档预览（针对PDF）
    @Override
    public Mono<ResponseEntity<Resource>> getPreviewResource(String id) {
        return documentRepository.findById(id)
                .flatMap(document -> Mono.fromCallable(() -> {
                    Path filePath = Paths.get(document.getFilePath());
                    Resource resource = new UrlResource(filePath.toUri());

                    if (!resource.exists() || !resource.isReadable()) {
                        return ResponseEntity.notFound().<Resource>build();
                    }

                    String contentType = Files.probeContentType(filePath);
                    if (contentType == null) {
                        contentType = "application/octet-stream";
                    }

                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "inline; filename=\"" + document.getDocumentName() + "\"")
                            .body(resource);
                }).subscribeOn(Schedulers.boundedElastic()))
                .onErrorResume(e -> {
                    log.error("获取文档预览失败: " + id, e);
                    return Mono.just(ResponseEntity.internalServerError().<Resource>build());
                })
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    // 删除文档
    @Override
    @Transactional
    public Mono<Void> deleteDocument(String id) {
        return documentRepository.findById(id)
                .flatMap(document -> {
                    // 1. 获取文档所有分块
                    return documentChunkRepository.findByDocumentId(id)
                            .collectList()
                            .flatMap(chunks -> {
                                List<String> vectorIds = new ArrayList<>();
                                for (DocumentChunk chunk : chunks) {
                                    try {
                                        Map<String, Object> meta = objectMapper.readValue(chunk.getMetadata(),
                                                Map.class);
                                        if (meta.containsKey("vectorId")) {
                                            vectorIds.add((String) meta.get("vectorId"));
                                        }
                                    } catch (Exception e) {
                                        log.warn("解析文档分块元数据失败: " + chunk.getId(), e);
                                    }
                                }
                                // 2. 删除向量库中的分块
                                if (!vectorIds.isEmpty()) {
                                    Mono.fromRunnable(() -> vectorStore.delete(vectorIds))
                                            .subscribeOn(Schedulers.boundedElastic())
                                            .subscribe();
                                }
                                return Mono.empty();
                            })
                            // 3. 删除数据库中的分块
                            .then(documentChunkRepository.deleteAll(documentChunkRepository.findByDocumentId(id)))
                            // 4. 删除文件系统中的文件
                            .then(Mono.fromRunnable(() -> {
                                try {
                                    Files.deleteIfExists(Paths.get(document.getFilePath()));
                                } catch (IOException e) {
                                    log.warn("Failed to delete file: " + document.getFilePath(), e);
                                }
                            }).subscribeOn(Schedulers.boundedElastic()))
                            // 5. 删除数据库中的文档记录
                            .then(documentRepository.deleteById(id));
                });
    }

    // 获取文档预览（针对txt、doc类型）
    @Override
    public Mono<String> getPreviewContent(String id) {
        return documentRepository.findById(id)
                .flatMap(document -> Mono.fromCallable(() -> {
                    Path filePath = Paths.get(document.getFilePath());
                    if (!Files.exists(filePath)) {
                        throw new IOException("文件不存在: " + document.getFilePath());
                    }

                    String fileType = document.getFileType().toLowerCase();
                    if ("txt".equals(fileType) || "md".equals(fileType)) {
                        return Files.readString(filePath);
                    }

                    if ("doc".equals(fileType)) {
                        try {
                            return convertDocToHtml(filePath);
                        } catch (Exception e) {
                            log.error("文件转换为HTML失败: " + document.getFilePath(), e);
                        }
                    }

                    try (InputStream stream = Files.newInputStream(filePath)) {
                        AutoDetectParser parser = new AutoDetectParser();
                        BodyContentHandler handler = new BodyContentHandler(-1); 
                        Metadata metadata = new Metadata();
                        parser.parse(stream, handler, metadata, new ParseContext());
                        return handler.toString();
                    }
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    // 辅助方法：将DOC文件转换为HTML格式
    private String convertDocToHtml(Path filePath) throws Exception {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            HWPFDocument wordDocument = new HWPFDocument(inputStream);
            WordToHtmlConverter wordToHtmlConverter = new WordToHtmlConverter(
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());
            wordToHtmlConverter.setPicturesManager(new PicturesManager() {
                public String savePicture(byte[] content, PictureType pictureType, String suggestedName,
                        float widthInches, float heightInches) {
                    return "data:" + pictureType.getMime() + ";base64,"
                            + Base64.getEncoder().encodeToString(content);
                }
            });
            wordToHtmlConverter.processDocument(wordDocument);
            org.w3c.dom.Document htmlDocument = wordToHtmlConverter.getDocument();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DOMSource domSource = new DOMSource(htmlDocument);
            StreamResult streamResult = new StreamResult(out);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer serializer = tf.newTransformer();
            serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setOutputProperty(OutputKeys.METHOD, "html");
            serializer.transform(domSource, streamResult);
            out.close();
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}