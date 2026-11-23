package io.github.juliocastro.sepainst.application.port.out;

import java.util.List;

import io.github.juliocastro.sepainst.domain.model.GeneratedAnswer;
import io.github.juliocastro.sepainst.domain.model.Passage;
import io.github.juliocastro.sepainst.domain.model.Question;

/** Writes an answer to a question based only on the given passages (implemented by an LLM). */
public interface AnswerGenerator {

    /**
     * @param passages the passages the answer may use, numbered from 1 in this order
     * @return the answer and the numbers of the passages it cites
     */
    GeneratedAnswer generate(Question question, List<Passage> passages);
}
