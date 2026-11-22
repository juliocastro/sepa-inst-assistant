package io.github.juliocastro.sepainst.application.port.out;

import java.util.List;
import java.util.Optional;

import io.github.juliocastro.sepainst.domain.model.SourceDocument;

/** The list of public documents the assistant is allowed to learn from. */
public interface SourceCatalog {

    List<SourceDocument> all();

    default Optional<SourceDocument> find(String documentId) {
        return all().stream().filter(document -> document.id().equals(documentId)).findFirst();
    }
}
