---
name: Kafka Topic Design
domain: data-streaming
complexity: L2
output-format: markdown-table
token-estimate: medium
tags: kafka, topic-design, partitioning, schema-registry, spring-boot, event-streaming, avro
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt when designing new Kafka topics for a business event or reviewing existing topic configurations for correctness, scalability, and operational safety. Ideal before a new event-driven feature is built or as part of a Kafka estate review.

## Prerequisites

- `[TOPIC_PURPOSE]` — Business description of what events this topic carries (e.g., "Order placed by a customer in the e-commerce platform").
- `[PRODUCER_SERVICE]` — Spring Boot service that produces to this topic.
- `[CONSUMER_SERVICES]` — Comma-separated list of consumer services and their use cases.
- `[EXPECTED_TPS]` — Expected events per second at peak.
- `[RETENTION_REQUIREMENT_DAYS]` — How long events must be retained (business / compliance requirement).
- `[ORDERING_REQUIREMENT]` — Level of ordering needed: global / per-entity-id / none.
- `[KAFKA_VERSION]` — Kafka cluster version (e.g., 3.6, KRaft mode yes/no).
- `[SCHEMA_REGISTRY_URL]` — Schema registry URL and type (Confluent / Apicurio), or "none."
- `[SERIALIZATION_FORMAT]` — Avro / Protobuf / JSON.
- `[REPLICATION_FACTOR]` — Number of Kafka brokers available for replication.

## The Prompt

```
You are a principal engineer expert in Apache Kafka topic design and Spring Boot event-driven systems.

Context:
- Topic purpose: [TOPIC_PURPOSE]
- Producer: [PRODUCER_SERVICE]
- Consumers: [CONSUMER_SERVICES]
- Peak TPS: [EXPECTED_TPS]
- Retention requirement: [RETENTION_REQUIREMENT_DAYS] days
- Ordering requirement: [ORDERING_REQUIREMENT]
- Kafka version: [KAFKA_VERSION]
- Schema registry: [SCHEMA_REGISTRY_URL]
- Serialization: [SERIALIZATION_FORMAT]
- Replication factor: [REPLICATION_FACTOR]

Task:
Produce a Kafka Topic Design specification with the following sections:

## 1. Topic Name and Naming Convention
Propose a topic name following the pattern: <domain>.<entity>.<event-verb-past-tense>
| Component | Value | Rationale |
| Domain | | |
| Entity | | |
| Event | | |
| Full topic name | | |
| DLT topic name | | |
| Retry topic names (if needed) | | |

## 2. Partition Design
Calculate and justify partition count:
| Parameter | Value | Calculation / Rationale |
| Target partition count | | (based on [EXPECTED_TPS] / target throughput per partition) |
| Max message size (bytes) | | |
| Partition key | | (entity ID for ordering, or null for throughput) |
| Key selection justification | | |
| Hot partition risk | | |
| Future scaling headroom | | |

## 3. Topic Configuration Table
| Configuration Key | Recommended Value | Rationale |
|---|---|---|
| num.partitions | | |
| replication.factor | | |
| min.insync.replicas | | |
| retention.ms | | |
| retention.bytes | | |
| cleanup.policy | | (delete / compact / compact,delete) |
| compression.type | | (lz4 recommended for throughput) |
| max.message.bytes | | |
| unclean.leader.election.enable | false | Data loss prevention |
| message.timestamp.type | CreateTime | |

Provide the equivalent kafka-topics.sh creation command as a code block.

## 4. Schema Design
Produce a complete [SERIALIZATION_FORMAT] schema for the event with these mandatory envelope fields:
- eventId (UUID)
- correlationId (UUID)
- causationId (UUID)
- schemaVersion (string, semver)
- sourceService (string)
- occurredAt (ISO-8601 UTC timestamp)
- payload (domain-specific fields inferred from [TOPIC_PURPOSE])

Return the full schema as a code block. If Avro, include the namespace.

## 5. Schema Compatibility Strategy
| Concern | Decision | Rationale |
| Compatibility mode | FULL_TRANSITIVE / BACKWARD / FORWARD | |
| Schema registry subject naming | TopicNameStrategy / RecordNameStrategy | |
| Field addition policy | | |
| Field removal policy | | |
| Breaking change process | | |

## 6. Producer Configuration (Spring Boot)
Provide a Spring Boot application.yml snippet and a Java @Configuration class for the producer:
- acks=all
- retries + delivery.timeout.ms
- idempotence enabled
- enable.idempotence=true
- transactional.id if exactly-once is needed

## 7. Consumer Configuration (Spring Boot)
For each consumer in [CONSUMER_SERVICES], provide:
| Consumer Service | Consumer Group ID | Auto Offset Reset | Isolation Level | Concurrency | Error Handler | DLT Strategy |

Provide a Spring Kafka @KafkaListener configuration snippet with DefaultErrorHandler + exponential backoff + DLT publisher.

## 8. Observability and Operational Runbook
| Metric | Alert Threshold | Grafana Panel | Action |
| Consumer lag | > 1000 messages | Yes | Investigate consumer performance |
| Producer error rate | > 0.1% | Yes | Check DLT for failed messages |
| Partition skew | > 20% imbalance | Yes | Review partition key |
| DLT event count | Any | Yes | Investigate and replay |

Reference Kafka 3.x configurations; flag any KRaft-mode specific changes if [KAFKA_VERSION] uses KRaft.
```

## Expected Output

A comprehensive topic design spec containing:
- Topic naming decision table
- Partition design calculation table
- Full topic configuration table with kafka-topics.sh command
- Complete event schema (Avro/Protobuf/JSON) code block
- Schema compatibility strategy table
- Producer Spring Boot YAML + Java config
- Consumer configuration table + Spring Kafka listener code
- Observability and alert table

## Benefits

- Prevents partition key hot spots and under-partitioning before topics are created (costly to fix later).
- Produces a complete, copy-paste Spring Boot producer and consumer configuration.
- Establishes schema compatibility contracts that prevent consumer breakage on schema evolution.

## Related Prompts

- [flink-pipeline-design.md](flink-pipeline-design.md) — Design a Flink pipeline consuming from this topic.
- [../architecture/event-driven-architecture-review.md](../architecture/event-driven-architecture-review.md) — Review the broader event-driven architecture.
- [mongodb-schema-review.md](mongodb-schema-review.md) — Design the MongoDB sink for this event stream.
