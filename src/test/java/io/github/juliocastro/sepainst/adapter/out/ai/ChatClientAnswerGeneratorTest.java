package io.github.juliocastro.sepainst.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import io.github.juliocastro.sepainst.domain.model.Passage;
import io.github.juliocastro.sepainst.domain.model.Question;
import io.github.juliocastro.sepainst.domain.model.SourceReference;

class ChatClientAnswerGeneratorTest {

    private static final Question QUESTION = new Question("When is a negative pacs.002 sent?");
    private static final List<Passage> PASSAGES = List.of(
            new Passage("Text about rejects.", new SourceReference("EPC122-16 IG", "2.3 Reject", 48), 0.9),
            new Passage("Text about timeouts.", new SourceReference("Rulebook", null, null), 0.7));

    @Test
    void numbersEachPassageWithItsSourceAndEndsWithTheQuestion() {
        String message = ChatClientAnswerGenerator.userMessage(QUESTION, PASSAGES);

        assertThat(message).contains(
                "[1] EPC122-16 IG, section 2.3 Reject, page 48\nText about rejects.",
                "[2] Rulebook\nText about timeouts.");
        assertThat(message).endsWith("Question: When is a negative pacs.002 sent?");
    }

    @Test
    void systemPromptRestrictsTheModelToThePassagesAndAsksForCitations() {
        assertThat(ChatClientAnswerGenerator.SYSTEM_PROMPT)
                .contains("ONLY from the numbered passages", "say so plainly", "[1]");
    }

    @Test
    void returnsTheModelAnswer() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("  A negative pacs.002 is sent when... [1]  ");

        String answer = new ChatClientAnswerGenerator(chatClient).generate(QUESTION, PASSAGES);

        assertThat(answer).isEqualTo("A negative pacs.002 is sent when... [1]");
    }

    @Test
    void failsOnAnEmptyAnswer() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content()).thenReturn(" ");

        assertThatThrownBy(() -> new ChatClientAnswerGenerator(chatClient).generate(QUESTION, PASSAGES))
                .isInstanceOf(IllegalStateException.class);
    }
}
