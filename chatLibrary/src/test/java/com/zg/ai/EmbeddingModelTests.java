package com.zg.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@SpringBootTest
public class EmbeddingModelTests {
    @Autowired
    private OllamaEmbeddingModel embeddingModel;

    @Test
    void contextLoads() {
        float[] floats = embeddingModel.embed("广东工业大学");
        System.out.println(Arrays.toString(floats));
    }
}
