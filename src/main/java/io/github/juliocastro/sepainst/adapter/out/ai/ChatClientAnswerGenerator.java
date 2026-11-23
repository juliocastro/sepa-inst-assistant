package io.github.juliocastro.sepainst.adapter.out.ai;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClient;

import io.github.juliocastro.sepainst.application.port.out.AnswerGenerator;
import io.github.juliocastro.sepainst.domain.model.GeneratedAnswer;
import io.github.juliocastro.sepainst.domain.model.Passage;
import io.github.juliocastro.sepainst.domain.model.Question;
import io.github.juliocastro.sepainst.domain.model.SourceReference;

/**
 * Generates answers with a chat model through Spring AI's {@link ChatClient}. The model only sees
 * the retrieved passages and is instructed to answer from them alone and to cite them.
 */
public class ChatClientAnswerGenerator implements AnswerGenerator {

    static final String SYSTEM_PROMPT = """
            You are an assistant for the SEPA Instant Credit Transfer (SCT Inst) scheme and the
            ISO 20022 messages it uses. You help payment engineers and analysts.

            Rules:
            - Answer ONLY from the numbered passages provided. Do not use outside knowledge.
            - If the passages do not contain the answer, say so plainly instead of guessing.
            - Cite the passages you used with their numbers, e.g. [1] or [2][3]. Cite only passages
              that actually support the answer.
            - If the passages do not answer the question, do not cite any passage.
            - Keep message names, element names and codes exactly as written (pacs.008, AM04...).
            - Be concise: a direct answer first, then the supporting detail.
            - Answer in the language of the question.
            """;

    /** Citation markers such as [1], [2], [3, 4] or [2-4]. */
    private static final Pattern CITATION = Pattern.compile("\\[(\\d{1,2}(?:\\s*[,\\-–]\\s*\\d{1,2})*)\\]");

    private final ChatClient chatClient;

    public ChatClientAnswerGenerator(ChatClient chatClient) {
        this.chatClient = Objects.requireNonNull(chatClient, "chatClient must not be null");
    }

    @Override
    public GeneratedAnswer generate(Question question, List<Passage> passages) {
        String answer = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage(question, passages))
                .call()
                .content();
        if (answer == null || answer.isBlank()) {
            throw new IllegalStateException("The model returned an empty answer");
        }
        String text = answer.strip();
        return new GeneratedAnswer(text, citations(text));
    }

    /** Passage numbers cited in the text: [1], [2][3], [1, 3] and ranges like [2-4]. */
    static Set<Integer> citations(String text) {
        Set<Integer> numbers = new LinkedHashSet<>();
        Matcher matcher = CITATION.matcher(text);
        while (matcher.find()) {
            for (String part : matcher.group(1).split("\\s*,\\s*")) {
                String[] range = part.split("\\s*[-–]\\s*");
                int from = Integer.parseInt(range[0]);
                int to = Integer.parseInt(range[range.length - 1]);
                for (int n = Math.min(from, to); n <= Math.max(from, to) && n - Math.min(from, to) < 20; n++) {
                    numbers.add(n);
                }
            }
        }
        return numbers;
    }

    static String userMessage(Question question, List<Passage> passages) {
        StringBuilder message = new StringBuilder("Passages:\n\n");
        for (int i = 0; i < passages.size(); i++) {
            Passage passage = passages.get(i);
            message.append('[').append(i + 1).append("] ").append(describe(passage.source())).append('\n')
                    .append(passage.content().strip()).append("\n\n");
        }
        return message.append("Question: ").append(question.text()).toString();
    }

    private static String describe(SourceReference source) {
        StringBuilder text = new StringBuilder(source.document());
        if (source.section() != null) {
            text.append(", section ").append(source.section());
        }
        if (source.page() != null) {
            text.append(", page ").append(source.page());
        }
        return text.toString();
    }
}
