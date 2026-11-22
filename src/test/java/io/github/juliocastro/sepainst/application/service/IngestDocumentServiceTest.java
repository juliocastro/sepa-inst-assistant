package io.github.juliocastro.sepainst.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.juliocastro.sepainst.application.port.in.IngestDocumentUseCase.UnknownDocumentException;
import io.github.juliocastro.sepainst.application.port.out.DocumentFetcher;
import io.github.juliocastro.sepainst.application.port.out.DocumentReader;
import io.github.juliocastro.sepainst.application.port.out.PassageStore;
import io.github.juliocastro.sepainst.application.port.out.SourceCatalog;
import io.github.juliocastro.sepainst.domain.chunking.SectionAwareChunker;
import io.github.juliocastro.sepainst.domain.model.DocumentChunk;
import io.github.juliocastro.sepainst.domain.model.DocumentPage;
import io.github.juliocastro.sepainst.domain.model.IngestionResult;
import io.github.juliocastro.sepainst.domain.model.SourceDocument;

/** Pure unit tests: ports are replaced by in-memory fakes. */
class IngestDocumentServiceTest {

    private static final SourceDocument IG = new SourceDocument("IG", "Inter-PSP IG", URI.create("https://example.org/ig.pdf"));
    private static final SourceDocument RULEBOOK = new SourceDocument("RB", "Rulebook", URI.create("https://example.org/rb.pdf"));

    private final SourceCatalog catalog = () -> List.of(IG, RULEBOOK);
    private final DocumentFetcher fetcher = document -> Path.of("sources", document.fileName());
    private final DocumentReader reader = file -> List.of(
            new DocumentPage(1, "1 Introduction\nText of " + file.getFileName()),
            new DocumentPage(2, "2 Messages\nMore text"));
    private final Map<String, List<DocumentChunk>> stored = new LinkedHashMap<>();
    private final PassageStore store = (documentId, chunks) -> stored.put(documentId, chunks);

    private final IngestDocumentService service =
            new IngestDocumentService(catalog, fetcher, reader, new SectionAwareChunker(1_000), store);

    @Test
    void fetchesReadsChunksAndStoresTheDocument() {
        IngestionResult result = service.ingest("IG");

        assertThat(result).isEqualTo(new IngestionResult("IG", 2, 2));
        assertThat(stored).containsOnlyKeys("IG");
        assertThat(stored.get("IG").get(0).content()).contains("Text of IG.pdf");
        assertThat(stored.get("IG").get(1).source().section()).isEqualTo("2 Messages");
    }

    @Test
    void ingestsEveryDocumentOfTheCatalog() {
        List<IngestionResult> results = service.ingestAll();

        assertThat(results).extracting(IngestionResult::documentId).containsExactly("IG", "RB");
        assertThat(stored).containsOnlyKeys("IG", "RB");
    }

    @Test
    void rejectsDocumentsOutsideTheCatalog() {
        assertThatThrownBy(() -> service.ingest("UNKNOWN"))
                .isInstanceOf(UnknownDocumentException.class)
                .hasMessageContaining("UNKNOWN");
        assertThat(stored).isEmpty();
    }

    @Test
    void failsInsteadOfStoringNothingWhenNoTextIsExtracted() {
        List<String> storeCalls = new ArrayList<>();
        IngestDocumentService emptyReaderService = new IngestDocumentService(
                catalog, fetcher, file -> List.of(new DocumentPage(1, "")), new SectionAwareChunker(1_000),
                (documentId, chunks) -> storeCalls.add(documentId));

        assertThatThrownBy(() -> emptyReaderService.ingest("IG"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No text");
        assertThat(storeCalls).isEmpty();
    }
}
