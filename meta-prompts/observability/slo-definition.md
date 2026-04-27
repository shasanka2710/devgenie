---
name: SLO Definition
domain: observability
complexity: L2
output-format: markdown-table
token-estimate: medium
tags: slo, sli, error-budget, observability, prometheus, spring-boot, reliability
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt when defining Service Level Objectives (SLOs) and Service Level Indicators (SLIs) for a Spring Boot service. Ideal when onboarding a service to production monitoring, after an incident reveals an undefined availability target, or during a quarterly reliability review.

## Prerequisites

- `[SERVICE_NAME]` — Name of the Spring Boot service.
- `[KEY_USER_JOURNEYS]` — List of the most important user-facing operations (e.g., "checkout flow, search, account login").
- `[CURRENT_ERROR_RATE_PERCENT]` — Current p28-day error rate, or "unknown."
- `[CURRENT_LATENCY_P99_MS]` — Current p99 latency in milliseconds, or "unknown."
- `[AVAILABILITY_REQUIREMENT]` — Business availability requirement (e.g., "99.9% during business hours").
- `[MONITORING_STACK]` — Prometheus/Grafana / Datadog / Dynatrace.
- `[RELEASE_CADENCE]` — Deployment frequency (affects error budget burn rate).
- `[CONSUMER_TIER]` — Who consumes this service: external customers / internal services / both.
- `[KAFKA_CONSUMER]` — Whether a Kafka consumer SLO is also needed (yes/no).
- `[EXISTING_SLOS]` — Any existing SLO definitions, or "none."

## The Prompt

```
You are a principal reliability engineer defining SLOs for a Spring Boot service in production.

Context:
- Service: [SERVICE_NAME]
- Key user journeys: [KEY_USER_JOURNEYS]
- Current error rate: [CURRENT_ERROR_RATE_PERCENT]%
- Current p99 latency: [CURRENT_LATENCY_P99_MS] ms
- Availability requirement: [AVAILABILITY_REQUIREMENT]
- Monitoring stack: [MONITORING_STACK]
- Release cadence: [RELEASE_CADENCE]
- Consumer tier: [CONSUMER_TIER]
- Kafka consumer SLO needed: [KAFKA_CONSUMER]
- Existing SLOs: [EXISTING_SLOS]

Task:
Produce an SLO Definition document with the following sections:

## 1. SLO Hierarchy
Define the three levels of service level objectives:
| Level | Name | Definition |
| SLA | Service Level Agreement | Customer-facing commitment, legal consequences |
| SLO | Service Level Objective | Internal target, slightly tighter than SLA |
| SLI | Service Level Indicator | The actual metric measured |

## 2. SLI/SLO Definitions
For each key user journey in [KEY_USER_JOURNEYS]:

### [Journey Name]
| Dimension | SLI | Measurement Method | SLO Target | SLA Target (if applicable) |
| Availability | Good requests / total requests | HTTP 5xx rate | | |
| Latency | % requests completing < threshold | p99 of request duration | | |
| Error Rate | Bad requests / total requests | HTTP 4xx + 5xx (excluding known user errors) | | |
| Throughput | Requests/sec sustained | Min acceptable RPS | | |

For latency SLI, specify:
- Latency threshold (ms) — suggest based on [CURRENT_LATENCY_P99_MS]
- Measurement window (rolling 28 days)
- Whether to use histogram_quantile or bucket-based approach in Prometheus

## 3. Error Budget Calculation
For each SLO:
| Journey | SLO | Monthly Budget (minutes of downtime allowed) | Weekly Budget | Daily Budget |
Formula: Monthly budget = (1 - SLO) × 43,800 minutes

| Release cadence impact: Given [RELEASE_CADENCE], how many deployments can consume the budget before it is exhausted? |

## 4. Error Budget Policy
Define the team's error budget policy:
| Budget Remaining | Policy |
| > 50% | Normal velocity — new feature releases allowed |
| 25–50% | Caution — only bug fixes and hardening releases |
| 10–25% | Freeze — no new feature releases, reliability work only |
| < 10% | Red — incident response mode, escalate to leadership |

## 5. Prometheus / Recording Rules
For [MONITORING_STACK] = Prometheus, provide PromQL recording rules and alert rules:

### Recording Rules (prometheus.yml or PrometheusRule CRD)
```yaml
# Availability SLI — rate of successful requests
- record: job:http_request_success_rate:rate5m
  expr: |
    sum(rate(http_server_requests_seconds_count{status!~"5.."}[5m]))
    /
    sum(rate(http_server_requests_seconds_count[5m]))
```

Provide recording rules for:
- 5-minute availability window
- 1-hour availability window
- 28-day availability window
- p99 latency per journey

### Alert Rules
| Alert Name | Condition | Burn Rate | Severity | Action |
| SLOBudgetCritical | 1h burn rate > 14.4 | Fast burn | CRITICAL | Page on-call |
| SLOBudgetWarning | 6h burn rate > 6 | Slow burn | WARNING | Notify team |
| SLOBudgetDepleted | 28d budget < 10% | — | CRITICAL | Reliability freeze |

## 6. Kafka Consumer SLO (if applicable)
If [KAFKA_CONSUMER] is yes:
| SLI | Metric | SLO | Measurement |
| Consumer Lag | kafka_consumer_fetch_manager_records_lag | < 1000 messages p99 | 28-day rolling |
| Processing Success Rate | Successfully processed / total consumed | 99.9% | 28-day rolling |
| End-to-End Latency | Time from produce to processed | < [LATENCY_SLA_MS] ms p99 | 28-day rolling |

## 7. SLO Dashboard Specification
For [MONITORING_STACK]:
| Panel | Metric | Visualization | Purpose |
| Current SLO Status | 28d availability | Stat (green/amber/red) | At-a-glance |
| Error Budget Remaining | Budget % | Gauge | Runway |
| Burn Rate | 1h and 6h burn rate | Time series | Alerting context |
| Latency Histogram | p50/p95/p99 | Heatmap | Distribution |
| Error Budget History | Per release | Bar chart | Deployment impact |

## 8. SLO Review Cadence
| Review Type | Frequency | Participants | Outputs |
| Weekly SLO check | Weekly | SRE + team lead | Budget status, incidents review |
| Monthly SLO review | Monthly | Principal + product | SLO accuracy, budget adjustments |
| Quarterly SLO adjustment | Quarterly | Leadership + eng | SLO target changes, SLA negotiation |

Use Google SRE SLO methodology. Reference Prometheus Operator PrometheusRule CRD format. Apply multi-window, multi-burn-rate alerting (fast-burn + slow-burn alerts).
```

## Expected Output

A complete SLO definition document containing:
- SLO hierarchy explanation table
- SLI/SLO definition tables per user journey (4 dimensions each)
- Error budget calculation table
- Error budget policy table (4 thresholds)
- Prometheus recording rules and alert rules (YAML code blocks)
- Kafka consumer SLO table (if applicable)
- Dashboard panel specification table
- SLO review cadence table

## Benefits

- Produces complete, deployable Prometheus recording rules and alert rules in one pass.
- Error budget policy table gives the team a clear, pre-agreed process for reliability vs velocity trade-offs.
- Multi-burn-rate alerting rules prevent both pager fatigue (too many false alarms) and missed incidents.

## Related Prompts

- [observability-design.md](observability-design.md) — Instrument the service to emit the SLI metrics.
- [incident-rca.md](incident-rca.md) — Document SLO breaches as structured RCAs.
- [../devops-platform/release-strategy-review.md](../devops-platform/release-strategy-review.md) — Use error budget burn rate to gate releases.
