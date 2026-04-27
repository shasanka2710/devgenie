# Meta-Prompt Library for Java Principal Engineers

A curated, production-ready collection of reusable meta-prompts organized by engineering domain. Every prompt is specific to the Java/Spring Boot enterprise ecosystem and is immediately usable with `[PLACEHOLDER]` markers.

## Tech Stack Coverage

| Technology | Category |
|---|---|
| Core Java / Spring Boot | Application Framework |
| Apache Kafka | Event Streaming |
| Apache Flink | Stream Processing |
| MongoDB | NoSQL Data Store |
| OpenShift Container Platform (OCP) | Container Orchestration |
| Harness CI/CD | Continuous Delivery |
| HashiCorp Vault | Secrets Management |
| Istio Service Mesh | Service Networking |
| SonarQube | Code Quality |
| Snyk | Security Scanning |

---

## Prompt Index

| Prompt | Domain | Complexity | When to Use | Output Format | File |
|---|---|---|---|---|---|
| Microservices Decomposition | architecture | L3 | Decomposing a monolith or redesigning domain boundaries | ADR + scorecard | [architecture/microservices-decomposition.md](architecture/microservices-decomposition.md) |
| Event-Driven Architecture Review | architecture | L3 | Reviewing or proposing an event-driven design with Kafka | scorecard | [architecture/event-driven-architecture-review.md](architecture/event-driven-architecture-review.md) |
| API Contract Design | architecture | L2 | Designing or reviewing REST/gRPC/AsyncAPI contracts | markdown-table | [architecture/api-contract-design.md](architecture/api-contract-design.md) |
| Resilience Pattern Review | architecture | L2 | Hardening microservices against failures | scorecard | [architecture/resilience-pattern-review.md](architecture/resilience-pattern-review.md) |
| Spring Boot Code Review | code-quality | L2 | Pull request review or code-quality gate | scorecard | [code-quality/spring-boot-code-review.md](code-quality/spring-boot-code-review.md) |
| Tech Debt Classification | code-quality | L2 | Quarterly tech-debt prioritization sessions | markdown-table | [code-quality/tech-debt-classification.md](code-quality/tech-debt-classification.md) |
| Refactoring Strategy | code-quality | L3 | Planning incremental refactors on legacy Spring services | runbook | [code-quality/refactoring-strategy.md](code-quality/refactoring-strategy.md) |
| mTLS Service Mesh Review | security | L3 | Auditing Istio mTLS posture across services | scorecard | [security/mtls-service-mesh-review.md](security/mtls-service-mesh-review.md) |
| Secrets Management Review | security | L2 | Auditing Vault integration and secret hygiene | scorecard | [security/secrets-management-review.md](security/secrets-management-review.md) |
| Security Architecture Review | security | L3 | End-to-end threat modelling for a new service or feature | ADR | [security/security-architecture-review.md](security/security-architecture-review.md) |
| Kafka Topic Design | data-streaming | L2 | Designing or reviewing Kafka topics for a new event type | markdown-table | [data-streaming/kafka-topic-design.md](data-streaming/kafka-topic-design.md) |
| Flink Pipeline Design | data-streaming | L3 | Designing a stateful stream processing pipeline | runbook | [data-streaming/flink-pipeline-design.md](data-streaming/flink-pipeline-design.md) |
| MongoDB Schema Review | data-streaming | L2 | Reviewing or evolving a MongoDB collection schema | scorecard | [data-streaming/mongodb-schema-review.md](data-streaming/mongodb-schema-review.md) |
| CI/CD Pipeline Review | devops-platform | L2 | Auditing or redesigning a Harness pipeline | scorecard | [devops-platform/cicd-pipeline-review.md](devops-platform/cicd-pipeline-review.md) |
| OCP Migration Strategy | devops-platform | L3 | Planning workload migration to OpenShift | runbook | [devops-platform/ocp-migration-strategy.md](devops-platform/ocp-migration-strategy.md) |
| Release Strategy Review | devops-platform | L2 | Reviewing release cadence, gates, and rollback plans | markdown-table | [devops-platform/release-strategy-review.md](devops-platform/release-strategy-review.md) |
| Observability Design | observability | L2 | Instrumenting a new or existing service for production | runbook | [observability/observability-design.md](observability/observability-design.md) |
| Incident RCA | observability | L2 | Writing a post-incident root cause analysis | narrative | [observability/incident-rca.md](observability/incident-rca.md) |
| SLO Definition | observability | L2 | Defining Service Level Objectives for a service | markdown-table | [observability/slo-definition.md](observability/slo-definition.md) |
| ADR Generation | documentation | L1 | Capturing an architectural decision with context and rationale | ADR | [documentation/adr-generation.md](documentation/adr-generation.md) |
| Release Notes Generation | documentation | L1 | Generating structured release notes from commits/tickets | narrative | [documentation/release-notes-generation.md](documentation/release-notes-generation.md) |
| Technical Specification | documentation | L2 | Writing a detailed technical spec for a new feature | narrative | [documentation/technical-specification.md](documentation/technical-specification.md) |
| RAG Pipeline Design | ai-llm | L3 | Designing a Retrieval-Augmented Generation pipeline for code/docs | runbook | [ai-llm/rag-pipeline-design.md](ai-llm/rag-pipeline-design.md) |
| AI Code Review Integration | ai-llm | L2 | Embedding AI-assisted code review into CI/CD pipeline | runbook | [ai-llm/ai-code-review-integration.md](ai-llm/ai-code-review-integration.md) |
| Prompt Engineering for Code | ai-llm | L1 | Crafting precise prompts for code generation and review tasks | narrative | [ai-llm/prompt-engineering-for-code.md](ai-llm/prompt-engineering-for-code.md) |

---

## Complexity Legend

| Level | Meaning |
|---|---|
| L1 | Focused, single-output task. Low context required. |
| L2 | Multi-faceted review or design. Moderate context. |
| L3 | Cross-cutting, system-level reasoning. High context. |

## How to Use a Prompt

1. Open the relevant `.md` file for your task.
2. Collect all items listed under **Prerequisites**.
3. Replace every `[PLACEHOLDER]` in **The Prompt** section with real values.
4. Paste the filled-in prompt into your LLM of choice.
5. Validate the output against the **Expected Output** description.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for prompt quality standards, the review process, versioning, and how to test a prompt before merging.
