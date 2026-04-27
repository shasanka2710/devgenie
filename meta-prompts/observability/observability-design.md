---
name: Observability Design
domain: observability
complexity: L2
output-format: runbook
token-estimate: medium
tags: observability, micrometer, prometheus, opentelemetry, spring-boot, logging, tracing, alerting
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt when instrumenting a new or existing Spring Boot service for production observability. Ideal during service design, before a major production launch, or when a service is generating too many false-positive alerts or insufficient signal during incidents.

## Prerequisites

- `[SERVICE_NAME]` — Name of the Spring Boot service.
- `[SPRING_BOOT_VERSION]` — Spring Boot version (e.g., 3.2.4).
- `[CURRENT_OBSERVABILITY_STACK]` — Monitoring stack: Prometheus/Grafana / Datadog / Dynatrace / ELK / etc.
- `[TRACING_BACKEND]` — Distributed tracing backend: Jaeger / Zipkin / OpenTelemetry Collector / none.
- `[LOG_AGGREGATION]` — Log aggregation system: EFK / Splunk / Loki / CloudWatch.
- `[EXISTING_METRICS]` — List of metrics currently emitted, or "none."
- `[KEY_OPERATIONS]` — 5 most important business operations (e.g., "order placement, payment processing").
- `[SLO_TARGETS]` — Availability and latency SLO targets, or "not defined."
- `[KAFKA_CONSUMER]` — Whether the service consumes from Kafka (yes/no, topic names).
- `[ISTIO_ENABLED]` — Whether Istio metrics are available (yes/no).

## The Prompt

```
You are a principal engineer designing production observability for a Spring Boot service.

Context:
- Service: [SERVICE_NAME]
- Spring Boot version: [SPRING_BOOT_VERSION]
- Monitoring stack: [CURRENT_OBSERVABILITY_STACK]
- Tracing backend: [TRACING_BACKEND]
- Log aggregation: [LOG_AGGREGATION]
- Existing metrics: [EXISTING_METRICS]
- Key business operations: [KEY_OPERATIONS]
- SLO targets: [SLO_TARGETS]
- Kafka consumer: [KAFKA_CONSUMER]
- Istio enabled: [ISTIO_ENABLED]

Task:
Produce an Observability Design Runbook with the following sections:

## 1. Observability Maturity Assessment
Score current observability maturity (1–5 per pillar):
| Pillar | Current Score | Target Score | Key Gaps |
| Metrics | | | |
| Logs | | | |
| Traces | | | |
| Alerting | | | |
| Dashboards | | | |

## 2. Metrics Instrumentation Plan
For each key operation in [KEY_OPERATIONS], define the required metrics:
| Metric Name | Type (Counter/Gauge/Timer/Distribution Summary) | Labels | Purpose | Alert Needed? |

For Spring Boot, provide the Micrometer code implementation:
- @Timed on @RestController methods
- MeterRegistry injection for custom counters
- DistributionSummary for payload size tracking
- Tags strategy (service name, environment, version)

Provide a Java @Configuration class with MeterRegistry customization as a code block.

Also list all Spring Boot Actuator auto-configured metrics to enable:
| Metric Group | Actuator Property | Enable? |
| JVM Memory | management.metrics.enable.jvm | true |
| Tomcat/Undertow | management.metrics.enable.tomcat | true |
| MongoDB | management.metrics.enable.mongodb | true |
| Kafka Consumer | management.metrics.enable.kafka | true |
| Hikari (if SQL) | management.metrics.enable.hikari | true |

## 3. Structured Logging Design
Define the standard log format for [SERVICE_NAME]:
- Log format: JSON structured (ECS-compatible or custom)
- Required fields: timestamp, level, service, version, traceId, spanId, correlationId, userId (if applicable), message, exception
- Provide logback-spring.xml configuration with:
  - JSON encoder (logstash-logback-encoder)
  - Async appender
  - MDC propagation from Spring Security context
  - Sensitive field masking (PII, secrets)

Provide the logback-spring.xml code block.

Define logging levels policy:
| Level | When to Use | Example |
| ERROR | Unhandled exceptions, integration failures | |
| WARN | Recoverable issues, deprecated API usage | |
| INFO | Business events (order placed, payment completed) | |
| DEBUG | Request/response bodies, query parameters (non-prod only) | |

## 4. Distributed Tracing Configuration
For [TRACING_BACKEND] with Spring Boot Micrometer Tracing (Spring Boot 3.x):
| Configuration | Value | Rationale |
| management.tracing.sampling.probability | 0.1 (prod) / 1.0 (dev) | |
| Propagation format | W3C TraceContext + Baggage | Istio compatible |
| Span enrichment | Custom SpanFilter for business context | |
| Trace ID in response header | X-Trace-Id | |

Provide a Spring Boot @Configuration class for tracing setup.

If [ISTIO_ENABLED] is yes, explain how Envoy-generated spans correlate with application-generated spans.

## 5. Kafka Consumer Observability
If [KAFKA_CONSUMER] is yes:
| Metric | Micrometer Metric Name | Alert Threshold | Dashboard Panel |
| Consumer lag | kafka.consumer.fetch-manager.records-lag | > 1000 | Yes |
| Commit rate | | | |
| Error rate | | > 0.1% | Yes |
| Processing time | | > p99 SLO | Yes |

Provide a KafkaListenerObservation configuration code block for Spring Kafka 3.x.

## 6. Health Endpoint Configuration
Configure Spring Boot Actuator health endpoints:
| Endpoint | Path | Exposed? | Contents |
| Liveness | /actuator/health/liveness | OCP only | Application alive (not dependent on downstream) |
| Readiness | /actuator/health/readiness | OCP only | All dependencies healthy |
| Full health | /actuator/health | Internal only | All components |
| Metrics | /actuator/prometheus | Internal only | Prometheus scrape target |

Provide the application.yml configuration block.

## 7. Alerting Rules
For [CURRENT_OBSERVABILITY_STACK], define the top 10 alerts:
| Alert Name | Metric | Condition | Duration | Severity | Runbook Link |
Must include: error rate spike, p99 latency breach, JVM memory pressure, consumer lag, circuit breaker OPEN state, pod restart loop, disk/CPU saturation.

## 8. Grafana Dashboard Specification
Define the required dashboard panels:
| Panel Title | Metric | Visualization | Purpose |
| Request Rate (RPS) | | Time series | Traffic |
| Error Rate (%) | | Stat + threshold | SLO |
| p50/p95/p99 Latency | | Heatmap | Performance |
| Active Circuit Breakers | | State timeline | Resilience |
| JVM Heap Used | | Gauge | Capacity |
| Consumer Lag | | Time series | Kafka health |

Use Micrometer 1.12+, Spring Boot 3.x Actuator, and OpenTelemetry semantic conventions for metric names.
```

## Expected Output

A complete observability design runbook containing:
- Maturity assessment scorecard (5 pillars)
- Business metric definitions table + Micrometer Java config
- Actuator metric enablement table
- logback-spring.xml configuration
- Logging level policy table
- Distributed tracing configuration + Spring Boot code
- Kafka consumer observability table + code block
- Health endpoint configuration YAML
- Top-10 alerting rules table
- Grafana dashboard panel specification

## Benefits

- Produces a complete, copy-paste observability configuration that a team can implement in a single sprint.
- Enforces correlation ID and trace ID propagation, making incident investigation dramatically faster.
- Defines alert rules before production launch rather than after the first outage.

## Related Prompts

- [incident-rca.md](incident-rca.md) — Use observability data to write a post-incident RCA.
- [slo-definition.md](slo-definition.md) — Define the SLOs that drive alert thresholds.
- [../architecture/resilience-pattern-review.md](../architecture/resilience-pattern-review.md) — Instrument circuit breaker state changes.
