package io.github.juliocastro.sepainst.domain.chunking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.juliocastro.sepainst.domain.model.DocumentChunk;
import io.github.juliocastro.sepainst.domain.model.DocumentPage;
import io.github.juliocastro.sepainst.domain.model.SourceDocument;

class SectionAwareChunkerTest {

    private static final SourceDocument DOC =
            new SourceDocument("EPC-TEST", "EPC test guideline", URI.create("https://example.org/test.pdf"));
    private static final String FOOTER = "SEPA Instant Credit Transfer Inter-PSP IGs 2025 Version 1.0 Approved Page %d";

    private final SectionAwareChunker chunker = new SectionAwareChunker(500);

    @Test
    void startsANewChunkAtEveryNumberedHeadingAndRecordsSectionAndPage() {
        List<DocumentChunk> chunks = chunker.chunk(DOC, List.of(
                new DocumentPage(1, """
                        1 Introduction
                        This document sets out the rules.
                        2.1 Scope
                        The scope covers pacs.008 messages."""),
                new DocumentPage(2, """
                        Still part of the scope section.
                        2.2 Reject reasons
                        A negative pacs.002 is sent when the payment is rejected.""")));

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0).source().section()).isEqualTo("1 Introduction");
        assertThat(chunks.get(1).source().section()).isEqualTo("2.1 Scope");
        assertThat(chunks.get(1).source().page()).isEqualTo(1);
        assertThat(chunks.get(1).content()).contains("pacs.008", "Still part of the scope section.");
        assertThat(chunks.get(2).source().section()).isEqualTo("2.2 Reject reasons");
        assertThat(chunks.get(2).source().page()).isEqualTo(2);
        assertThat(chunks).extracting(DocumentChunk::index).containsExactly(0, 1, 2);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.source().document()).isEqualTo("EPC test guideline"));
    }

    @Test
    void removesRunningHeadersAndFootersRepeatedOnMostPages() {
        List<DocumentChunk> chunks = chunker.chunk(DOC, List.of(
                new DocumentPage(1, "1 Introduction\nFirst page text.\n" + FOOTER.formatted(1)),
                new DocumentPage(2, "Second page text.\n" + FOOTER.formatted(2)),
                new DocumentPage(3, "Third page text.\n" + FOOTER.formatted(3))));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).content()).doesNotContain("Approved Page");
        assertThat(chunks.get(0).content()).contains("First page text.", "Third page text.");
    }

    @Test
    void ignoresTableOfContentsLines() {
        List<DocumentChunk> chunks = chunker.chunk(DOC, List.of(new DocumentPage(1, """
                2.1 Scope ............................ 7
                2.2 Reject reasons ................... 9
                2.1 Scope
                Real content.""")));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).content()).isEqualTo("2.1 Scope\nReal content.");
    }

    @Test
    void splitsLongSectionsAtLineBoundariesWithinTheLimit() {
        String line = "x".repeat(150);
        String text = "3 Long section\n" + String.join("\n", java.util.Collections.nCopies(10, line));

        List<DocumentChunk> chunks = chunker.chunk(DOC, List.of(new DocumentPage(1, text)));

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.content().length()).isLessThanOrEqualTo(500);
            assertThat(chunk.source().section()).isEqualTo("3 Long section");
        });
    }

    @Test
    void cutsASingleLineLongerThanAChunk() {
        List<DocumentChunk> chunks = chunker.chunk(DOC, List.of(new DocumentPage(1, "y".repeat(1_200))));

        assertThat(chunks).hasSize(3);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.content().length()).isLessThanOrEqualTo(500));
    }

    @Test
    void textBeforeTheFirstHeadingHasNoSection() {
        List<DocumentChunk> chunks = chunker.chunk(DOC, List.of(new DocumentPage(1, "Cover page text")));

        assertThat(chunks).singleElement().satisfies(chunk -> assertThat(chunk.source().section()).isNull());
    }

    @Test
    void returnsNoChunksForEmptyPages() {
        assertThat(chunker.chunk(DOC, List.of(new DocumentPage(1, "  \n \n")))).isEmpty();
    }

    @Test
    void rejectsTooSmallChunkSize() {
        assertThatThrownBy(() -> new SectionAwareChunker(100)).isInstanceOf(IllegalArgumentException.class);
    }
}
