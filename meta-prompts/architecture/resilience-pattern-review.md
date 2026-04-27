---
name: Resilience Pattern Review
domain: architecture
complexity: L2
output-format: scorecard
token-estimate: medium
tags: resilience, resilience4j, circuit-breaker, retry, bulkhead, spring-boot, istio, fault-tolerance
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt when hardening a Spring Boot microservice or service mesh against cascading failures, dependency timeouts, and traffic spikes. Valuable before a high-stakes production launch, after a production outage caused by a downstream dependency failure, or during a quarterly resilience review.

## Prerequisites

- `[SERVICE_NAME]` — The Spring Boot service being hardened.
- `[DEPENDENCY_LIST]` — Comma-separated list of downstream dependencies (services, databases, Kafka, external APIs).
- `[RESILIENCE4J_VERSION]` — Version of Resilience4j in use (e.g., 2.1.0).
- `[ISTIO_ENABLED]` — Whether Istio service mesh is active (yes/no).
- `[CURRENT_TIMEOUT_CONFIG]` — Existing timeout values per dependency, or "not configured."
- `[INCIDENT_HISTORY]` — Brief description of past outages caused by dependency failures, or "none."
- `[PEAK_TPS]` — Peak TPS the service handles.
- `[MAX_THREAD_POOL_SIZE]` — Current thread pool / virtual thread configuration.

## The Prompt

```
You are a principal engineer expert in resilience engineering for Java Spring Boot microservices using Resilience4j and Istio.

Context:
- Service: [SERVICE_NAME]
- Downstream dependencies: [DEPENDENCY_LIST]
- Resilience4j version: [RESILIENCE4J_VERSION]
- Istio service mesh: [ISTIO_ENABLED]
- Current timeout config: [CURRENT_TIMEOUT_CONFIG]
- Past incidents: [INCIDENT_HISTORY]
- Peak TPS: [PEAK_TPS]
- Max thread pool size: [MAX_THREAD_POOL_SIZE]

Task:
Produce a Resilience Pattern Review scorecard for [SERVICE_NAME] with the following sections:

## 1. Dependency Risk Map
For each dependency in [DEPENDENCY_LIST], assess:
| Dependency | Type (sync/async) | Criticality (P1/P2/P3) | Current Timeout | Circuit Breaker? | Retry Config | Bulkhead? | Risk Score (1-5) |

## 2. Resilience4j Configuration Scorecard
Score the current Resilience4j configuration against best practices:
| Pattern | Configured? | Config Values | Gap | Recommended Config | Score (1-5) |
Patterns to assess:
- CircuitBreaker (sliding window, failure rate threshold, slow call threshold, wait duration in OPEN state)
- Retry (max attempts, wait duration, exponential backoff, jitter, retryable exceptions)
- TimeLimiter (timeout duration per dependency)
- Bulkhead (max concurrent calls, max wait duration — ThreadPoolBulkhead vs SemaphoreBulkhead)
- RateLimiter (limit-for-period, limit-refresh-period, timeout-duration)

## 3. Istio-Level Resilience Assessment
If Istio is enabled [ISTIO_ENABLED], evaluate:
| Istio Feature | Configured? | Config Snippet | Gap | Recommendation |
Features: Outlier Detection, Circuit Breaking (DestinationRule), Retry Policy (VirtualService), Timeout (VirtualService), Traffic Splitting for canary.

Flag any double-retry anti-pattern (Resilience4j retry + Istio retry on the same call path) and recommend resolution.

## 4. Fallback Strategy Review
For each P1 dependency, specify:
| Dependency | Fallback Strategy | Fallback Implementation (Spring @Fallback / manual) | Tested in Chaos? |
Fallback options: cached response, default value, degraded-mode response, fail-fast with user-friendly error.

## 5. Cascading Failure Blast Radius
Simulate the failure of each P1 dependency and describe the blast radius:
| Dependency Failure | Directly Affected Endpoints | Cascading Services | User Impact | Current Containment | Gap |

## 6. Thread/Virtual Thread Configuration Review
Assess whether the current thread pool configuration [MAX_THREAD_POOL_SIZE] is compatible with:
- Bulkhead thread pool sizing (rule: isolate each P1 dependency thread pool)
- Java 21 virtual threads (Spring Boot 3.2+ spring.threads.virtual.enabled=true) compatibility with Resilience4j
Return recommendations as a numbered list.

## 7. Chaos Engineering Readiness
Rate readiness to run chaos experiments (1–5) and list three specific chaos scenarios to run first:
| Scenario | Tool (Chaos Monkey / Istio Fault Injection) | Expected Outcome | Pass Criteria |

## 8. Priority Fixes
| Rank | Fix | Pattern | Impact (H/M/L) | Effort (H/M/L) |

Provide concrete Resilience4j YAML configuration snippets (application.yml format) for all recommended changes. Reference Spring Boot 3.x and Resilience4j 2.x APIs.
```

## Expected Output

A multi-section scorecard report containing:
- Dependency risk map table (8 columns, one row per dependency)
- Resilience4j pattern scorecard (5 patterns scored)
- Istio feature assessment table (if applicable)
- Fallback strategy table per P1 dependency
- Cascading failure blast radius table
- Thread/virtual thread configuration recommendations
- Chaos scenario table
- Ranked priority fixes

## Benefits

- Surfaces hidden single points of failure in dependency chains before they cause production outages.
- Provides concrete, copy-paste Resilience4j YAML configuration, eliminating guesswork.
- Identifies double-retry anti-patterns introduced by combining Resilience4j and Istio policies.

## Related Prompts

- [event-driven-architecture-review.md](event-driven-architecture-review.md) — Apply resilience to Kafka consumer pipelines.
- [../observability/observability-design.md](../observability/observability-design.md) — Instrument circuit breaker state changes for alerting.
- [../observability/incident-rca.md](../observability/incident-rca.md) — Document past resilience failures.
