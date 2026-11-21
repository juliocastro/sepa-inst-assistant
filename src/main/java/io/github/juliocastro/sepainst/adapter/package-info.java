/**
 * Adapters: the technology at the edges of the application.
 * <ul>
 *   <li>{@code in.web} — REST API (driving adapter): translates HTTP into use case calls</li>
 *   <li>{@code out.ai} — Spring AI (driven adapters): pgvector search and LLM answer generation</li>
 *   <li>{@code out.document} — downloading and reading the public EPC / ISO 20022 documents</li>
 * </ul>
 * Adapters implement or call the application ports. Inbound and outbound adapters never depend
 * on each other.
 */
package io.github.juliocastro.sepainst.adapter;
