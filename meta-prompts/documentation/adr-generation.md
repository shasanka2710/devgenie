---
name: ADR Generation
domain: documentation
complexity: L1
output-format: adr
token-estimate: low
tags: adr, architecture-decision-record, documentation, spring-boot, design-decision
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt whenever a significant architectural or design decision is made for a Spring Boot service that future engineers need to understand and that would be costly to reverse. Typical triggers: choosing between two frameworks, selecting a database, deciding on a communication pattern, adopting a new platform component.

## Prerequisites

- `[DECISION_TITLE]` — One-line title for the decision (e.g., "Use Kafka instead of REST for order events").
- `[CONTEXT]` — 3–5 sentences describing the situation that forced this decision.
- `[OPTIONS_CONSIDERED]` — Comma-separated list of 2–4 options that were evaluated.
- `[DECISION_MADE]` — The option that was chosen.
- `[CONSEQUENCES]` — Known consequences of the decision (positive and negative).
- `[SERVICE_NAME]` — Service or system this decision applies to.
- `[DECISION_DATE]` — Date the decision was finalized (ISO-8601, e.g., 2025-04-27).
- `[DECISION_MAKERS]` — Names/roles of decision-makers.
- `[STATUS]` — Proposed / Accepted / Deprecated / Superseded.

## The Prompt

```
You are a principal engineer writing a formal Architecture Decision Record (ADR) using the Michael Nygard format.

Context:
- Decision title: [DECISION_TITLE]
- Service/system: [SERVICE_NAME]
- Date: [DECISION_DATE]
- Decision makers: [DECISION_MAKERS]
- Status: [STATUS]
- Situation: [CONTEXT]
- Options evaluated: [OPTIONS_CONSIDERED]
- Chosen option: [DECISION_MADE]
- Known consequences: [CONSEQUENCES]

Task:
Produce a complete ADR document in the following format:

---
# ADR-[NUMBER]: [DECISION_TITLE]

**Date:** [DECISION_DATE]  
**Status:** [STATUS]  
**Deciders:** [DECISION_MAKERS]  
**Service:** [SERVICE_NAME]  

---

## Context

Write 3–5 concise paragraphs that explain:
1. The technical and business forces driving the decision
2. The constraints (technology stack, team skills, timelines, compliance)
3. Why the decision could not be deferred

Be specific to the Java/Spring Boot ecosystem. Reference specific framework versions, library names, and platform components where relevant.

## Decision Drivers

List the weighted decision criteria:
| Criterion | Weight (H/M/L) | Rationale |
| | | |

## Options Considered

For each option in [OPTIONS_CONSIDERED]:

### Option N: [Option Name]
**Description:** 1–2 sentence technical description.

**Pros:**
- ...

**Cons:**
- ...

**Score against each criterion:** (H/M/L per criterion in a mini-table)

## Decision

**Chosen Option: [DECISION_MADE]**

Write 2–3 sentences explaining why this option was chosen over the alternatives, referencing the decision criteria scores.

## Consequences

### Positive Consequences
- ...

### Negative Consequences / Trade-offs
- ...

### Risks and Mitigations
| Risk | Mitigation | Owner |

### Affected Components
List all Spring Boot services, OCP namespaces, Harness pipelines, or other components affected by this decision.

## Implementation Notes
Brief technical notes for the implementing engineer. Include:
- Specific Spring Boot dependencies or auto-configurations required
- Configuration properties to set
- Links to reference implementations or documentation

## Compliance / Security Implications
If the decision has security or compliance implications, list them explicitly.

## Review Date
State when this ADR should be reviewed (suggest 6–12 months or when a triggering event occurs).

---

Write in clear, direct technical English. Avoid marketing language. The ADR should be useful to an engineer reading it 3 years from now with no other context.
```

## Expected Output

A complete ADR document containing:
- Decision metadata header (date, status, deciders)
- Context section (3–5 paragraphs)
- Decision drivers table
- Options considered section (one subsection per option with pros/cons and score table)
- Decision section with justification
- Positive and negative consequences
- Risk/mitigation table
- Implementation notes
- Compliance implications
- Review date

## Benefits

- Produces a complete, standardized ADR in minutes rather than the typical hour-long writing session.
- Forces explicit trade-off scoring, preventing decisions being made on preference rather than criteria.
- Creates a durable record that prevents the same decision from being debated repeatedly by future teams.

## Related Prompts

- [../architecture/microservices-decomposition.md](../architecture/microservices-decomposition.md) — Generate an ADR for decomposition decisions.
- [technical-specification.md](technical-specification.md) — Write the implementation spec after the decision is recorded.
- [../architecture/api-contract-design.md](../architecture/api-contract-design.md) — Record API design decisions as ADRs.
