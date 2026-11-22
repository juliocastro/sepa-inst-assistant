package io.github.juliocastro.sepainst.adapter.out.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.juliocastro.sepainst.domain.model.DocumentPage;

class PdfBoxDocumentReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsTheTextOfEachPageWithItsNumber() throws IOException {
        Path pdf = writePdf(tempDir.resolve("test.pdf"), "1 Introduction", "2.1 Negative pacs.002");

        List<DocumentPage> pages = new PdfBoxDocumentReader().read(pdf);

        assertThat(pages).hasSize(2);
        assertThat(pages.get(0).number()).isEqualTo(1);
        assertThat(pages.get(0).text()).contains("1 Introduction");
        assertThat(pages.get(1).number()).isEqualTo(2);
        assertThat(pages.get(1).text()).contains("2.1 Negative pacs.002");
    }

    @Test
    void reportsFilesThatAreNotPdfs() throws IOException {
        Path notPdf = Files.writeString(tempDir.resolve("fake.pdf"), "not a pdf");

        assertThatThrownBy(() -> new PdfBoxDocumentReader().read(notPdf))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("fake.pdf");
    }

    private static Path writePdf(Path file, String... pageTexts) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (String text : pageTexts) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(font, 12);
                    content.newLineAtOffset(72, 700);
                    content.showText(text);
                    content.endText();
                }
            }
            document.save(file.toFile());
        }
        return file;
    }
}
