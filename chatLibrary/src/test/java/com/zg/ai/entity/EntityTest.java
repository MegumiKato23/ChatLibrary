package com.zg.ai.entity;

import com.zg.ai.entity.po.Document;
import com.zg.ai.entity.po.User;
import com.zg.ai.entity.po.DocumentChunk;
import com.zg.ai.repository.DocumentChunkRepository;
import com.zg.ai.repository.DocumentRepository;
import com.zg.ai.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootTest
public class EntityTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testUserEntity() {
        String username = "testuser_" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername(username);
        user.setRole("USER");
        user.setEmail(username + "@example.com");
        user.setPasswordHash("hashedpassword");
        user.setUploadPermission(true);
        // user.setCreateAt(LocalDateTime.now()); // Let Auditing handle it
        // user.setUpdateAt(LocalDateTime.now());

        StepVerifier.create(userRepository.save(user))
                .expectNextMatches(saved -> saved.getId() != null && saved.getUsername().equals(username))
                .verifyComplete();
    }

    @Test
    public void testDocumentEntity() {
        Document doc = new Document();
        doc.setUserId("user123");
        doc.setDocumentName("test.pdf");
        doc.setOriginalFilename("test.pdf");
        doc.setFilePath("/tmp/test.pdf");
        doc.setFileType("pdf");
        doc.setFileSize(1024L);
        doc.setContentType("application/pdf");
        doc.setStatus(0); // UPLOADED
        doc.setTotalChunks(10);
        doc.setTotalPages(5);
        doc.setEmbeddingModel("ollama");
        // doc.setCreateAt(LocalDateTime.now());
        // doc.setUpdateAt(LocalDateTime.now());

        StepVerifier.create(documentRepository.save(doc))
                .expectNextMatches(saved -> saved.getId() != null &&
                        saved.getDocumentName().equals("test.pdf") &&
                        saved.getStatus() == 0 &&
                        saved.getFilePath().equals("/tmp/test.pdf"))
                .verifyComplete();
    }

    @Test
    public void testDocumentChunkSaveAll() {
        String docId = UUID.randomUUID().toString();
        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(docId);
            chunk.setChunkIndex(i);
            chunk.setContent("content " + i);
            chunk.setTokenCount(10);
            chunk.setMetadata("{}");
            chunks.add(chunk);
        }

        StepVerifier.create(documentChunkRepository.saveAll(chunks))
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    public void testUserUpdate() {
        String username = "update_user_" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername(username);
        user.setRole("USER");
        user.setEmail(username + "@example.com");
        user.setPasswordHash("hashedpassword");
        user.setUploadPermission(true);

        // 1. Save (Insert)
        userRepository.save(user).block();

        // 2. Find, Update, Save (Update)
        StepVerifier.create(
                userRepository.findByUsername(username)
                        .flatMap(found -> {
                            // Check if createAt is populated
                            if (found.getCreateAt() == null) {
                                return Mono.error(new RuntimeException("createAt is null!"));
                            }
                            found.setUploadPermission(false);
                            return userRepository.save(found);
                        }))
                .expectNextMatches(updated -> !updated.getUploadPermission())
                .verifyComplete();
    }
}
