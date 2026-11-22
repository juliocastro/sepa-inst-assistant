package io.github.juliocastro.sepainst.adapter.out.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.juliocastro.sepainst.domain.model.SourceDocument;

class ConfiguredSourceCatalogTest {

    private static final SourceDocument IG = new SourceDocument("IG", "IG", URI.create("https://example.org/ig.pdf"));

    @Test
    void findsDocumentsById() {
        ConfiguredSourceCatalog catalog = new ConfiguredSourceCatalog(List.of(IG));

        assertThat(catalog.find("IG")).contains(IG);
        assertThat(catalog.find("OTHER")).isEmpty();
    }

    @Test
    void rejectsDuplicateIds() {
        assertThatThrownBy(() -> new ConfiguredSourceCatalog(List.of(IG, IG)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IG");
    }
}
