/**
 * Application layer: the use cases of the assistant and the ports they need.
 * <ul>
 *   <li>{@code port.in} — what the outside world can ask the application to do (use cases)</li>
 *   <li>{@code port.out} — what the application needs from the outside world (search, LLM, storage)</li>
 *   <li>{@code service} — use case implementations, orchestrating domain objects and outbound ports</li>
 * </ul>
 * Plain Java only: it depends on the domain, never on adapters or on Spring.
 */
package io.github.juliocastro.sepainst.application;
