package io.github.juliocastro.sepainst.adapter.out.document;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import io.github.juliocastro.sepainst.application.port.out.DocumentReader;
import io.github.juliocastro.sepainst.domain.model.DocumentPage;

/** Extracts text from PDF files with Apache PDFBox, one page at a time to keep page numbers. */
public class PdfBoxDocumentReader implements DocumentReader {

    @Override
    public List<DocumentPage> read(Path file) {
        try (PDDocument pdf = Loader.loadPDF(file.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            List<DocumentPage> pages = new ArrayList<>(pdf.getNumberOfPages());
            for (int number = 1; number <= pdf.getNumberOfPages(); number++) {
                stripper.setStartPage(number);
                stripper.setEndPage(number);
                pages.add(new DocumentPage(number, stripper.getText(pdf)));
            }
            return pages;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read PDF " + file, e);
        }
    }
}
