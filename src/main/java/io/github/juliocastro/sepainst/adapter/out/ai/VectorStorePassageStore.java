package io.github.juliocastro.sepainst.adapter.out.ai;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import io.github.juliocastro.sepainst.application.port.out.PassageStore;
import io.github.juliocastro.sepainst.domain.model.DocumentChunk;

/**
 * Stores chunks in a Spring AI {@link VectorStore} (pgvector). The store computes the embeddings.
 * Chunk ids are derived from document id + chunk index, so re-ingesting a document is idempotent.
 */
public class VectorStorePassageStore implements PassageStore {

    static final String DOCUMENT_ID = "document_id";
    static final String DOCUMENT_TITLE = "document_title";
    static final String SECTION = "section";
    static final String PAGE = "page";
    static final String CHUNK_INDEX = "chunk_index";

    private static final Logger log = LoggerFactory.getLogger(VectorStorePassageStore.class);

    private final VectorStore vectorStore;
    private final int batchSize;

    public VectorStorePassageStore(VectorStore vectorStore, int batchSize) {
        this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore must not be null");
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be at least 1");
        }
        this.batchSize = batchSize;
    }

    @Override
    public void replace(String documentId, List<DocumentChunk> chunks) {
        vectorStore.delete(new FilterExpressionBuilder().eq(DOCUMENT_ID, documentId).build());

        List<Document> documents = chunks.stream().map(chunk -> toDocument(documentId, chunk)).toList();
        for (int from = 0; from < documents.size(); from += batchSize) {
            int to = Math.min(documents.size(), from + batchSize);
            vectorStore.add(documents.subList(from, to));
            log.info("Embedded and stored {}/{} chunks of {}", to, documents.size(), documentId);
        }
    }

    static String chunkId(String documentId, int index) {
        return UUID.nameUUIDFromBytes((documentId + "#" + index).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static Document toDocument(String documentId, DocumentChunk chunk) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(DOCUMENT_ID, documentId);
        metadata.put(DOCUMENT_TITLE, chunk.source().document());
        metadata.put(CHUNK_INDEX, chunk.index());
        if (chunk.source().section() != null) {
            metadata.put(SECTION, chunk.source().section());
        }
        if (chunk.source().page() != null) {
            metadata.put(PAGE, chunk.source().page());
        }
        return new Document(chunkId(documentId, chunk.index()), chunk.content(), metadata);
    }
}
