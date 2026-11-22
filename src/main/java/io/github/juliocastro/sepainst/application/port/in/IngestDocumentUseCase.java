package io.github.juliocastro.sepainst.application.port.in;

import java.util.List;

import io.github.juliocastro.sepainst.domain.model.IngestionResult;

/**
 * Makes source documents searchable: fetch, read, chunk, embed and store them.
 * Ingesting a document again replaces its previous chunks, so the operation is idempotent.
 */
public interface IngestDocumentUseCase {

    /**
     * @param documentId identifier of a document in the source catalog
     * @throws UnknownDocumentException if the catalog has no such document
     */
    IngestionResult ingest(String documentId);

    /** Ingests every document in the source catalog. */
    List<IngestionResult> ingestAll();

    /** Thrown when a document identifier is not in the source catalog. */
    final class UnknownDocumentException extends RuntimeException {

        public UnknownDocumentException(String documentId) {
            super("Unknown document '" + documentId + "'. Check the source catalog (sepainst.sources).");
        }
    }
}
