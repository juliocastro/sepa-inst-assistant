package io.github.juliocastro.sepainst.domain.model;

import java.util.Objects;

/**
 * A fragment of a source document retrieved as relevant to a question.
 *
 * @param content the text of the fragment
 * @param source  where the fragment comes from
 * @param score   relevance to the question, from 0 (unrelated) to 1 (identical meaning)
 */
public record Passage(String content, SourceReference source, double score) {

    public Passage {
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(source, "source must not be null");
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("score must be between 0 and 1");
        }
    }
}
