package io.github.juliocastro.sepainst.application.port.out;

import java.util.List;

import io.github.juliocastro.sepainst.domain.model.DocumentChunk;

/** Stores document chunks so they can later be retrieved as passages (embedding them on the way). */
public interface PassageStore {

    /** Replaces every chunk previously stored for the document with the given ones. */
    void replace(String documentId, List<DocumentChunk> chunks);
}
