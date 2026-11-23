package io.github.juliocastro.sepainst.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import io.github.juliocastro.sepainst.domain.model.Passage;
import io.github.juliocastro.sepainst.domain.model.Question;
import io.github.juliocastro.sepainst.domain.model.SourceReference;

class VectorStorePassageRetrieverTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final VectorStorePassageRetriever retriever = new VectorStorePassageRetriever(vectorStore);

    @Test
    void searchesWithTheQuestionAndMapsMetadataToSources() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                Document.builder()
                        .text("A negative pacs.002 is sent when...")
                        .metadata(Map.of(
                                VectorStorePassageStore.DOCUMENT_ID, "EPC122-16-2025",
                                VectorStorePassageStore.DOCUMENT_TITLE, "EPC122-16 IG",
                                VectorStorePassageStore.SECTION, "2.3 Reject",
                                VectorStorePassageStore.PAGE, 48))
                        .score(0.82)
                        .build()));

        List<Passage> passages = retriever.findRelevant(new Question("When is a negative pacs.002 sent?"), 4);

        ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(request.capture());
        assertThat(request.getValue().getQuery()).isEqualTo("When is a negative pacs.002 sent?");
        assertThat(request.getValue().getTopK()).isEqualTo(4);

        assertThat(passages).singleElement().satisfies(passage -> {
            assertThat(passage.content()).startsWith("A negative pacs.002");
            assertThat(passage.source()).isEqualTo(new SourceReference("EPC122-16 IG", "2.3 Reject", 48));
            assertThat(passage.score()).isEqualTo(0.82);
        });
    }

    @Test
    void toleratesNumbersStoredAsTextAndMissingMetadata() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                Document.builder()
                        .text("text")
                        .metadata(Map.of(VectorStorePassageStore.DOCUMENT_ID, "IG", VectorStorePassageStore.PAGE, "12"))
                        .score(1.0000001)
                        .build()));

        List<Passage> passages = retriever.findRelevant(new Question("q"), 3);

        assertThat(passages).singleElement().satisfies(passage -> {
            assertThat(passage.source()).isEqualTo(new SourceReference("IG", null, 12));
            assertThat(passage.score()).isEqualTo(1.0);
        });
    }
}
