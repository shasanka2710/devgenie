---
name: Flink Pipeline Design
domain: data-streaming
complexity: L3
output-format: runbook
token-estimate: high
tags: flink, stream-processing, apache-flink, stateful, kafka, watermarks, checkpointing, java
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt when designing a new Apache Flink stateful stream processing pipeline that consumes from Kafka and writes to MongoDB or another sink. Ideal at the start of a new Flink job design, during pipeline architecture review, or when diagnosing performance and correctness issues in an existing Flink job.

## Prerequisites

- `[PIPELINE_NAME]` — Name of the Flink pipeline / job.
- `[BUSINESS_REQUIREMENT]` — What business problem this pipeline solves (e.g., real-time fraud detection, order aggregation, session windowing for analytics).
- `[INPUT_KAFKA_TOPICS]` — Source Kafka topics (name, schema, TPS).
- `[OUTPUT_SINK]` — Sink type and target (e.g., MongoDB collection, Kafka topic, Elasticsearch index).
- `[PROCESSING_SEMANTICS]` — Required semantics: at-least-once / exactly-once.
- `[LATENCY_SLA_MS]` — End-to-end processing latency SLA in milliseconds.
- `[STATE_BACKEND]` — Flink state backend: RocksDB / HashMap.
- `[FLINK_VERSION]` — Apache Flink version (e.g., 1.18).
- `[JAVA_VERSION]` — Java version (e.g., 21).
- `[CHECKPOINT_INTERVAL_MS]` — Desired checkpoint interval in milliseconds.
- `[PARALLELISM]` — Desired Flink job parallelism.
- `[OCP_DEPLOYMENT]` — Whether deployed on OCP (yes/no) and resource constraints (CPU/memory requests/limits).

## The Prompt

```
You are a principal engineer expert in Apache Flink stream processing on Java.

Context:
- Pipeline: [PIPELINE_NAME]
- Business requirement: [BUSINESS_REQUIREMENT]
- Input Kafka topics: [INPUT_KAFKA_TOPICS]
- Output sink: [OUTPUT_SINK]
- Processing semantics: [PROCESSING_SEMANTICS]
- Latency SLA: [LATENCY_SLA_MS] ms end-to-end
- State backend: [STATE_BACKEND]
- Flink version: [FLINK_VERSION]
- Java version: [JAVA_VERSION]
- Checkpoint interval: [CHECKPOINT_INTERVAL_MS] ms
- Parallelism: [PARALLELISM]
- OCP deployment: [OCP_DEPLOYMENT]

Task:
Produce a Flink Pipeline Design Runbook with the following sections:

## 1. Pipeline Architecture Overview
Describe the data flow in a structured format:
| Stage | Component | Type | Input | Output | Parallelism | State? |
Stages: Source → Filter/Validation → Enrichment → Window/Aggregation → Sink

## 2. Source Configuration
Provide the FlinkKafkaConsumer / KafkaSource (Flink 1.15+ API) configuration:
- Java code block with KafkaSource.builder() configuration
- Deserialization schema (Avro / JSON / Protobuf)
- Starting offset strategy
- Consumer group assignment
- Watermark strategy (event time vs processing time, watermark delay based on [LATENCY_SLA_MS])

## 3. Windowing Strategy
For the aggregation requirement in [BUSINESS_REQUIREMENT]:
| Window Type | Window Size | Slide/Gap | Allowed Lateness | Late Data Strategy | Trigger |
Window types to consider: Tumbling / Sliding / Session / Global.

Provide a Java code block for the recommended windowing implementation using Flink's DataStream API.

## 4. State Design
| State Name | State Type (ValueState/MapState/ListState) | TTL | State Backend Rationale | Serializer |

Provide a Java code block showing state descriptor declaration and usage within a KeyedProcessFunction.

## 5. Exactly-Once / At-Least-Once Configuration
If [PROCESSING_SEMANTICS] is exactly-once:
- Kafka source: isolation.level=read_committed
- Kafka sink: transactional producer configuration
- Checkpoint barrier alignment vs unaligned checkpoints trade-off
- Two-phase commit sink setup for [OUTPUT_SINK]
Return as a numbered configuration checklist with code snippets.

## 6. Checkpointing and State Backend Configuration
Provide flink-conf.yaml (or programmatic StreamExecutionEnvironment) configuration:
| Parameter | Recommended Value | Rationale |
| execution.checkpointing.interval | [CHECKPOINT_INTERVAL_MS] ms | |
| execution.checkpointing.mode | EXACTLY_ONCE / AT_LEAST_ONCE | |
| execution.checkpointing.timeout | | |
| state.backend | [STATE_BACKEND] | |
| state.checkpoints.dir | | |
| state.savepoints.dir | | |
| execution.checkpointing.max-concurrent-checkpoints | 1 | |
| taskmanager.memory.managed.fraction | | |

## 7. Sink Configuration
For [OUTPUT_SINK], provide:
- Java code block for the sink implementation (Flink SinkV2 API)
- Idempotency key design (for MongoDB: upsert key field)
- Sink parallelism recommendation
- Backpressure handling strategy

## 8. Watermark and Late Data Strategy
- Explain the watermark delay chosen relative to [LATENCY_SLA_MS]
- Describe allowed lateness configuration
- Describe side output for truly late events
- Provide Java code block for BoundedOutOfOrdernessWatermarks

## 9. OCP Deployment Configuration
If [OCP_DEPLOYMENT] is yes:
- Flink Kubernetes Operator CRD YAML (FlinkDeployment) with resource requests/limits
- TaskManager and JobManager resource sizing formula
- Checkpoint storage (PVC or S3-compatible object store)
- Health check and liveness probe configuration

## 10. Observability
| Metric | Flink Metric Name | Alert Threshold | Action |
| Checkpoint duration | | > 80% of [CHECKPOINT_INTERVAL_MS] | Tune state size |
| Checkpoint failures | | Any | Immediate alert |
| Consumer lag | | > [LATENCY_SLA_MS] ms | Scale up parallelism |
| Backpressure | isBackPressured | true | Investigate bottleneck |
| Late events | numLateRecordsDropped | > 0 | Review watermark strategy |

## 11. Failure Scenarios and Recovery Runbook
| Failure | Detection | Recovery Steps | Data Loss Risk |
| Job crash | JM health check fails | Restart from last checkpoint | None if checkpoint succeeded |
| Kafka broker failure | Consumer lag spike | Automatic — Kafka client reconnects | None |
| State backend corruption | Checkpoint failure | Restore from previous savepoint | Possible for in-flight |
| OCP pod eviction | Pod restart | Job resubmit via Flink Operator | None if checkpoint succeeded |

Use Flink [FLINK_VERSION] DataStream API. Use Java [JAVA_VERSION] features. Reference Flink Kubernetes Operator 1.7+ for OCP deployment.
```

## Expected Output

A complete Flink pipeline design runbook containing:
- Pipeline stage architecture table
- Source configuration Java code block
- Windowing strategy table + Java code block
- State design table + Java code block
- Exactly-once configuration checklist
- Checkpointing parameter table
- Sink implementation code block
- Watermark/late data code block
- OCP FlinkDeployment YAML (if applicable)
- Observability metric table
- Failure recovery runbook table

## Benefits

- Produces a complete, runnable Flink pipeline design with Java code that junior engineers can implement directly.
- Forces explicit decisions on watermarking, state TTL, and late data — the three most common sources of Flink correctness bugs.
- Includes OCP-specific deployment configuration, avoiding trial-and-error with the Flink Kubernetes Operator.

## Related Prompts

- [kafka-topic-design.md](kafka-topic-design.md) — Design the source Kafka topics for this pipeline.
- [mongodb-schema-review.md](mongodb-schema-review.md) — Design the MongoDB sink schema.
- [../observability/observability-design.md](../observability/observability-design.md) — Instrument the Flink job for production monitoring.
