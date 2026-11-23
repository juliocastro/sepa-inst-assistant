package io.github.juliocastro.sepainst.adapter.in.web;

import java.util.List;
import java.util.Objects;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.juliocastro.sepainst.application.port.in.AskQuestionUseCase;
import io.github.juliocastro.sepainst.domain.model.Answer;
import io.github.juliocastro.sepainst.domain.model.Question;
import io.github.juliocastro.sepainst.domain.model.SourceReference;

/** {@code POST /ask}: answers a question about SCT Inst / ISO 20022, citing the official documents. */
@RestController
public class AskController {

    private final AskQuestionUseCase askQuestion;

    public AskController(AskQuestionUseCase askQuestion) {
        this.askQuestion = Objects.requireNonNull(askQuestion, "askQuestion must not be null");
    }

    @PostMapping(path = "/ask", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public AskResponse ask(@RequestBody AskRequest request) {
        Answer answer = askQuestion.ask(toQuestion(request));
        return AskResponse.from(answer);
    }

    private static Question toQuestion(AskRequest request) {
        String text = request == null || request.question() == null ? "" : request.question();
        try {
            return new Question(text);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("question " + e.getMessage().replaceFirst("^text ", ""));
        }
    }

    public record AskRequest(String question) {
    }

    public record AskResponse(String answer, boolean grounded, List<Source> sources) {

        static AskResponse from(Answer answer) {
            return new AskResponse(answer.text(), answer.grounded(),
                    answer.sources().stream().map(Source::from).toList());
        }
    }

    public record Source(String document, String section, Integer page) {

        static Source from(SourceReference reference) {
            return new Source(reference.document(), reference.section(), reference.page());
        }
    }
}
