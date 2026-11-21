package io.github.juliocastro.sepainst.application.service;

import java.util.List;
import java.util.Objects;

import io.github.juliocastro.sepainst.application.port.in.AskQuestionUseCase;
import io.github.juliocastro.sepainst.application.port.out.AnswerGenerator;
import io.github.juliocastro.sepainst.application.port.out.PassageRetriever;
import io.github.juliocastro.sepainst.domain.model.Answer;
import io.github.juliocastro.sepainst.domain.model.Passage;
import io.github.juliocastro.sepainst.domain.model.Question;
import io.github.juliocastro.sepainst.domain.model.SourceReference;

/**
 * Retrieval-augmented answering: retrieve the most relevant passages, discard weak matches and
 * only then ask the generator. When nothing relevant is found, the assistant says so instead of
 * letting the model answer from memory.
 */
public class AskQuestionService implements AskQuestionUseCase {

    private final PassageRetriever retriever;
    private final AnswerGenerator generator;
    private final int maxResults;
    private final double minScore;

    public AskQuestionService(PassageRetriever retriever, AnswerGenerator generator,
                              int maxResults, double minScore) {
        this.retriever = Objects.requireNonNull(retriever, "retriever must not be null");
        this.generator = Objects.requireNonNull(generator, "generator must not be null");
        if (maxResults < 1) {
            throw new IllegalArgumentException("maxResults must be at least 1");
        }
        if (minScore < 0.0 || minScore > 1.0) {
            throw new IllegalArgumentException("minScore must be between 0 and 1");
        }
        this.maxResults = maxResults;
        this.minScore = minScore;
    }

    @Override
    public Answer ask(Question question) {
        Objects.requireNonNull(question, "question must not be null");

        List<Passage> relevant = retriever.findRelevant(question, maxResults).stream()
                .filter(passage -> passage.score() >= minScore)
                .toList();

        if (relevant.isEmpty()) {
            return Answer.notFound();
        }

        String text = generator.generate(question, relevant);
        List<SourceReference> sources = relevant.stream()
                .map(Passage::source)
                .distinct()
                .toList();

        return Answer.grounded(text, sources);
    }
}
