package io.github.juliocastro.sepainst.domain.model;

import java.util.Objects;

/** The extracted text of one page of a source document. Pages are numbered from 1. */
public record DocumentPage(int number, String text) {

    public DocumentPage {
        if (number < 1) {
            throw new IllegalArgumentException("page number must be positive");
        }
        Objects.requireNonNull(text, "text must not be null");
    }
}
