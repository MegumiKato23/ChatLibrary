I will enhance the system to explicitly handle large documents by implementing **Document Chunking** and persisting these chunks to the database.

1. **Database Schema Expansion (schema.sql)**:

   * **New Table**: Create `document_chunk` table to store the actual text segments of split documents.

     * Columns: `id`, `document_id` (FK), `chunk_index`, `content` (LONGTEXT to hold the chunk text), `token_count`.

   * **Optimizations**: Apply `utf8mb4` and indexes as previously discussed.

2. **Entity & Persistence Layer**:

   * Create `DocumentChunk` entity and `DocumentChunkMapper` (MyBatis-Plus) to manage chunk data.

3. **Enhanced Document Processing (DocumentServiceImpl)**:

   * **Splitting Logic**: Use `TokenTextSplitter` to break large files into manageable chunks (handling the "document too large" issue).

   * **Dual Storage**:

     * **Vector Store**: Save chunk embeddings to Qdrant for semantic search.

     * **MySQL**: Save raw chunk text to the `document_chunk` table for reference and backup.

4. **Chat & Verification**:

   * Update `ChatService` to perform RAG using the vector store.

   * Add `VectorSearchTests` using `VectorDistanceUtils` to verify embedding quality.

