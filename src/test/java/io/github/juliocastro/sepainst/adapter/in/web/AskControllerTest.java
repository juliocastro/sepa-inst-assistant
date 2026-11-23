package io.github.juliocastro.sepainst.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.juliocastro.sepainst.adapter.in.web.AskController.AskRequest;
import io.github.juliocastro.sepainst.adapter.in.web.AskController.AskResponse;
import io.github.juliocastro.sepainst.adapter.in.web.AskController.Source;
import io.github.juliocastro.sepainst.domain.model.Answer;
import io.github.juliocastro.sepainst.domain.model.SourceReference;

class AskControllerTest {

    @Test
    void mapsTheAnswerAndItsSourcesToTheResponse() {
        AskController controller = new AskController(question -> Answer.grounded(
                "Answer to: " + question.text(), List.of(new SourceReference("EPC122-16 IG", "2.3 Reject", 48))));

        AskResponse response = controller.ask(new AskRequest("  When is a negative pacs.002 sent?  "));

        assertThat(response.answer()).isEqualTo("Answer to: When is a negative pacs.002 sent?");
        assertThat(response.grounded()).isTrue();
        assertThat(response.sources()).containsExactly(new Source("EPC122-16 IG", "2.3 Reject", 48));
    }

    @Test
    void returnsNotGroundedAnswersAsTheyAre() {
        AskController controller = new AskController(question -> Answer.notFound());

        AskResponse response = controller.ask(new AskRequest("What is the weather?"));

        assertThat(response.grounded()).isFalse();
        assertThat(response.sources()).isEmpty();
    }

    @Test
    void rejectsMissingOrBlankQuestions() {
        AskController controller = new AskController(question -> Answer.notFound());

        assertThatThrownBy(() -> controller.ask(new AskRequest("  ")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("question must not be blank");
        assertThatThrownBy(() -> controller.ask(new AskRequest(null)))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> controller.ask(null))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsTooLongQuestions() {
        AskController controller = new AskController(question -> Answer.notFound());

        assertThatThrownBy(() -> controller.ask(new AskRequest("x".repeat(2_001))))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("at most 2000");
    }
}
