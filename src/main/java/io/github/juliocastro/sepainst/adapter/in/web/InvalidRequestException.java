package io.github.juliocastro.sepainst.adapter.in.web;

/** The client sent a request the API cannot process; mapped to HTTP 400. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
