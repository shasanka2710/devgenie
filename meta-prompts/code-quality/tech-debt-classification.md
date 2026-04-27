---
name: Tech Debt Classification
domain: code-quality
complexity: L2
output-format: markdown-table
token-estimate: medium
tags: tech-debt, sonarqube, spring-boot, prioritization, backlog, java
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt during quarterly tech debt review sessions, PI planning, or when a SonarQube report reveals a large volume of issues that need triaging. Produces a prioritized, actionable tech debt backlog that engineering managers and principal engineers can use to allocate capacity.

## Prerequisites

- `[SERVICE_NAME]` — Name of the Spring Boot service or monorepo module.
- `[SONARQUBE_ISSUES]` — Paste the SonarQube issues export (CSV or JSON summary), or describe the top issues manually.
- `[SNYK_REPORT]` — Paste the Snyk vulnerability report summary, or "not available."
- `[CODEBASE_AGE_YEARS]` — Age of the codebase in years.
- `[SPRING_BOOT_VERSION]` — Current Spring Boot version.
- `[JAVA_VERSION]` — Current Java version.
- `[TARGET_JAVA_VERSION]` — Target Java version if an upgrade is planned, or "same."
- `[TEAM_VELOCITY_SP]` — Team's average sprint velocity in story points.
- `[BUSINESS_CRITICALITY]` — P1 / P2 / P3 — business criticality of the service.

## The Prompt

```
You are a principal engineer conducting a structured tech debt review for a Spring Boot service.

Context:
- Service: [SERVICE_NAME]
- Codebase age: [CODEBASE_AGE_YEARS] years
- Spring Boot version: [SPRING_BOOT_VERSION] → target: (same unless specified)
- Java version: [JAVA_VERSION] → target: [TARGET_JAVA_VERSION]
- Team velocity: [TEAM_VELOCITY_SP] story points / sprint
- Business criticality: [BUSINESS_CRITICALITY]
- SonarQube issues: [SONARQUBE_ISSUES]
- Snyk vulnerabilities: [SNYK_REPORT]

Task:
Produce a Tech Debt Classification Report with the following sections:

## 1. Tech Debt Taxonomy
Classify all identified issues into the following debt types. Return as a summary table:
| Debt Type | Issue Count | Estimated Remediation Hours | Priority (H/M/L) |
Debt types:
- Code Smells (SonarQube code smells, long methods, duplication)
- Design Debt (poor abstractions, missing interfaces, anemic domain model)
- Dependency Debt (outdated libraries, CVEs from Snyk, Spring Boot version lag)
- Test Debt (missing tests, brittle tests, test anti-patterns)
- Configuration Debt (hardcoded values, missing Vault integration, property sprawl)
- Documentation Debt (missing Javadoc, outdated README, no ADRs)
- Security Debt (CVEs, OWASP issues, insecure defaults)

## 2. Prioritized Tech Debt Backlog
List every distinct debt item as a backlog entry. Sort by Priority descending, then by Remediation Cost ascending (quick wins first within same priority).
| # | Debt Item | Type | Source (SonarQube/Snyk/Manual) | Priority | Story Points | Complexity (L1/L2/L3) | Business Risk if Ignored | Quick Win? |

## 3. Dependency Upgrade Plan
For each outdated dependency identified:
| Dependency | Current Version | Latest Stable | CVEs (Snyk) | Breaking Changes? | Migration Guide URL | Recommended Sprint |

## 4. Spring Boot / Java Version Migration Assessment
If applicable ([SPRING_BOOT_VERSION] or [JAVA_VERSION] is not current):
- List the migration steps numbered
- Flag breaking API changes
- Estimate total migration effort in story points
- Recommend the migration approach: in-place upgrade vs strangler fig vs new service

## 5. Quick Wins (< 4 hours each)
List debt items that can be fixed in under 4 hours, formatted as ready-to-assign tasks:
| Task | File(s) Affected | Fix Description | Expected SonarQube Impact |

## 6. Tech Debt Capacity Recommendation
Given team velocity [TEAM_VELOCITY_SP] sp/sprint, recommend:
- Percentage of capacity to allocate to debt (use the 20% rule as baseline, adjust for criticality)
- Proposed debt burn-down timeline (sprints to reach "manageable" state)
- Top 3 debt items to tackle in the next sprint

## 7. Tech Debt Metrics Dashboard
Recommend Sonarqube Quality Gate thresholds and custom metrics to track debt progress:
| Metric | Current Target | Healthy Target | SonarQube Metric Key |

Output all tables in markdown format. Estimate story points using Fibonacci scale (1, 2, 3, 5, 8, 13, 21).
```

## Expected Output

A structured tech debt report containing:
- Debt taxonomy summary table (7 debt types)
- Full prioritized backlog table (all items, sorted)
- Dependency upgrade plan table
- Spring Boot/Java migration assessment (if applicable)
- Quick wins list (tasks ready to assign)
- Capacity allocation recommendation
- SonarQube metrics dashboard table

## Benefits

- Transforms a raw SonarQube dump into a prioritized, sprint-ready backlog in one pass.
- Provides a data-driven capacity recommendation that principal engineers can present to management.
- Identifies CVE-carrying dependencies that would otherwise be lost in a long issues list.

## Related Prompts

- [spring-boot-code-review.md](spring-boot-code-review.md) — Review new code to prevent debt accumulation.
- [refactoring-strategy.md](refactoring-strategy.md) — Plan the refactoring of high-debt areas.
- [../devops-platform/cicd-pipeline-review.md](../devops-platform/cicd-pipeline-review.md) — Integrate SonarQube/Snyk gates into the Harness pipeline.
