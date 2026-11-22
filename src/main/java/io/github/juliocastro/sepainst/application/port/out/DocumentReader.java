package io.github.juliocastro.sepainst.application.port.out;

import java.nio.file.Path;
import java.util.List;

import io.github.juliocastro.sepainst.domain.model.DocumentPage;

/** Extracts the text of a local document, page by page. */
public interface DocumentReader {

    List<DocumentPage> read(Path file);
}
