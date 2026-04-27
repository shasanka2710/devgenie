---
name: Event-Driven Architecture Review
domain: architecture
complexity: L3
output-format: scorecard
token-estimate: high
tags: event-driven, kafka, spring-boot, async, choreography, orchestration, schema-registry
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt when reviewing or proposing an event-driven architecture involving Apache Kafka for a Spring Boot system. Ideal for architecture reviews before a new Kafka-heavy feature goes to production, or when existing event flows are suffering from tight coupling, consumer lag, or schema drift.

## Prerequisites

- `[SYSTEM_NAME]` — Name of the system or service cluster under review.
- `[EVENT_CATALOG]` — List of current or proposed events (name, producer, consumer(s)).
- `[KAFKA_VERSION]` — Kafka version in use (e.g., 3.6, with or without KRaft).
- `[SCHEMA_REGISTRY]` — Schema registry in use (e.g., Confluent Schema Registry, AWS Glue, none).
- `[SERIALIZATION_FORMAT]` — Avro / Protobuf / JSON.
- `[CONSUMER_GROUP_STRATEGY]` — Current consumer group naming and assignment approach.
- `[RETENTION_POLICY]` — Current topic retention (time and/or size).
- `[ORDERING_REQUIREMENTS]` — Which events require strict ordering and at what granularity (e.g., per-customer-id).
- `[DEAD_LETTER_STRATEGY]` — Existing DLT/DLQ strategy (or "none").

## The Prompt

```
You are a principal engineer expert in Apache Kafka and event-driven architecture for Java/Spring Boot systems.

Context:
- System: [SYSTEM_NAME]
- Kafka version: [KAFKA_VERSION]
- Schema registry: [SCHEMA_REGISTRY]
- Serialization format: [SERIALIZATION_FORMAT]
- Consumer group strategy: [CONSUMER_GROUP_STRATEGY]
- Topic retention policy: [RETENTION_POLICY]
- Ordering requirements: [ORDERING_REQUIREMENTS]
- Dead-letter strategy: [DEAD_LETTER_STRATEGY]

Event catalog:
[EVENT_CATALOG]

Task:
Produce a structured Event-Driven Architecture Review report with the following sections:

## 1. Event Taxonomy Scorecard
Score each event in the catalog against these dimensions (1–5, 5 = best practice):
| Event Name | Producer | Consumer(s) | Schema Completeness | Ordering Guarantee | Idempotency Design | DLT Coverage | Overall Score |

## 2. Coupling Analysis
For each event, classify the coupling level: Temporal / Data / Behavioural. Return as a table:
| Event Name | Coupling Type | Evidence | Recommendation |

## 3. Schema Design Assessment
Evaluate the serialization format and schema registry setup:
- Forward/backward compatibility posture
- Schema evolution strategy (FULL_TRANSITIVE vs FORWARD vs BACKWARD)
- Missing fields that should be in every event envelope (event-id, correlation-id, causation-id, schema-version, source-service, timestamp-utc)
Return findings as a bulleted list, then a corrected example Avro schema snippet for one representative event.

## 4. Kafka Topic Design Review
Return a table reviewing existing topics and recommended changes:
| Topic Name | Partitions | Replication Factor | Retention | Compaction? | Key Strategy | Gap / Issue | Recommended Action |

## 5. Consumer Resilience Review
Assess each consumer group for:
- At-least-once vs exactly-once semantics configuration
- Retry strategy (Spring Kafka RetryTemplate / SeekToCurrentErrorHandler / DefaultErrorHandler)
- Back-pressure handling
- Consumer lag alerting
Return a scorecard:
| Consumer Group | Semantics | Retry Config | Back-pressure | Lag Alert | Score (1-5) |

## 6. Choreography vs Orchestration Assessment
For each multi-step event flow in the catalog, recommend choreography or orchestration (Saga via Spring Boot + Kafka) and justify.

## 7. Priority Recommendations
List the top 5 improvements ranked by impact/effort. Use this format:
| Rank | Recommendation | Impact (H/M/L) | Effort (H/M/L) | Quick Win? |

Use Spring Kafka 3.x APIs, reference Confluent Schema Registry compatibility modes by name, and name DLT topics using the pattern: <original-topic>.DLT.
```

## Expected Output

A multi-section scorecard report containing:
- Event taxonomy scorecard table (8 columns)
- Coupling analysis table
- Schema assessment with a corrected Avro snippet
- Topic design review table
- Consumer resilience scorecard
- Choreography vs orchestration recommendation per flow
- Ranked top-5 improvement table

## Benefits

- Identifies event coupling anti-patterns before they calcify into production incidents.
- Produces a scored, shareable artifact that drives team alignment on schema and retry strategy.
- Provides a concrete improvement backlog ranked by impact/effort for the next sprint planning.

## Related Prompts

- [microservices-decomposition.md](microservices-decomposition.md) — Establish service boundaries before event contracts.
- [../data-streaming/kafka-topic-design.md](../data-streaming/kafka-topic-design.md) — Deep-dive on individual topic design.
- [../observability/observability-design.md](../observability/observability-design.md) — Instrument Kafka consumers for lag alerting.
