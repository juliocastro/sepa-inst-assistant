package io.github.juliocastro.sepainst.domain.model;

import java.util.Objects;

/** A question asked by a user about the SEPA Instant scheme or ISO 20022 messages. */
public record Question(String text) {

    public static final int MAX_LENGTH = 2_000;

    public Question {
        Objects.requireNonNull(text, "text must not be null");
        text = text.strip();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        if (text.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("text must be at most " + MAX_LENGTH + " characters");
        }
    }
}
