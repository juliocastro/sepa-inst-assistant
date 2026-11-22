package io.github.juliocastro.sepainst.domain.model;

import java.util.Objects;

/** Outcome of ingesting one source document. */
public record IngestionResult(String documentId, int pages, int chunks) {

    public IngestionResult {
        Objects.requireNonNull(documentId, "documentId must not be null");
        if (pages < 0 || chunks < 0) {
            throw new IllegalArgumentException("pages and chunks must not be negative");
        }
    }
}
