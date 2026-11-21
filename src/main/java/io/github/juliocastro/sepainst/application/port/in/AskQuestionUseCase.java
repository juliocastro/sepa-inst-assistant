package io.github.juliocastro.sepainst.application.port.in;

import io.github.juliocastro.sepainst.domain.model.Answer;
import io.github.juliocastro.sepainst.domain.model.Question;

/** Answers a question using only the indexed public documents, citing its sources. */
public interface AskQuestionUseCase {

    Answer ask(Question question);
}
