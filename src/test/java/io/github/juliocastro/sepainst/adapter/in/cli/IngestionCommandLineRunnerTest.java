package io.github.juliocastro.sepainst.adapter.in.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import io.github.juliocastro.sepainst.application.port.in.IngestDocumentUseCase;
import io.github.juliocastro.sepainst.domain.model.IngestionResult;

class IngestionCommandLineRunnerTest {

    private final List<String> calls = new ArrayList<>();

    private final IngestDocumentUseCase ingestion = new IngestDocumentUseCase() {
        @Override
        public IngestionResult ingest(String documentId) {
            calls.add(documentId);
            return new IngestionResult(documentId, 1, 1);
        }

        @Override
        public List<IngestionResult> ingestAll() {
            calls.add("*");
            return List.of();
        }
    };

    private final IngestionCommandLineRunner runner = new IngestionCommandLineRunner(ingestion);

    @Test
    void doesNothingWithoutTheIngestOption() {
        runner.run(new DefaultApplicationArguments());

        assertThat(calls).isEmpty();
    }

    @Test
    void ingestsTheRequestedDocuments() {
        runner.run(new DefaultApplicationArguments("--ingest=IG", "--ingest=RB"));

        assertThat(calls).containsExactly("IG", "RB");
    }

    @Test
    void ingestsEverythingWithAll() {
        runner.run(new DefaultApplicationArguments("--ingest=all"));

        assertThat(calls).containsExactly("*");
    }
}
