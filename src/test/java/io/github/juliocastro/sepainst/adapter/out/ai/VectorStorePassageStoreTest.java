package io.github.juliocastro.sepainst.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import io.github.juliocastro.sepainst.domain.model.DocumentChunk;
import io.github.juliocastro.sepainst.domain.model.SourceReference;

class VectorStorePassageStoreTest {

    private final VectorStore vectorStore = mock(VectorStore.class);

    @Test
    void deletesPreviousChunksThenAddsTheNewOnesWithMetadata() {
        new VectorStorePassageStore(vectorStore, 10).replace("IG", List.of(
                new DocumentChunk(0, "first", new SourceReference("Inter-PSP IG", "2.1 Scope", 7)),
                new DocumentChunk(1, "second", new SourceReference("Inter-PSP IG", null, 8))));

        InOrder order = inOrder(vectorStore);
        order.verify(vectorStore).delete(any(Filter.Expression.class));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> added = ArgumentCaptor.forClass(List.class);
        order.verify(vectorStore).add(added.capture());

        List<Document> documents = added.getValue();
        assertThat(documents).hasSize(2);
        assertThat(documents.get(0).getText()).isEqualTo("first");
        assertThat(documents.get(0).getMetadata())
                .containsEntry(VectorStorePassageStore.DOCUMENT_ID, "IG")
                .containsEntry(VectorStorePassageStore.DOCUMENT_TITLE, "Inter-PSP IG")
                .containsEntry(VectorStorePassageStore.SECTION, "2.1 Scope")
                .containsEntry(VectorStorePassageStore.PAGE, 7)
                .containsEntry(VectorStorePassageStore.CHUNK_INDEX, 0);
        assertThat(documents.get(1).getMetadata()).doesNotContainKey(VectorStorePassageStore.SECTION);
    }

    @Test
    void usesStableIdsSoReingestingOverwritesInsteadOfDuplicating() {
        assertThat(VectorStorePassageStore.chunkId("IG", 3)).isEqualTo(VectorStorePassageStore.chunkId("IG", 3));
        assertThat(VectorStorePassageStore.chunkId("IG", 3)).isNotEqualTo(VectorStorePassageStore.chunkId("IG", 4));
        assertThat(VectorStorePassageStore.chunkId("IG", 3)).isNotEqualTo(VectorStorePassageStore.chunkId("RB", 3));
    }

    @Test
    void addsChunksInBatches() {
        List<DocumentChunk> chunks = IntStream.range(0, 25)
                .mapToObj(i -> new DocumentChunk(i, "chunk " + i, new SourceReference("IG", null, 1)))
                .toList();

        new VectorStorePassageStore(vectorStore, 10).replace("IG", chunks);

        verify(vectorStore, times(3)).add(any());
    }
}
