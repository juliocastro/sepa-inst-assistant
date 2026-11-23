package io.github.juliocastro.sepainst.adapter.in.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps errors to RFC 9457 problem details. Standard Spring MVC errors (malformed JSON, wrong
 * content type...) are handled by the base class; everything else unexpected becomes a 503
 * without leaking internals to the client.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail invalidRequest(InvalidRequestException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Invalid request");
        return problem;
    }

    /** Failures of the model or the vector store. */
    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail unexpected(RuntimeException e) {
        log.error("Could not answer the question", e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "The assistant could not answer right now. Please try again later.");
        problem.setTitle("Assistant unavailable");
        return problem;
    }
}
