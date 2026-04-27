---
name: API Contract Design
domain: architecture
complexity: L2
output-format: markdown-table
token-estimate: medium
tags: api, rest, openapi, grpc, asyncapi, spring-boot, contract-first, versioning
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt when designing a new API surface or reviewing an existing one for a Spring Boot service. Particularly valuable before contract finalization between teams, during API-first design workshops, or when auditing an existing API for REST maturity, versioning strategy, and security posture.

## Prerequisites

- `[SERVICE_NAME]` — Name of the Spring Boot service exposing the API.
- `[API_STYLE]` — REST / gRPC / AsyncAPI (or mixed).
- `[CONSUMER_LIST]` — Who consumes this API (internal services, mobile clients, third-party partners).
- `[RESOURCE_LIST]` — Key resources/operations to design (e.g., OrderResource: create, get, cancel).
- `[AUTH_MECHANISM]` — Authentication method (OAuth2 / mTLS / API Key / JWT).
- `[VERSIONING_STRATEGY]` — URL path versioning / header versioning / content negotiation / none.
- `[EXISTING_SPEC_SNIPPET]` — Paste the existing OpenAPI/AsyncAPI YAML snippet, or write "none."
- `[SLA_P99_MS]` — Target p99 latency in milliseconds.

## The Prompt

```
You are a principal engineer specializing in API design for Java Spring Boot microservices.

Context:
- Service: [SERVICE_NAME]
- API style: [API_STYLE]
- Consumers: [CONSUMER_LIST]
- Resources and operations: [RESOURCE_LIST]
- Authentication: [AUTH_MECHANISM]
- Versioning strategy: [VERSIONING_STRATEGY]
- Existing spec: [EXISTING_SPEC_SNIPPET]
- p99 SLA: [SLA_P99_MS] ms

Task:
Produce a structured API Contract Design Review with the following sections:

## 1. API Maturity Assessment
Score the existing (or proposed) API against the Richardson Maturity Model:
| Dimension | Level (0-3) | Evidence | Gap |
Dimensions: Resource Modelling, HTTP Verb Usage, Hypermedia Controls, Idempotency.

## 2. Resource and Operation Design
For each resource/operation pair, evaluate and produce:
| Resource | Operation | HTTP Method | Path | Idempotent? | Request Body Schema | Response Schema | Status Codes | Notes |

## 3. Error Contract
Define the standard error response envelope for [SERVICE_NAME]. Include:
- JSON schema (as a code block)
- Mapping table: | HTTP Status | Error Code | Retryable? | Consumer Action |
- Spring Boot @ControllerAdvice handler skeleton (Java code block, Spring Boot 3.x).

## 4. Versioning Strategy Assessment
Evaluate the chosen versioning strategy [VERSIONING_STRATEGY] against these criteria:
| Criterion | Score (1-5) | Notes |
Criteria: Consumer transparency, Routing simplicity, Backward compatibility enforcement, Spring Boot implementation ease, Gateway/Istio routing complexity.

## 5. Security Contract Review
Review the authentication and authorization design:
| Concern | Current State | Gap | Recommendation |
Concerns: Token validation (JWT claims), Scope/role enforcement per endpoint, Rate limiting, Input validation (Spring Validator / Bean Validation), Sensitive data in URLs, TLS enforcement.

## 6. Contract Testing Strategy
Recommend a contract testing approach using Spring Cloud Contract or Pact:
- Which side owns the contract (producer-driven vs consumer-driven)
- How to integrate contract tests into the Harness CI/CD pipeline
- Stub generation strategy for downstream consumers

## 7. OpenAPI Spec Gaps
List missing or incomplete sections in the existing spec [EXISTING_SPEC_SNIPPET] as a table:
| Section | Missing Element | Priority (H/M/L) | Recommended Addition |

## 8. Top Recommendations
| Rank | Recommendation | Impact | Effort | Owner (Producer/Consumer/Both) |

Use Spring Boot 3.x annotations, reference springdoc-openapi for spec generation, and flag any REST anti-patterns (e.g., verbs in paths, chatty APIs, missing pagination).
```

## Expected Output

A structured review document containing:
- Richardson maturity scorecard (4 rows)
- Resource/operation design table
- Error envelope JSON schema + mapping table + Java handler skeleton
- Versioning strategy assessment table
- Security review table (6 concerns)
- Contract testing recommendation narrative
- OpenAPI spec gaps table
- Ranked recommendation table

## Benefits

- Enforces contract-first discipline before implementation starts, eliminating costly API rework.
- Produces a reusable error contract that all Spring Boot services in the organization can adopt.
- Surfaces security gaps in the API surface before the service reaches production.

## Related Prompts

- [microservices-decomposition.md](microservices-decomposition.md) — Establish service boundaries before API contracts.
- [resilience-pattern-review.md](resilience-pattern-review.md) — Apply resilience patterns to API clients.
- [../security/security-architecture-review.md](../security/security-architecture-review.md) — Deeper threat modelling for the API layer.
