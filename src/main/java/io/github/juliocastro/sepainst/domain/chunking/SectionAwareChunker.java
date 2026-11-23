package io.github.juliocastro.sepainst.domain.chunking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.juliocastro.sepainst.domain.model.DocumentChunk;
import io.github.juliocastro.sepainst.domain.model.DocumentPage;
import io.github.juliocastro.sepainst.domain.model.SourceDocument;
import io.github.juliocastro.sepainst.domain.model.SourceReference;

/**
 * Splits a document into chunks that follow its numbered sections ("2.1.3 Title").
 *
 * <p>Rules, in order:
 * <ol>
 *   <li>Lines repeated on most pages (running headers and footers such as
 *       "... Version 1.0 Approved Page 12") are removed.</li>
 *   <li>Table-of-contents lines ("2.1 Scope ........ 7") are removed, and so are whole pages made
 *       mostly of numbered headings (a table of contents without dot leaders).</li>
 *   <li>A numbered heading always starts a new chunk, so a chunk never mixes two sections.
 *       A heading with no text of its own (e.g. "2 Messages" directly followed by "2.1 ...")
 *       is kept as context at the top of the next section's chunk.</li>
 *   <li>A section longer than {@code maxChars} is split at line boundaries.</li>
 * </ol>
 * Each chunk records the section and the page where it starts, so answers can cite them.
 */
public class SectionAwareChunker {

    /**
     * "2", "2.1", "2.1.3" or "2.1.3.4" followed by a title that starts with a capital letter.
     * A dot right after the number ("14. Note updated...") marks a numbered list item, not a heading.
     */
    private static final Pattern HEADING =
            Pattern.compile("^(\\d{1,2}(?:\\.\\d{1,3}){0,4})\\s+(\\p{Lu}[^.]{2,120})$");
    private static final Pattern TOC_LINE = Pattern.compile(".*\\.{5,}\\s*\\d+\\s*$");
    /** A numbered title of any case, as listed in a table of contents (optionally with a page number). */
    private static final Pattern NUMBERED_TITLE = Pattern.compile("^\\d{1,2}(?:\\.\\d{1,3}){0,4}\\.?\\s+\\p{L}.*");
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    private static final double REPEATED_LINE_PAGE_RATIO = 0.5;
    private static final int MIN_PAGES_FOR_REPEATED_LINE_DETECTION = 3;
    private static final int MIN_TITLES_ON_TOC_PAGE = 5;
    private static final double TOC_PAGE_TITLE_RATIO = 0.6;

    private final int maxChars;

    public SectionAwareChunker(int maxChars) {
        if (maxChars < 200) {
            throw new IllegalArgumentException("maxChars must be at least 200");
        }
        this.maxChars = maxChars;
    }

    public List<DocumentChunk> chunk(SourceDocument document, List<DocumentPage> pages) {
        Objects.requireNonNull(document, "document must not be null");
        Objects.requireNonNull(pages, "pages must not be null");

        Set<String> repeatedLines = repeatedLines(pages);
        Builder builder = new Builder(document);

        for (DocumentPage page : pages) {
            List<String> lines = contentLines(page, repeatedLines);
            if (isTableOfContents(lines)) {
                continue;
            }
            for (String line : lines) {
                if (TOC_LINE.matcher(line).matches()) {
                    continue;
                }
                Matcher heading = HEADING.matcher(line);
                if (heading.matches()) {
                    builder.startSection(line, page.number());
                    builder.append(line, page.number(), true);
                } else {
                    builder.append(line, page.number(), false);
                }
            }
        }
        builder.flush();
        return builder.chunks;
    }

    private static List<String> contentLines(DocumentPage page, Set<String> repeatedLines) {
        List<String> lines = new ArrayList<>();
        for (String rawLine : page.text().split("\\R")) {
            String line = rawLine.strip();
            if (!line.isEmpty() && !repeatedLines.contains(normalize(line))) {
                lines.add(line);
            }
        }
        return lines;
    }

    /** A page where most lines are numbered titles is a table of contents. */
    private static boolean isTableOfContents(List<String> lines) {
        long titles = lines.stream()
                .filter(line -> TOC_LINE.matcher(line).matches() || NUMBERED_TITLE.matcher(line).matches())
                .count();
        return titles >= MIN_TITLES_ON_TOC_PAGE && titles >= lines.size() * TOC_PAGE_TITLE_RATIO;
    }

    /** Lines that appear on at least half of the pages, compared with digits masked. */
    private static Set<String> repeatedLines(List<DocumentPage> pages) {
        if (pages.size() < MIN_PAGES_FOR_REPEATED_LINE_DETECTION) {
            return Set.of();
        }
        Map<String, Integer> pageCounts = new HashMap<>();
        for (DocumentPage page : pages) {
            Set<String> seenOnPage = new HashSet<>();
            for (String line : page.text().split("\\R")) {
                String normalized = normalize(line.strip());
                if (!normalized.isEmpty() && seenOnPage.add(normalized)) {
                    pageCounts.merge(normalized, 1, Integer::sum);
                }
            }
        }
        int threshold = (int) Math.ceil(pages.size() * REPEATED_LINE_PAGE_RATIO);
        Set<String> repeated = new HashSet<>();
        pageCounts.forEach((line, count) -> {
            if (count >= threshold) {
                repeated.add(line);
            }
        });
        return repeated;
    }

    private static String normalize(String line) {
        return DIGITS.matcher(line).replaceAll("#");
    }

    /** Accumulates lines into chunks; one instance per document. */
    private final class Builder {

        private final SourceDocument document;
        private final List<DocumentChunk> chunks = new ArrayList<>();
        private final StringBuilder text = new StringBuilder();
        private boolean hasBody;
        private String section;
        private int startPage;

        private Builder(SourceDocument document) {
            this.document = document;
        }

        void startSection(String heading, int page) {
            // A section made only of headings so far is carried over as context, not emitted alone.
            if (hasBody) {
                flush();
            }
            section = heading;
            if (text.length() == 0) {
                startPage = page;
            }
        }

        void append(String line, int page, boolean heading) {
            // A single line longer than a chunk (rare: a huge table cell) is cut into pieces.
            for (int start = 0; start < line.length(); start += maxChars) {
                appendPiece(line.substring(start, Math.min(line.length(), start + maxChars)), page);
            }
            if (!heading) {
                hasBody = true;
            }
        }

        private void appendPiece(String piece, int page) {
            if (text.length() > 0 && text.length() + 1 + piece.length() > maxChars) {
                flush();
            }
            if (text.length() == 0) {
                startPage = page;
            } else {
                text.append('\n');
            }
            text.append(piece);
        }

        void flush() {
            if (!text.toString().isBlank()) {
                SourceReference source = new SourceReference(document.title(), section, startPage);
                chunks.add(new DocumentChunk(chunks.size(), text.toString(), source));
            }
            text.setLength(0);
            hasBody = false;
        }
    }
}
