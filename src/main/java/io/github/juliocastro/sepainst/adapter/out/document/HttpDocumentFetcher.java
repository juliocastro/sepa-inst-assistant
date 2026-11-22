package io.github.juliocastro.sepainst.adapter.out.document;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.juliocastro.sepainst.application.port.out.DocumentFetcher;
import io.github.juliocastro.sepainst.domain.model.SourceDocument;

/**
 * Downloads source documents over HTTPS into a local directory. A document already present
 * is reused, so the official site is only contacted once. Downloads go to a temporary file
 * that is moved into place only when complete and verified to be a PDF.
 */
public class HttpDocumentFetcher implements DocumentFetcher {

    private static final Logger log = LoggerFactory.getLogger(HttpDocumentFetcher.class);
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F'};

    private final Path directory;
    private final HttpClient client;
    private final Duration timeout;

    public HttpDocumentFetcher(Path directory, Duration timeout) {
        this(directory, timeout, HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(timeout)
                .build());
    }

    HttpDocumentFetcher(Path directory, Duration timeout, HttpClient client) {
        this.directory = Objects.requireNonNull(directory, "directory must not be null");
        this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    @Override
    public Path fetch(SourceDocument document) {
        Path target = directory.resolve(document.fileName());
        try {
            if (Files.isRegularFile(target) && Files.size(target) > 0) {
                log.info("Using local copy of {}: {}", document.id(), target);
                return target;
            }
            Files.createDirectories(directory);
            log.info("Downloading {} from {}", document.id(), document.url());
            Path partial = Files.createTempFile(directory, document.id(), ".part");
            try {
                download(document, partial);
                Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(partial);
            }
            log.info("Saved {} ({} bytes)", target, Files.size(target));
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException(failureMessage(document, target, e.getMessage()), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(failureMessage(document, target, "interrupted"), e);
        }
    }

    private void download(SourceDocument document, Path destination) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(document.url())
                .timeout(timeout)
                .header("User-Agent", "sepa-inst-assistant (educational project)")
                .GET()
                .build();
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(destination));
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode());
        }
        if (!startsWithPdfMagic(destination)) {
            throw new IOException("the response is not a PDF");
        }
    }

    private static boolean startsWithPdfMagic(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] head = in.readNBytes(PDF_MAGIC.length);
            return Arrays.equals(head, PDF_MAGIC);
        }
    }

    private static String failureMessage(SourceDocument document, Path target, String reason) {
        return "Could not download " + document.id() + " from " + document.url() + " (" + reason + "). "
                + "Download it manually and save it as " + target.toAbsolutePath();
    }
}
