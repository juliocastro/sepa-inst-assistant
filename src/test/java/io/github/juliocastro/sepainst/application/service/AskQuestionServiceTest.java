package io.github.juliocastro.sepainst.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.juliocastro.sepainst.application.port.out.AnswerGenerator;
import io.github.juliocastro.sepainst.application.port.out.PassageRetriever;
import io.github.juliocastro.sepainst.domain.model.Answer;
import io.github.juliocastro.sepainst.domain.model.Passage;
import io.github.juliocastro.sepainst.domain.model.Question;
import io.github.juliocastro.sepainst.domain.model.SourceReference;

/** Pure unit tests: no Spring, no database, no model — the ports are replaced by lambdas. */
class AskQuestionServiceTest {

    private static final SourceReference IG = new SourceReference("EPC122-16 SCT Inst Inter-PSP IG", "2.1", 12);
    private static final SourceReference RULEBOOK = new SourceReference("SCT Inst Rulebook", "4.2", 30);
    private static final Question QUESTION = new Question("When is a negative pacs.002 sent?");

    @Test
    void answersFromRelevantPassagesAndCitesEachSourceOnce() {
        PassageRetriever retriever = (question, max) -> List.of(
                new Passage("passage A", IG, 0.91),
                new Passage("passage B", IG, 0.85),
                new Passage("passage C", RULEBOOK, 0.80));
        AnswerGenerator generator = (question, passages) -> "generated from " + passages.size() + " passages";

        Answer answer = new AskQuestionService(retriever, generator, 5, 0.7).ask(QUESTION);

        assertThat(answer.grounded()).isTrue();
        assertThat(answer.text()).isEqualTo("generated from 3 passages");
        assertThat(answer.sources()).containsExactly(IG, RULEBOOK);
    }

    @Test
    void sendsOnlyPassagesAboveTheMinimumScoreToTheGenerator() {
        PassageRetriever retriever = (question, max) -> List.of(
                new Passage("strong", IG, 0.90),
                new Passage("weak", RULEBOOK, 0.40));
        List<Passage> received = new ArrayList<>();
        AnswerGenerator generator = (question, passages) -> {
            received.addAll(passages);
            return "answer";
        };

        Answer answer = new AskQuestionService(retriever, generator, 5, 0.7).ask(QUESTION);

        assertThat(received).extracting(Passage::content).containsExactly("strong");
        assertThat(answer.sources()).containsExactly(IG);
    }

    @Test
    void declinesToAnswerWithoutCallingTheModelWhenNothingIsRelevant() {
        PassageRetriever retriever = (question, max) -> List.of(new Passage("unrelated", IG, 0.30));
        AnswerGenerator generator = (question, passages) -> {
            throw new AssertionError("the model must not be called without relevant passages");
        };

        Answer answer = new AskQuestionService(retriever, generator, 5, 0.7).ask(QUESTION);

        assertThat(answer).isEqualTo(Answer.notFound());
        assertThat(answer.grounded()).isFalse();
        assertThat(answer.sources()).isEmpty();
    }

    @Test
    void asksTheRetrieverForTheConfiguredNumberOfResults() {
        List<Integer> requested = new ArrayList<>();
        PassageRetriever retriever = (question, max) -> {
            requested.add(max);
            return List.of();
        };

        new AskQuestionService(retriever, (q, p) -> "unused", 7, 0.7).ask(QUESTION);

        assertThat(requested).containsExactly(7);
    }

    @Test
    void rejectsInvalidConfiguration() {
        PassageRetriever retriever = (question, max) -> List.of();
        AnswerGenerator generator = (question, passages) -> "";

        assertThatThrownBy(() -> new AskQuestionService(retriever, generator, 0, 0.7))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AskQuestionService(retriever, generator, 5, 1.5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
