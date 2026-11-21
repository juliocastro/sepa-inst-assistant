package io.github.juliocastro.sepainst.application.port.out;

import java.util.List;

import io.github.juliocastro.sepainst.domain.model.Passage;
import io.github.juliocastro.sepainst.domain.model.Question;

/** Writes an answer to a question based only on the given passages (implemented by an LLM). */
public interface AnswerGenerator {

    String generate(Question question, List<Passage> passages);
}
