package io.github.juliocastro.sepainst.domain.model;

import java.util.Objects;

/**
 * A fragment of a source document, sized to be embedded and retrieved on its own.
 *
 * @param index   position of the chunk inside its document, from 0
 * @param content the text of the chunk
 * @param source  document, section and page where the chunk starts
 */
public record DocumentChunk(int index, String content, SourceReference source) {

    public DocumentChunk {
        if (index < 0) {
            throw new IllegalArgumentException("index must not be negative");
        }
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(source, "source must not be null");
        if (content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }
}
