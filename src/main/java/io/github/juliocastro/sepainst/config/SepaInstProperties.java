package io.github.juliocastro.sepainst.config;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Application settings under the {@code sepainst} prefix. */
@ConfigurationProperties("sepainst")
public record SepaInstProperties(@DefaultValue Sources sources, @DefaultValue Ingestion ingestion) {

    /**
     * @param directory local folder for downloaded documents (git-ignored)
     * @param documents the public documents the assistant learns from
     */
    public record Sources(@DefaultValue("sources") Path directory, @DefaultValue List<Document> documents) {
    }

    public record Document(String id, String title, URI url) {
    }

    /**
     * @param maxChunkChars   maximum size of a chunk, in characters
     * @param batchSize       chunks embedded and stored per batch
     * @param downloadTimeout timeout for downloading a source document
     */
    public record Ingestion(@DefaultValue("2000") int maxChunkChars,
                            @DefaultValue("50") int batchSize,
                            @DefaultValue("60s") Duration downloadTimeout) {
    }
}
