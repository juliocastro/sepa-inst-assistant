# sepa-inst-assistant

A retrieval-augmented generation (RAG) assistant for **SEPA Instant Credit Transfer (SCT Inst)** and **ISO 20022** payment messages, built with **Java 21, Spring Boot and Spring AI**.

Ask a question about the scheme, for example *"When must the beneficiary PSP send a negative pacs.002?"*, and get an answer grounded in the official EPC documents, **with the exact source** (document, section, page).

> Educational portfolio project. It uses **only publicly available documents** (European Payments Council rulebooks and implementation guidelines, the ISO 20022 message catalogue) and **synthetic data**. It is not affiliated with, and contains no code or information from, any payment institution.

## Status

| Milestone | Scope | Status |
| --- | --- | --- |
| 1. Skeleton | Spring Boot + Spring AI, PostgreSQL/pgvector, hexagonal layers, architecture tests, core use case with unit tests | Done |
| 2. First ingestion | Download EPC documents, chunk by section, embed, store with metadata | Done |
| 3. `/ask` v1 | Top-k retrieval + answer with sources (Claude) | Next |
| 5. Evaluation | 20+ questions with expected answer and source, run in the build | Planned |
| 6. Observability | Latency, token usage, retrieved chunks per answer | Planned |
| Phase 2 | Reconciliation agent with tool calling and human approval | Planned |

## Architecture

```
Public sources (EPC, ISO 20022) ──► Ingestion ──► pgvector
                                                    │
Client ──► /ask ──► embed question ──► top-k search ┘──► LLM ──► answer + sources
```

### Code structure — hexagonal (ports and adapters)

```
io.github.juliocastro.sepainst
├── domain/                 Business concepts — plain Java, no framework, no I/O
│   ├── model/              Question, Answer, Passage, SourceDocument, DocumentChunk...
│   └── chunking/           SectionAwareChunker: splits documents by numbered section
├── application/            Use cases and the ports they need
│   ├── port/in/            What the app offers: AskQuestionUseCase, IngestDocumentUseCase
│   ├── port/out/           What the app needs: SourceCatalog, DocumentFetcher, DocumentReader,
│   │                       PassageStore, PassageRetriever, AnswerGenerator
│   └── service/            Use case implementations (plain Java)
├── adapter/                Technology at the edges
│   ├── in/cli/             --ingest command
│   ├── in/web/             REST API                          (milestone 3)
│   ├── out/ai/             Spring AI: pgvector storage, search, LLM
│   └── out/document/       Source catalog, HTTPS download, PDF reading (PDFBox)
└── config/                 Spring wiring — the only place services meet adapters
```

Dependencies point inwards only: `adapter → application → domain`. The domain and application layers have **no Spring dependency**, so the core logic is unit-tested without a database or a model. These rules are enforced on every build by [ArchUnit](https://www.archunit.org/) tests (`ArchitectureTest`).

### Components

| Component | Role |
| --- | --- |
| Spring Boot 4.2 | REST API and operations (Actuator) |
| Spring AI 2.1 | Chat, embeddings and vector store abstractions |
| PostgreSQL + pgvector | Chunks, metadata and embeddings |
| Google Gemini (`gemini-embedding-001`) | Embeddings, 768 dimensions — hosted, nothing heavy runs locally |
| Claude (Anthropic) | Answer generation (milestone 3) |
| Testcontainers | Integration tests against a real pgvector database |

## Running locally

Requirements: Java 21+, Docker and a Gemini API key. Maven is provided by the wrapper (`./mvnw`).

```bash
# 1. Get a free Gemini API key at https://aistudio.google.com and export it (never commit it)
export GEMINI_API_KEY=your-key

# 2. Run the app — PostgreSQL/pgvector is started automatically from compose.yaml
./mvnw spring-boot:run

# 3. Check it
curl http://localhost:8080/actuator/health
```

### Using Podman instead of Docker

Either give Podman a `docker` command (Spring Boot then manages `compose.yaml` as above):

```bash
sudo dnf install podman-docker docker-compose
systemctl --user enable --now podman.socket
export DOCKER_HOST=unix:///run/user/$UID/podman/podman.sock
```

or start the database yourself and tell Spring Boot not to:

```bash
podman compose up -d
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.docker.compose.enabled=false"
```

### Ingesting the documents

```bash
# Downloads the EPC documents listed in application.yaml (once), splits them by section,
# embeds every chunk with Gemini and stores it in pgvector. Safe to run again.
./mvnw spring-boot:run -Dspring-boot.run.arguments="--ingest=all --spring.main.web-application-type=none"
```

Use `--ingest=<id>` for a single document. If the download is blocked, save the PDF manually as `sources/<id>.pdf` — a local copy is always preferred over downloading.

How the documents are chunked:

1. Running headers and footers repeated on most pages are removed.
2. Table-of-contents lines (`2.1 Scope ........ 7`) are removed.
3. Every numbered heading (`2.1.3 Title`) starts a new chunk, so a chunk never mixes two sections.
4. Sections longer than 2,000 characters are split at line boundaries.

Each chunk is stored with its document, section, page and a stable id, so re-ingesting overwrites instead of duplicating.

On the Gemini free tier, chunks are embedded in batches of 20 with a 30-second pause, to stay within the tokens-per-minute limit. With a paid key, raise `sepainst.ingestion.batch-size` and set `sepainst.ingestion.pause-between-batches` to `0s`. Changing `EMBEDDING_DIMENSIONS` requires re-creating the `vector_store` table and re-ingesting.

## Tests

```bash
./mvnw verify
```

| Test | Type | Needs |
| --- | --- | --- |
| `AskQuestionServiceTest`, `IngestDocumentServiceTest` | Unit — use cases with fake ports | Nothing |
| `SectionAwareChunkerTest` | Unit — chunking rules | Nothing |
| `HttpDocumentFetcherTest`, `PdfBoxDocumentReaderTest`, `VectorStorePassageStoreTest`, ... | Adapter tests (local HTTP server, generated PDFs, mocked vector store) | Nothing |
| `ArchitectureTest` | Architecture rules (ArchUnit) | Nothing |
| `SepaInstAssistantApplicationTests` | Integration — app + real pgvector | Docker |

No API key is needed for the tests: no model is called. Without Docker, the integration test is skipped instead of failing.

On Fedora with Podman instead of Docker:

```bash
systemctl --user enable --now podman.socket
export DOCKER_HOST=unix:///run/user/$UID/podman/podman.sock
export TESTCONTAINERS_RYUK_DISABLED=true
```

## Sources

Source documents are **downloaded locally and never committed** (`sources/` is git-ignored): they are public, but subject to their publishers' intellectual property rights.

- European Payments Council — [SEPA Instant Credit Transfer](https://www.europeanpaymentscouncil.eu/what-we-do/sepa-instant-credit-transfer) rulebook, implementation guidelines and clarification paper
- ISO 20022 — [Catalogue of messages](https://www.iso20022.org/iso-20022-message-definitions)
