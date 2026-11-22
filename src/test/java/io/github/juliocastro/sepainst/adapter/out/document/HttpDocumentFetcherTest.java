package io.github.juliocastro.sepainst.adapter.out.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpServer;

import io.github.juliocastro.sepainst.domain.model.SourceDocument;

/** Uses the JDK's built-in HTTP server, so no network access is needed. */
class HttpDocumentFetcherTest {

    private static final byte[] PDF = "%PDF-1.7 test content".getBytes(StandardCharsets.US_ASCII);

    @TempDir
    Path directory;

    private HttpServer server;
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/doc.pdf", exchange -> respond(exchange, 200, PDF));
        server.createContext("/missing.pdf", exchange -> respond(exchange, 404, new byte[0]));
        server.createContext("/page.html", exchange -> respond(exchange, 200,
                "<html>not a pdf</html>".getBytes(StandardCharsets.UTF_8)));
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void downloadsTheDocumentOnceAndReusesTheLocalCopy() throws IOException {
        HttpDocumentFetcher fetcher = new HttpDocumentFetcher(directory, Duration.ofSeconds(5));
        SourceDocument document = document("DOC", "/doc.pdf");

        Path first = fetcher.fetch(document);
        Path second = fetcher.fetch(document);

        assertThat(first).isEqualTo(directory.resolve("DOC.pdf")).isEqualTo(second);
        assertThat(Files.readAllBytes(first)).isEqualTo(PDF);
        assertThat(requests).hasValue(1);
    }

    @Test
    void failsWithManualDownloadInstructionsOnHttpErrors() {
        HttpDocumentFetcher fetcher = new HttpDocumentFetcher(directory, Duration.ofSeconds(5));

        assertThatThrownBy(() -> fetcher.fetch(document("MISSING", "/missing.pdf")))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("HTTP 404")
                .hasMessageContaining("Download it manually");
        assertThat(directory.resolve("MISSING.pdf")).doesNotExist();
    }

    @Test
    void rejectsResponsesThatAreNotPdfsAndLeavesNoPartialFile() throws IOException {
        HttpDocumentFetcher fetcher = new HttpDocumentFetcher(directory, Duration.ofSeconds(5));

        assertThatThrownBy(() -> fetcher.fetch(document("HTML", "/page.html")))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("not a PDF");
        try (var files = Files.list(directory)) {
            assertThat(files).isEmpty();
        }
    }

    private SourceDocument document(String id, String path) {
        URI url = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
        return new SourceDocument(id, id + " title", url);
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, int status, byte[] body) throws IOException {
        requests.incrementAndGet();
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        try (var out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
