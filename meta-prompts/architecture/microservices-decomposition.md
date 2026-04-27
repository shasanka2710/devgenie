---
name: Microservices Decomposition
domain: architecture
complexity: L3
output-format: adr
token-estimate: high
tags: microservices, domain-driven-design, spring-boot, decomposition, bounded-context
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt when you are decomposing a monolithic Java application into microservices, re-evaluating existing service boundaries, or onboarding a new bounded context. It is most valuable before a major planning increment when domain ownership is being established.

## Prerequisites

- `[MONOLITH_NAME]` — Name of the existing application or system being decomposed.
- `[DOMAIN_LIST]` — Comma-separated list of business domains discovered (e.g., Orders, Inventory, Payments).
- `[DATABASE_TECH]` — Current persistence technology (e.g., Oracle 19c, MongoDB 6).
- `[TRANSACTION_BOUNDARIES]` — Description of existing distributed-transaction or saga patterns in use (or "none").
- `[TEAM_COUNT]` — Number of teams that will own services (Conway's Law input).
- `[PEAK_TPS]` — Peak transactions per second the system handles today.
- `[SLA_TARGET]` — Current availability SLA (e.g., 99.9%).
- `[INTEGRATION_STYLE]` — How the monolith integrates externally today (REST, SOAP, Kafka, shared DB).

## The Prompt

```
You are a principal engineer expert in domain-driven design (DDD) and Spring Boot microservices architecture.

Context:
- Application: [MONOLITH_NAME]
- Business domains identified: [DOMAIN_LIST]
- Current database: [DATABASE_TECH]
- Existing transaction boundaries: [TRANSACTION_BOUNDARIES]
- Target team count: [TEAM_COUNT]
- Peak throughput: [PEAK_TPS] TPS
- Availability SLA: [SLA_TARGET]
- Current external integration style: [INTEGRATION_STYLE]

Task:
Produce a microservices decomposition recommendation as a formal Architecture Decision Record (ADR) with the following sections:

## ADR-001: Microservices Decomposition of [MONOLITH_NAME]

### Status
Proposed

### Context
Describe the forces driving decomposition: scaling pressure, team autonomy, deployment frequency goals, and technical constraints from the current stack.

### Bounded Context Map
Return a markdown table with columns:
| Bounded Context | Responsibility | Owning Team | Key Aggregates | Proposed Service Name | Spring Boot Module |

### Decomposition Strategy
Score each of the three decomposition strategies below against the given context (score 1–5, higher = better fit). Return as a scorecard table:
| Strategy | Scalability Fit | Team Autonomy Fit | Data Isolation Feasibility | Migration Risk | Recommended? |
Strategies to score: Strangler Fig, Domain-Partitioned Monorepo (modular monolith first), Big-Bang Rewrite.

### Data Architecture Decisions
For each bounded context, specify:
- Database-per-service recommendation (yes/no and why)
- Event sourcing applicability (yes/no)
- Saga pattern needed (yes/no, choreography or orchestration)

### Inter-Service Communication
Return a table:
| Producer Service | Consumer Service | Interaction Type (sync/async) | Protocol (REST/Kafka) | Kafka Topic Name if async |

### Migration Roadmap
Number the migration phases. Each phase must include:
1. Services to extract
2. Prerequisite refactors in the monolith
3. Feature flags or strangler fig facade needed
4. Rollback criterion

### Risks and Mitigations
Return a table:
| Risk | Likelihood (H/M/L) | Impact (H/M/L) | Mitigation |

### Decision
State the chosen decomposition strategy and the first three services to extract with justification.

### Consequences
List positive and negative consequences of the decision.

Be specific to Spring Boot 3.x, use Resilience4j for resilience patterns, reference Spring Modulith where applicable for the modular monolith option, and name Kafka topics following the pattern: <domain>.<entity>.<event> in past tense.
```

## Expected Output

A complete ADR document containing:
- A bounded context map table (6 columns, one row per domain)
- A scored decomposition strategy scorecard (5 columns, 3 rows)
- A data architecture decision per context
- An inter-service communication table
- A numbered migration roadmap (phases with sub-steps)
- A risk/mitigation table
- A clear decision statement

## Benefits

- Eliminates ambiguity in service boundary debates by producing a scored, traceable decision record.
- Forces team ownership alignment (Conway's Law) before a single line of new code is written.
- Produces a migration roadmap with explicit rollback criteria, reducing big-bang risk.

## Related Prompts

- [event-driven-architecture-review.md](event-driven-architecture-review.md) — Once boundaries are set, design the event contracts.
- [api-contract-design.md](api-contract-design.md) — Design synchronous contracts between the new services.
- [../documentation/adr-generation.md](../documentation/adr-generation.md) — Generic ADR template for other decisions.
