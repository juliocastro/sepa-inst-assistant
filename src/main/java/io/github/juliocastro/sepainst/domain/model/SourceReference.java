package io.github.juliocastro.sepainst.domain.model;

import java.util.Objects;

/**
 * Where a piece of information comes from: the public document, the section and the page.
 *
 * @param document document identifier, e.g. {@code EPC122-16 SCT Inst Inter-PSP IG 2025 V1.0}
 * @param section  section number or title inside the document, may be {@code null} if unknown
 * @param page     page number in the document, may be {@code null} if unknown
 */
public record SourceReference(String document, String section, Integer page) {

    public SourceReference {
        Objects.requireNonNull(document, "document must not be null");
        if (document.isBlank()) {
            throw new IllegalArgumentException("document must not be blank");
        }
        if (page != null && page < 1) {
            throw new IllegalArgumentException("page must be positive");
        }
    }
}
