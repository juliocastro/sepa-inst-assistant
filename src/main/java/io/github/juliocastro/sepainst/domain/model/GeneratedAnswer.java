package io.github.juliocastro.sepainst.domain.model;

import java.util.Objects;
import java.util.Set;

/**
 * An answer written by the model, with the passages it actually cited.
 *
 * @param text      the answer text
 * @param citations 1-based positions of the cited passages in the list the model received;
 *                  empty when the model cited nothing (typically: the passages did not answer)
 */
public record GeneratedAnswer(String text, Set<Integer> citations) {

    public GeneratedAnswer {
        Objects.requireNonNull(text, "text must not be null");
        citations = Set.copyOf(Objects.requireNonNull(citations, "citations must not be null"));
    }
}
