package io.github.juliocastro.sepainst.adapter.in.cli;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import io.github.juliocastro.sepainst.application.port.in.IngestDocumentUseCase;
import io.github.juliocastro.sepainst.domain.model.IngestionResult;

/**
 * Command-line entry point for ingestion: {@code --ingest=all} or {@code --ingest=<document id>}
 * (repeatable). Without the option, the application starts normally and nothing is ingested.
 */
public class IngestionCommandLineRunner implements ApplicationRunner {

    static final String OPTION = "ingest";
    static final String ALL = "all";

    private static final Logger log = LoggerFactory.getLogger(IngestionCommandLineRunner.class);

    private final IngestDocumentUseCase ingestion;

    public IngestionCommandLineRunner(IngestDocumentUseCase ingestion) {
        this.ingestion = Objects.requireNonNull(ingestion, "ingestion must not be null");
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption(OPTION)) {
            return;
        }
        List<String> values = args.getOptionValues(OPTION);
        List<IngestionResult> results = values.isEmpty() || values.contains(ALL)
                ? ingestion.ingestAll()
                : values.stream().map(ingestion::ingest).toList();

        results.forEach(result -> log.info("Ingested {}: {} pages, {} chunks",
                result.documentId(), result.pages(), result.chunks()));
    }
}
