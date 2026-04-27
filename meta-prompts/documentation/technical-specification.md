---
name: Technical Specification
domain: documentation
complexity: L2
output-format: narrative
token-estimate: medium
tags: technical-spec, design-doc, spring-boot, feature, implementation, api, documentation
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt to write a detailed technical specification for a new feature or significant change to a Spring Boot service. Ideal before implementation begins (to align the team), during a design review, or when a feature is complex enough that multiple engineers need to understand the design simultaneously.

## Prerequisites

- `[FEATURE_NAME]` — Name of the feature or change.
- `[BUSINESS_REQUIREMENT]` — 2–4 sentence description of the business problem being solved.
- `[SERVICE_NAME]` — Primary Spring Boot service involved.
- `[RELATED_SERVICES]` — Other services or systems affected.
- `[TECHNICAL_APPROACH]` — Brief description of the proposed technical approach, or "to be determined."
- `[API_CHANGES]` — New or changed API endpoints (method, path, request/response), or "none."
- `[DATA_CHANGES]` — Database schema changes (MongoDB/SQL), or "none."
- `[EVENT_CHANGES]` — New or changed Kafka events, or "none."
- `[NON_FUNCTIONAL_REQUIREMENTS]` — Performance, availability, security requirements (or "standard").
- `[CONSTRAINTS]` — Technical or business constraints (deadlines, technology restrictions).
- `[AUTHOR]` — Name and role of the spec author.
- `[REVIEW_DEADLINE]` — Date by which the spec needs to be reviewed.

## The Prompt

```
You are a principal engineer writing a detailed technical specification for a new feature.

Context:
- Feature: [FEATURE_NAME]
- Business requirement: [BUSINESS_REQUIREMENT]
- Primary service: [SERVICE_NAME]
- Related services: [RELATED_SERVICES]
- Proposed technical approach: [TECHNICAL_APPROACH]
- API changes: [API_CHANGES]
- Data changes: [DATA_CHANGES]
- Event changes: [EVENT_CHANGES]
- Non-functional requirements: [NON_FUNCTIONAL_REQUIREMENTS]
- Constraints: [CONSTRAINTS]
- Author: [AUTHOR]
- Review deadline: [REVIEW_DEADLINE]

Task:
Produce a complete Technical Specification document with the following sections:

---

# Technical Specification: [FEATURE_NAME]

**Author:** [AUTHOR]  
**Service:** [SERVICE_NAME]  
**Status:** Draft  
**Review Deadline:** [REVIEW_DEADLINE]  
**Last Updated:** (today's date)

---

## Table of Contents
1. Overview
2. Goals and Non-Goals
3. Background and Context
4. Technical Design
5. API Contract
6. Data Model Changes
7. Event Contract Changes
8. Non-Functional Requirements
9. Security Considerations
10. Testing Strategy
11. Rollout Plan
12. Open Questions
13. Appendix

---

## 1. Overview
Write a 3–5 sentence plain-English summary of the feature, its purpose, and how it fits into the system.

## 2. Goals and Non-Goals
| Goals | Non-Goals |
| What this spec will deliver | What is explicitly out of scope |

## 3. Background and Context
Explain the technical context: existing system behaviour, why the current approach is insufficient, and any prior art (linked ADRs, previous attempts).

## 4. Technical Design
Describe the implementation design:

### 4.1 System Interaction Diagram
Describe the component interactions in text (producers, consumers, APIs, data stores). If possible, provide a Mermaid sequence diagram in a code block.

### 4.2 Spring Boot Implementation Design
| Component | Class / Interface | Responsibility | Dependencies |
| Controller | | | |
| Service | | | |
| Repository | | | |
| Event Producer | | | |
| Event Consumer | | | |

For each new class, provide the Java interface signature (not full implementation) with:
- Spring stereotype annotations (@Service, @RestController, @Component)
- Constructor injection of dependencies
- Method signatures with parameter and return types

### 4.3 Configuration Properties
| Property | Type | Default | Description | Environment-specific? |

Provide the application.yml snippet for new properties.

### 4.4 Feature Flag Design
If this feature requires a feature flag:
- Flag name, type (release/ops/experiment)
- Default state (on/off)
- Removal criteria

## 5. API Contract
For each new or changed endpoint in [API_CHANGES]:
| Method | Path | Request Body | Response | Status Codes | Auth Required |

Provide the OpenAPI YAML snippet for new endpoints.

## 6. Data Model Changes
For [DATA_CHANGES]:
- MongoDB: provide the new document structure as a JSON example
- SQL: provide the Flyway/Liquibase migration script
- Backward compatibility analysis (will existing documents/rows still work?)

## 7. Event Contract Changes
For [EVENT_CHANGES]:
- Provide the full Avro/JSON schema for new events
- Schema compatibility analysis (BACKWARD/FORWARD/FULL)
- Consumer impact analysis (which consumers need to update?)

## 8. Non-Functional Requirements
| NFR | Requirement | How Achieved | Measurement |
| Latency | [NON_FUNCTIONAL_REQUIREMENTS] | | |
| Throughput | | | |
| Availability | | | |
| Scalability | | | |
| Security | | | |

## 9. Security Considerations
| Concern | Design Decision | Spring Security Config |
| Authentication | | |
| Authorization | | |
| Input validation | | |
| Data classification | | |
| Sensitive data handling | | |

## 10. Testing Strategy
| Test Type | Scope | Tools | Coverage Target |
| Unit tests | Business logic | JUnit 5, Mockito | 80% new code |
| Integration tests | Spring context | @SpringBootTest, Testcontainers | Happy path + error cases |
| Contract tests | API consumers | Spring Cloud Contract / Pact | All consumer contracts |
| Performance tests | Key endpoints | Gatling / k6 | p99 < SLA threshold |
| Security tests | Auth/authz | Spring Security test | All secure endpoints |

## 11. Rollout Plan
| Phase | Description | Duration | Rollback Trigger |
| Feature flag off | Deploy to prod, flag disabled | 1 sprint | Any test failure |
| Canary (10%) | Enable flag for 10% of traffic | 24h | Error rate > 0.5% |
| Full rollout | Enable flag for 100% | — | — |
| Flag removal | Remove feature flag from code | Next sprint | — |

## 12. Open Questions
| # | Question | Owner | Due Date | Resolution |

## 13. Appendix
Links to: related ADRs, JIRA epic, API documentation, runbooks, design meeting notes.

---

Write in clear technical English. Include enough detail that an engineer can implement without needing further clarification, while also being readable by technical product managers.
```

## Expected Output

A complete technical specification document containing:
- Metadata header (author, status, deadline)
- Goals and non-goals table
- Background and context narrative
- System interaction description + Mermaid diagram
- Component design table with Java interface signatures
- Configuration properties table + YAML snippet
- API contract table + OpenAPI YAML
- Data model changes + migration scripts
- Event contract changes + schema
- NFR table with measurement criteria
- Security considerations table
- Testing strategy table
- Rollout plan table
- Open questions tracker

## Benefits

- Aligns entire team on the design before implementation begins, catching issues when they are cheap to fix.
- Produces a testing strategy that prevents gaps (missing contract tests, no performance tests).
- The rollout plan with feature flags enables safe production validation without big-bang releases.

## Related Prompts

- [adr-generation.md](adr-generation.md) — Record key decisions made during spec review as ADRs.
- [release-notes-generation.md](release-notes-generation.md) — Generate release notes once the feature is shipped.
- [../architecture/api-contract-design.md](../architecture/api-contract-design.md) — Deep-dive on the API contract section.
