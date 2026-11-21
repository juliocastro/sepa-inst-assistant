# sepa-inst-assistant

A retrieval-augmented generation (RAG) assistant for **SEPA Instant Credit Transfer (SCT Inst)** and **ISO 20022** payment messages, built with **Java 21, Spring Boot and Spring AI**.

Ask a question about the scheme, for example *"When must the beneficiary PSP send a negative pacs.002?"*, and get an answer grounded in the official EPC documents, **with the exact source** (document, section, page).

> Educational portfolio project. It uses **only publicly available documents** (European Payments Council rulebooks and implementation guidelines, the ISO 20022 message catalogue) and **synthetic data**. It is not affiliated with, and contains no code or information from, any payment institution.

## Status

| Milestone | Scope | Status |
| --- | --- | --- |
| 1. Skeleton | Spring Boot + Spring AI, PostgreSQL/pgvector, Ollama, health check, integration test | Done |
| 2. First ingestion | Download EPC documents, chunk by section, embed, store with metadata | Next |
| 3. `/ask` v1 | Top-k retrieval + answer with sources (Ollama) | Planned |
| 4. Claude for answers | Hybrid: local embeddings, Claude for final answers | Planned |
| 5. Evaluation | 20+ questions with expected answer and source, run in the build | Planned |
| 6. Observability | Latency, token usage, retrieved chunks per answer | Planned |
| Phase 2 | Reconciliation agent with tool calling and human approval | Planned |

## Architecture

```
Public sources (EPC, ISO 20022) ──► Ingestion ──► pgvector
                                                    │
Client ──► /ask ──► embed question ──► top-k search ┘──► LLM ──► answer + sources
```

| Component | Role |
| --- | --- |
| Spring Boot 4.2 | REST API and operations (Actuator) |
| Spring AI 2.1 | Chat, embeddings and vector store abstractions |
| PostgreSQL + pgvector | Chunks, metadata and embeddings |
| Ollama | Local models for embeddings and development |
| Testcontainers | Integration tests against a real pgvector database |

## Running locally

Requirements: Java 21, Maven 3.9+, Docker.

```bash
# 1. Ollama: either run it natively (https://ollama.com) or start it with Docker
docker compose --profile ollama up -d ollama

# 2. Pull the models (once)
ollama pull nomic-embed-text      # embeddings (768 dimensions)
ollama pull qwen3:8b              # chat; pick a smaller model if your machine needs it

# 3. Run the app — PostgreSQL/pgvector is started automatically from compose.yaml
mvn spring-boot:run

# 4. Check it
curl http://localhost:8080/actuator/health
```

Models and the Ollama URL can be changed with `OLLAMA_BASE_URL`, `OLLAMA_CHAT_MODEL`, `OLLAMA_EMBEDDING_MODEL` and `EMBEDDING_DIMENSIONS` (must match the embedding model).

## Tests

```bash
mvn verify
```

The integration test starts a `pgvector/pgvector:pg17` container, so Docker must be running. Ollama is not needed for the tests.

## Sources

Source documents are **downloaded locally and never committed** (`sources/` is git-ignored): they are public, but subject to their publishers' intellectual property rights.

- European Payments Council — [SEPA Instant Credit Transfer](https://www.europeanpaymentscouncil.eu/what-we-do/sepa-instant-credit-transfer) rulebook, implementation guidelines and clarification paper
- ISO 20022 — [Catalogue of messages](https://www.iso20022.org/iso-20022-message-definitions)
