package io.github.juliocastro.sepainst.application.service;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import io.github.juliocastro.sepainst.application.port.in.IngestDocumentUseCase;
import io.github.juliocastro.sepainst.application.port.out.DocumentFetcher;
import io.github.juliocastro.sepainst.application.port.out.DocumentReader;
import io.github.juliocastro.sepainst.application.port.out.PassageStore;
import io.github.juliocastro.sepainst.application.port.out.SourceCatalog;
import io.github.juliocastro.sepainst.domain.chunking.SectionAwareChunker;
import io.github.juliocastro.sepainst.domain.model.DocumentChunk;
import io.github.juliocastro.sepainst.domain.model.DocumentPage;
import io.github.juliocastro.sepainst.domain.model.IngestionResult;
import io.github.juliocastro.sepainst.domain.model.SourceDocument;

/** Ingestion pipeline: catalog → fetch → read pages → chunk by section → store. */
public class IngestDocumentService implements IngestDocumentUseCase {

    private final SourceCatalog catalog;
    private final DocumentFetcher fetcher;
    private final DocumentReader reader;
    private final SectionAwareChunker chunker;
    private final PassageStore store;

    public IngestDocumentService(SourceCatalog catalog, DocumentFetcher fetcher, DocumentReader reader,
                                 SectionAwareChunker chunker, PassageStore store) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.fetcher = Objects.requireNonNull(fetcher, "fetcher must not be null");
        this.reader = Objects.requireNonNull(reader, "reader must not be null");
        this.chunker = Objects.requireNonNull(chunker, "chunker must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
    }

    @Override
    public IngestionResult ingest(String documentId) {
        SourceDocument document = catalog.find(documentId)
                .orElseThrow(() -> new UnknownDocumentException(documentId));
        return ingest(document);
    }

    @Override
    public List<IngestionResult> ingestAll() {
        return catalog.all().stream().map(this::ingest).toList();
    }

    private IngestionResult ingest(SourceDocument document) {
        Path file = fetcher.fetch(document);
        List<DocumentPage> pages = reader.read(file);
        List<DocumentChunk> chunks = chunker.chunk(document, pages);
        if (chunks.isEmpty()) {
            throw new IllegalStateException(
                    "No text could be extracted from '" + document.id() + "' (" + file + ")");
        }
        store.replace(document.id(), chunks);
        return new IngestionResult(document.id(), pages.size(), chunks.size());
    }
}
