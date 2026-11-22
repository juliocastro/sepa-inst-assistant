package io.github.juliocastro.sepainst.config;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.juliocastro.sepainst.adapter.in.cli.IngestionCommandLineRunner;
import io.github.juliocastro.sepainst.adapter.out.ai.VectorStorePassageStore;
import io.github.juliocastro.sepainst.adapter.out.document.ConfiguredSourceCatalog;
import io.github.juliocastro.sepainst.adapter.out.document.HttpDocumentFetcher;
import io.github.juliocastro.sepainst.adapter.out.document.PdfBoxDocumentReader;
import io.github.juliocastro.sepainst.application.port.in.IngestDocumentUseCase;
import io.github.juliocastro.sepainst.application.port.out.DocumentFetcher;
import io.github.juliocastro.sepainst.application.port.out.DocumentReader;
import io.github.juliocastro.sepainst.application.port.out.PassageStore;
import io.github.juliocastro.sepainst.application.port.out.SourceCatalog;
import io.github.juliocastro.sepainst.application.service.IngestDocumentService;
import io.github.juliocastro.sepainst.domain.chunking.SectionAwareChunker;
import io.github.juliocastro.sepainst.domain.model.SourceDocument;

/**
 * Wires use cases to adapters. Services and adapters are plain classes instantiated here, so
 * the domain and application layers need no Spring annotations.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SepaInstProperties.class)
class ApplicationConfiguration {

    // --- outbound adapters -------------------------------------------------------------

    @Bean
    SourceCatalog sourceCatalog(SepaInstProperties properties) {
        return new ConfiguredSourceCatalog(properties.sources().documents().stream()
                .map(document -> new SourceDocument(document.id(), document.title(), document.url()))
                .toList());
    }

    @Bean
    DocumentFetcher documentFetcher(SepaInstProperties properties) {
        return new HttpDocumentFetcher(properties.sources().directory(), properties.ingestion().downloadTimeout());
    }

    @Bean
    DocumentReader documentReader() {
        return new PdfBoxDocumentReader();
    }

    @Bean
    PassageStore passageStore(VectorStore vectorStore, SepaInstProperties properties) {
        return new VectorStorePassageStore(vectorStore, properties.ingestion().batchSize());
    }

    // --- domain and use cases ------------------------------------------------------------

    @Bean
    SectionAwareChunker sectionAwareChunker(SepaInstProperties properties) {
        return new SectionAwareChunker(properties.ingestion().maxChunkChars());
    }

    @Bean
    IngestDocumentUseCase ingestDocumentUseCase(SourceCatalog catalog, DocumentFetcher fetcher,
                                                DocumentReader reader, SectionAwareChunker chunker,
                                                PassageStore store) {
        return new IngestDocumentService(catalog, fetcher, reader, chunker, store);
    }

    // --- inbound adapters ----------------------------------------------------------------

    @Bean
    IngestionCommandLineRunner ingestionCommandLineRunner(IngestDocumentUseCase ingestion) {
        return new IngestionCommandLineRunner(ingestion);
    }
}
