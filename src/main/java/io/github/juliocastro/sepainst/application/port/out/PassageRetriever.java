package io.github.juliocastro.sepainst.application.port.out;

import java.util.List;

import io.github.juliocastro.sepainst.domain.model.Passage;
import io.github.juliocastro.sepainst.domain.model.Question;

/** Finds the passages of the indexed documents most relevant to a question. */
public interface PassageRetriever {

    /**
     * @param question   the user's question
     * @param maxResults the maximum number of passages to return (top-k)
     * @return passages ordered from most to least relevant
     */
    List<Passage> findRelevant(Question question, int maxResults);
}
