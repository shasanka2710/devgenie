---
name: RAG Pipeline Design
domain: ai-llm
complexity: L3
output-format: runbook
token-estimate: high
tags: rag, retrieval-augmented-generation, llm, vector-database, spring-boot, langchain4j, embeddings, ai
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt when designing a Retrieval-Augmented Generation (RAG) pipeline for a Java/Spring Boot service — for example, an internal documentation Q&A system, code search, runbook assistant, or enterprise knowledge base. Ideal at the start of an AI feature design, or when evaluating RAG architecture options before implementation.

## Prerequisites

- `[RAG_USE_CASE]` — Specific use case (e.g., "Answer questions about internal runbooks and ADRs," "Code review suggestions based on internal style guide").
- `[DOCUMENT_SOURCES]` — Types and locations of documents to index (e.g., Confluence, GitHub repos, OCP cluster manifests, Kafka schemas).
- `[EXPECTED_QUERY_VOLUME]` — Queries per day.
- `[LATENCY_SLA_MS]` — Acceptable end-to-end response latency in milliseconds.
- `[LLM_PROVIDER]` — LLM provider and model (e.g., OpenAI GPT-4o, Azure OpenAI GPT-4, AWS Bedrock Claude 3.5).
- `[EMBEDDING_MODEL]` — Embedding model (e.g., text-embedding-3-large, sentence-transformers/all-MiniLM-L6-v2).
- `[VECTOR_STORE]` — Vector store technology (e.g., pgvector / Weaviate / Pinecone / Chroma / OpenSearch).
- `[DATA_CLASSIFICATION]` — Data sensitivity of the indexed documents (public / internal / confidential / restricted).
- `[SPRING_AI_OR_LANGCHAIN4J]` — Framework preference: Spring AI / LangChain4j / custom.
- `[DEPLOYMENT_PLATFORM]` — Where the RAG service will run (OCP / AWS / Azure).

## The Prompt

```
You are a principal engineer designing a production-grade RAG pipeline for a Java Spring Boot application.

Context:
- Use case: [RAG_USE_CASE]
- Document sources: [DOCUMENT_SOURCES]
- Expected query volume: [EXPECTED_QUERY_VOLUME] queries/day
- Latency SLA: [LATENCY_SLA_MS] ms end-to-end
- LLM: [LLM_PROVIDER]
- Embedding model: [EMBEDDING_MODEL]
- Vector store: [VECTOR_STORE]
- Data classification: [DATA_CLASSIFICATION]
- Framework: [SPRING_AI_OR_LANGCHAIN4J]
- Deployment: [DEPLOYMENT_PLATFORM]

Task:
Produce a RAG Pipeline Design Runbook with the following sections:

## 1. RAG Architecture Overview
Describe the full pipeline in a structured table:
| Stage | Component | Technology | Input | Output | Latency Budget (ms) |
Stages: Document Ingestion → Chunking → Embedding → Vector Store → Query → Retrieval → Augmentation → Generation → Response

## 2. Document Ingestion Design
For each source in [DOCUMENT_SOURCES]:
| Source | Connector / Loader | Format | Update Frequency | Auth Method | Data Classification |

Provide a Java code block using [SPRING_AI_OR_LANGCHAIN4J] for the document loader implementation:
- DocumentReader / DocumentLoader interface
- Markdown / PDF / HTML parsing
- Metadata extraction strategy (source URL, last modified, author, section)

## 3. Chunking Strategy
| Strategy | Chunk Size (tokens) | Overlap | Pros | Cons | Recommended For |
Strategies to compare:
- Fixed-size chunking
- Sentence / paragraph chunking
- Semantic chunking (sentence transformer)
- Hierarchical chunking (parent-child)

Select and justify the chunking strategy for [RAG_USE_CASE].

Provide a Java code block for the chunking implementation.

## 4. Embedding Model Selection
| Criterion | [EMBEDDING_MODEL] | Alternative | Recommendation |
| Dimensionality | | | |
| Semantic quality (MTEB score) | | | |
| Cost per 1M tokens | | | |
| Latency | | | |
| Max sequence length | | | |
| On-premises availability | | | |

## 5. Vector Store Configuration
For [VECTOR_STORE]:
| Parameter | Value | Rationale |
| Index type | HNSW / IVFFlat / Flat | |
| Distance metric | cosine / dot product / L2 | |
| Dimensions | (from embedding model) | |
| ef_construction / m parameters | | |
| Index rebuild strategy | | |

Provide the Spring AI / LangChain4j vector store configuration code block.

## 6. Retrieval Strategy Design
Compare retrieval approaches and recommend:
| Strategy | Description | Precision | Recall | Complexity | Recommended? |
| Naive similarity search | Top-k cosine similarity | | | Low | |
| MMR (Max Marginal Relevance) | Diversity-aware retrieval | | | Medium | |
| Hybrid search (BM25 + vector) | Keyword + semantic | | | High | |
| Re-ranking (cross-encoder) | Two-stage retrieval | | | High | |
| HyDE (Hypothetical Document Embedding) | Query expansion | | | Medium | |

Provide a Java code block for the recommended retrieval strategy.

## 7. Prompt Augmentation Design
Provide the system prompt template for the RAG use case [RAG_USE_CASE]:
```
[SYSTEM_PROMPT_TEMPLATE]
```
Include:
- Role definition
- Context injection placeholder ({context})
- Source citation instruction
- Hallucination mitigation instruction ("If the answer is not in the provided context, say 'I don't know'")
- Response format instruction (markdown / structured JSON / plain text)

## 8. Evaluation Framework
Define how to evaluate RAG quality:
| Metric | Tool | Target | Measurement Method |
| Answer Faithfulness | RAGAS / TruLens | > 0.9 | LLM-as-judge |
| Answer Relevance | RAGAS | > 0.85 | LLM-as-judge |
| Context Precision | RAGAS | > 0.8 | Retrieved chunk evaluation |
| Context Recall | RAGAS | > 0.8 | Ground truth comparison |
| End-to-end latency | Micrometer | < [LATENCY_SLA_MS] ms | p99 |

Provide a test dataset creation strategy for [RAG_USE_CASE].

## 9. Security and Data Classification
For [DATA_CLASSIFICATION]:
| Concern | Control | Implementation |
| PII in documents | Redaction before indexing | |
| Access control on retrieval | Row-level security in vector store | |
| LLM prompt injection | Input sanitization | |
| Sensitive data in LLM context | Data classification tagging | |
| Audit log of queries | Structured logging | |
| LLM provider data residency | [LLM_PROVIDER] data region | |

## 10. OCP Deployment Configuration
If deployed on OCP:
- Deployment YAML for the RAG service (Spring Boot + [SPRING_AI_OR_LANGCHAIN4J])
- Vector store deployment (if self-hosted)
- Resource sizing for embedding compute
- ConfigMap for LLM provider configuration (API keys via Vault)

## 11. Observability
| Metric | Description | Alert Threshold |
| RAG query latency p99 | End-to-end | > [LATENCY_SLA_MS] ms |
| Retrieval latency | Vector search time | > 100 ms |
| LLM response latency | API call time | > 3000 ms |
| Retrieval hit rate | % queries with relevant chunks | < 70% |
| LLM error rate | API errors | > 1% |
| Context token utilization | Tokens used / max context | > 80% |

Use Spring AI 1.x or LangChain4j 0.35+ APIs. Reference [LLM_PROVIDER] API documentation. Flag any token cost implications.
```

## Expected Output

A complete RAG pipeline design runbook containing:
- Architecture overview table (all pipeline stages)
- Document ingestion table + Java loader code
- Chunking strategy comparison table + Java code
- Embedding model comparison table
- Vector store configuration table + Java code
- Retrieval strategy comparison table + Java code
- System prompt template
- Evaluation metrics framework table
- Security controls table
- OCP deployment configuration
- Observability metrics table

## Benefits

- Produces a complete, implementable RAG design with Java code in one pass — eliminating the typical 2-week architecture spiking phase.
- Forces explicit decisions on chunking, retrieval strategy, and evaluation framework before implementation begins.
- Addresses data classification and access control, which are commonly overlooked in initial RAG designs.

## Related Prompts

- [ai-code-review-integration.md](ai-code-review-integration.md) — Apply RAG for AI-assisted code review.
- [prompt-engineering-for-code.md](prompt-engineering-for-code.md) — Craft effective prompts for the RAG system prompt template.
- [../observability/observability-design.md](../observability/observability-design.md) — Instrument the RAG pipeline for production monitoring.
