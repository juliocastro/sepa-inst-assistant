package io.github.juliocastro.sepainst.adapter.out.document;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.juliocastro.sepainst.application.port.out.SourceCatalog;
import io.github.juliocastro.sepainst.domain.model.SourceDocument;

/** Source catalog defined in configuration ({@code sepainst.sources.documents}). */
public class ConfiguredSourceCatalog implements SourceCatalog {

    private final List<SourceDocument> documents;

    public ConfiguredSourceCatalog(List<SourceDocument> documents) {
        this.documents = List.copyOf(documents);
        Set<String> ids = new HashSet<>();
        for (SourceDocument document : this.documents) {
            if (!ids.add(document.id())) {
                throw new IllegalArgumentException("Duplicate document id in catalog: " + document.id());
            }
        }
    }

    @Override
    public List<SourceDocument> all() {
        return documents;
    }
}
