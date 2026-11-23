package io.github.juliocastro.sepainst.adapter.out.ai;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import io.github.juliocastro.sepainst.application.port.out.PassageRetriever;
import io.github.juliocastro.sepainst.domain.model.Passage;
import io.github.juliocastro.sepainst.domain.model.Question;
import io.github.juliocastro.sepainst.domain.model.SourceReference;

/** Semantic search on the pgvector store: the question is embedded and compared with every chunk. */
public class VectorStorePassageRetriever implements PassageRetriever {

    private final VectorStore vectorStore;

    public VectorStorePassageRetriever(VectorStore vectorStore) {
        this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore must not be null");
    }

    @Override
    public List<Passage> findRelevant(Question question, int maxResults) {
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(question.text())
                .topK(maxResults)
                .build());
        if (documents == null) {
            return List.of();
        }
        return documents.stream().map(VectorStorePassageRetriever::toPassage).toList();
    }

    private static Passage toPassage(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        SourceReference source = new SourceReference(
                String.valueOf(metadata.getOrDefault(VectorStorePassageStore.DOCUMENT_TITLE,
                        metadata.getOrDefault(VectorStorePassageStore.DOCUMENT_ID, "unknown document"))),
                asString(metadata.get(VectorStorePassageStore.SECTION)),
                asInteger(metadata.get(VectorStorePassageStore.PAGE)));
        return new Passage(Objects.requireNonNullElse(document.getText(), ""), source, clamp(document.getScore()));
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    /** Metadata comes back from JSON, so a number may arrive as Integer, Long, Double or String. */
    private static Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return (int) Double.parseDouble(text);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /** Similarity scores can fall slightly outside [0, 1] (cosine distance ranges up to 2). */
    private static double clamp(Double score) {
        if (score == null || score.isNaN()) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, score));
    }
}
