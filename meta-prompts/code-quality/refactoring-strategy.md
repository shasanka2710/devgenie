---
name: Refactoring Strategy
domain: code-quality
complexity: L3
output-format: runbook
token-estimate: high
tags: refactoring, spring-boot, java, legacy, strangler-fig, incremental, tech-debt
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt when planning an incremental refactoring of a legacy or high-debt Spring Boot service. Ideal when the team needs a safe, step-by-step refactoring plan that maintains production stability, can be delivered across multiple sprints, and is reviewable by stakeholders without deep technical knowledge.

## Prerequisites

- `[SERVICE_NAME]` — Name of the service to refactor.
- `[REFACTORING_GOAL]` — What the refactoring aims to achieve (e.g., extract domain logic from controllers, migrate from field injection to constructor injection, decompose God Service, remove JPA for MongoDB migration).
- `[CURRENT_ARCHITECTURE_DESCRIPTION]` — 3–5 sentence description of the current architecture and its pain points.
- `[KEY_CLASSES]` — List of key classes / packages involved (class names and package paths).
- `[TEST_COVERAGE_PERCENT]` — Current unit/integration test coverage percentage.
- `[HAS_INTEGRATION_TESTS]` — Whether integration or contract tests exist (yes/no).
- `[SPRING_BOOT_VERSION]` — Current Spring Boot version.
- `[JAVA_VERSION]` — Current Java version.
- `[DEPLOYMENT_FREQUENCY]` — How often the service is deployed today (e.g., daily, weekly).
- `[ROLLBACK_STRATEGY]` — Current rollback approach (feature flags / blue-green / revert deploy / none).

## The Prompt

```
You are a principal engineer planning a safe, incremental refactoring strategy for a legacy Spring Boot service.

Context:
- Service: [SERVICE_NAME]
- Refactoring goal: [REFACTORING_GOAL]
- Current architecture: [CURRENT_ARCHITECTURE_DESCRIPTION]
- Key classes/packages: [KEY_CLASSES]
- Test coverage: [TEST_COVERAGE_PERCENT]%
- Integration tests exist: [HAS_INTEGRATION_TESTS]
- Spring Boot version: [SPRING_BOOT_VERSION], Java: [JAVA_VERSION]
- Deployment frequency: [DEPLOYMENT_FREQUENCY]
- Rollback strategy: [ROLLBACK_STRATEGY]

Task:
Produce a Refactoring Strategy Runbook with the following sections:

## 1. Refactoring Feasibility Assessment
Before planning, assess feasibility:
| Factor | Current State | Risk Level (H/M/L) | Mitigation Required |
Factors:
- Test coverage adequacy (< 40% = high risk)
- Presence of integration/contract tests
- Deployment pipeline rollback capability
- Feature flag infrastructure availability
- Team familiarity with target patterns
- External dependencies on the class surface being changed

## 2. Safety Net: Testing Prerequisites
List the tests that MUST exist before refactoring begins:
1. Characterization tests needed (describe which classes and behaviours)
2. Integration test gaps to fill (which endpoints/flows)
3. Contract tests to add (which consumers)
4. Recommended Testcontainers setup if not present

For each, provide a Spring Boot test skeleton (Java code block).

## 3. Refactoring Strategy Selection
Score each applicable refactoring strategy for this context (1–5):
| Strategy | Applicability | Risk | Delivery Speed | Reversibility | Recommended? |
Strategies:
- Strangler Fig (route by route)
- Branch by Abstraction
- Parallel Change (expand-migrate-contract)
- Extract and Inline (for small focused refactors)
- Big-Bang (only for low-risk, well-tested code)

## 4. Step-by-Step Refactoring Runbook
Number every step. Each step must:
- Be independently deployable (no half-done state in production)
- Include a "done" definition
- Include a rollback instruction if this step fails
- Specify which classes/methods are touched
- Be completable within one sprint at most

Format:
### Step N: [Step Name]
**Goal:** ...
**Classes affected:** ...
**Actions:**
  1. ...
  2. ...
**Done when:** ...
**Rollback:** ...
**Code example:** (Java code block if applicable)

## 5. Feature Flag Integration
If the refactoring changes observable behaviour:
- Specify which feature flags are needed
- Provide Spring Boot feature flag implementation pattern (using a @ConditionalOnProperty or Unleash/LaunchDarkly integration)
- Define the flag lifecycle: when to enable for canary, full rollout, and flag removal

## 6. Continuous Integration Gates
For each step, specify the CI/CD gate (Harness pipeline stage) that must pass before proceeding:
| Step | Required Gate | SonarQube Threshold | Test Coverage Delta | Rollback Trigger |

## 7. Risk Register
| Risk | Trigger | Probability (H/M/L) | Impact (H/M/L) | Contingency |

## 8. Delivery Timeline
Given [DEPLOYMENT_FREQUENCY] deployment cadence, estimate:
| Sprint | Steps to Complete | Story Points | Dependencies | Exit Criteria |

Use Java 21 and Spring Boot 3.x idioms for all code examples. Prefer constructor injection, records for DTOs, and sealed interfaces for discriminated types.
```

## Expected Output

A complete refactoring runbook containing:
- Feasibility assessment table
- Testing prerequisites with code skeletons
- Strategy selection scorecard
- Numbered step-by-step runbook (each step independently deployable)
- Feature flag implementation pattern
- CI/CD gate table
- Risk register
- Sprint-by-sprint delivery timeline

## Benefits

- Ensures refactoring is safe-by-default: no step leaves the codebase in a broken or undeployable state.
- Provides a stakeholder-readable delivery timeline without requiring technical knowledge.
- Integrates CI/CD gates into the plan, preventing delivery without quality validation.

## Related Prompts

- [spring-boot-code-review.md](spring-boot-code-review.md) — Review each refactoring step as a PR.
- [tech-debt-classification.md](tech-debt-classification.md) — Identify and prioritize what to refactor.
- [../devops-platform/release-strategy-review.md](../devops-platform/release-strategy-review.md) — Coordinate refactoring releases with the release strategy.
