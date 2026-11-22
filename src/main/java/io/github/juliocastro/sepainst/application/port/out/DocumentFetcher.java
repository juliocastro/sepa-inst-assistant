package io.github.juliocastro.sepainst.application.port.out;

import java.nio.file.Path;

import io.github.juliocastro.sepainst.domain.model.SourceDocument;

/** Makes a source document available locally, downloading it only if needed. */
public interface DocumentFetcher {

    /** @return path of the local copy of the document */
    Path fetch(SourceDocument document);
}
