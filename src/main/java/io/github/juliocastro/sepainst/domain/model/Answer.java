package io.github.juliocastro.sepainst.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * The assistant's answer to a question.
 *
 * @param text     the answer text
 * @param sources  the documents the answer is based on; empty when the answer is not grounded
 * @param grounded {@code true} when the answer is based on retrieved passages; {@code false}
 *                 when no relevant passage was found and the assistant declined to answer
 */
public record Answer(String text, List<SourceReference> sources, boolean grounded) {

    static final String NOT_FOUND_TEXT =
            "No relevant information was found in the indexed documents to answer this question.";

    public Answer {
        Objects.requireNonNull(text, "text must not be null");
        sources = List.copyOf(Objects.requireNonNull(sources, "sources must not be null"));
        if (grounded && sources.isEmpty()) {
            throw new IllegalArgumentException("a grounded answer must cite at least one source");
        }
    }

    public static Answer grounded(String text, List<SourceReference> sources) {
        return new Answer(text, sources, true);
    }

    /** Answer returned instead of guessing when the documents do not cover the question. */
    public static Answer notFound() {
        return new Answer(NOT_FOUND_TEXT, List.of(), false);
    }
}
