package com.zg.ai;

import com.zg.ai.utils.VectorDistanceUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class VectorSearchTests {

    @Autowired
    private OllamaEmbeddingModel embeddingModel;

    @Test
    public void testVectorDistance() {
        // 1. Prepare sample texts
        String query = "Artificial Intelligence";
        String relevantText = "Machine Learning and AI";
        String irrelevantText = "Cooking Pasta Recipes";

        // 2. Generate embeddings
        float[] queryVector = embeddingModel.embed(query);
        float[] relevantVector = embeddingModel.embed(relevantText);
        float[] irrelevantVector = embeddingModel.embed(irrelevantText);

        // 3. Calculate distances using the custom utility
        double distanceToRelevant = VectorDistanceUtils.cosineDistance(queryVector, relevantVector);
        double distanceToIrrelevant = VectorDistanceUtils.cosineDistance(queryVector, irrelevantVector);

        System.out.println("Query: " + query);
        System.out.println("Distance to '" + relevantText + "': " + distanceToRelevant);
        System.out.println("Distance to '" + irrelevantText + "': " + distanceToIrrelevant);

        // 4. Verify logic (Cosine Similarity: Higher is better, usually 0-1)
        // Wait, VectorDistanceUtils.cosineDistance implementation usually returns *Similarity* (0 to 1) or *Distance* (1 - Similarity)?
        // Let's check the implementation provided earlier.
        // It calculated dotProduct / (normA * normB). This is Cosine Similarity.
        // So relevant text should have HIGHER similarity than irrelevant text.
        
        Assertions.assertTrue(distanceToRelevant > distanceToIrrelevant, 
            "Relevant text should have higher cosine similarity than irrelevant text");
            
        // 5. Verify Euclidean Distance (Lower is better)
        double euclideanToRelevant = VectorDistanceUtils.euclideanDistance(queryVector, relevantVector);
        double euclideanToIrrelevant = VectorDistanceUtils.euclideanDistance(queryVector, irrelevantVector);
        
        System.out.println("Euclidean to '" + relevantText + "': " + euclideanToRelevant);
        System.out.println("Euclidean to '" + irrelevantText + "': " + euclideanToIrrelevant);
        
        Assertions.assertTrue(euclideanToRelevant < euclideanToIrrelevant, 
            "Relevant text should have lower euclidean distance than irrelevant text");
    }
}
