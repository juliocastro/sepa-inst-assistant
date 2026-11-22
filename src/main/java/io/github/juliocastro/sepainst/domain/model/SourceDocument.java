package io.github.juliocastro.sepainst.domain.model;

import java.net.URI;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A public document the assistant can learn from, e.g. an EPC implementation guideline.
 *
 * @param id    short stable identifier, used in citations and as the local file name
 *              (e.g. {@code EPC122-16-2025})
 * @param title human-readable title, used in citations
 * @param url   official download location
 */
public record SourceDocument(String id, String title, URI url) {

    private static final Pattern ID_FORMAT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

    public SourceDocument {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(url, "url must not be null");
        if (!ID_FORMAT.matcher(id).matches()) {
            throw new IllegalArgumentException(
                    "id must be 1-64 characters: letters, digits, '.', '_' or '-' (got '" + id + "')");
        }
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
    }

    /** Local file name for the downloaded document. */
    public String fileName() {
        return id + ".pdf";
    }
}
