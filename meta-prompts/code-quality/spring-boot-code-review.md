---
name: Spring Boot Code Review
domain: code-quality
complexity: L2
output-format: scorecard
token-estimate: medium
tags: spring-boot, code-review, sonarqube, java, pull-request, best-practices
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt during pull request reviews of Spring Boot services to produce a structured, objective code quality assessment. Ideal for high-stakes PRs (new API endpoints, data pipeline changes, security-sensitive code) or when onboarding a new team to consistent code review standards.

## Prerequisites

- `[SERVICE_NAME]` — Name of the Spring Boot service.
- `[SPRING_BOOT_VERSION]` — Spring Boot version (e.g., 3.2.4).
- `[JAVA_VERSION]` — Java version (e.g., 21).
- `[CODE_DIFF]` — The full diff or key file snippets to review (paste the diff here).
- `[SONARQUBE_REPORT]` — SonarQube findings for the PR (paste summary or key issues), or "not available."
- `[PR_DESCRIPTION]` — The PR description / linked ticket summary.
- `[COMPONENT_TYPE]` — What the changed code represents: REST controller / Service layer / Repository / Kafka consumer / Flink operator / Configuration / Test.

## The Prompt

```
You are a Java principal engineer performing a detailed code review of a Spring Boot [SPRING_BOOT_VERSION] service running on Java [JAVA_VERSION].

Context:
- Service: [SERVICE_NAME]
- Component type: [COMPONENT_TYPE]
- PR description: [PR_DESCRIPTION]
- SonarQube findings: [SONARQUBE_REPORT]

Code to review:
[CODE_DIFF]

Task:
Produce a structured Code Review Report with the following sections:

## 1. Overall Quality Scorecard
Score the submission against each dimension (1–5, 5 = exemplary):
| Dimension | Score | Key Findings |
Dimensions:
- Correctness (logic errors, edge cases, null safety)
- Spring Boot Idioms (correct use of annotations, dependency injection, auto-configuration)
- Exception Handling (@ControllerAdvice, specific exception types, no swallowed exceptions)
- Transaction Management (@Transactional boundaries, propagation, read-only optimization)
- Security (input validation, no sensitive data in logs, no hardcoded secrets, OWASP Top 10)
- Testability (unit test coverage, use of @SpringBootTest vs slice tests, mocking strategy)
- Performance (N+1 queries, blocking calls in reactive context, unnecessary object creation)
- Observability (structured logging with correlation-id, Micrometer metrics, span propagation)

## 2. Critical Issues (Must Fix Before Merge)
List all issues that must be fixed. For each:
| # | File | Line(s) | Issue | Severity (BLOCKER/CRITICAL) | Fix Recommendation | Code Example |

## 3. Major Issues (Should Fix)
| # | File | Line(s) | Issue | Severity (MAJOR) | Fix Recommendation |

## 4. Minor Issues and Style (Optional)
| # | File | Line(s) | Issue | Type (MINOR/INFO) | Suggestion |

## 5. SonarQube Alignment
Cross-reference the code issues found against the SonarQube report [SONARQUBE_REPORT]:
| SonarQube Issue | Confirmed in Review? | Additional Context | Dismiss? |

## 6. Positive Observations
List 2-4 things done well in this PR. Be specific (e.g., "Correct use of @Transactional(readOnly=true) on query methods reduces lock contention").

## 7. Java 21 / Spring Boot 3.x Opportunities
Identify any code patterns that could be improved using modern Java (records, sealed classes, pattern matching, virtual threads) or Spring Boot 3.x features (ProblemDetail, HTTP interface clients, native image hints).
Return as a table:
| Current Pattern | Modern Alternative | Benefit | Effort (H/M/L) |

## 8. Test Coverage Assessment
- Are the right test slice annotations used (@WebMvcTest, @DataMongoTest, @EmbeddedKafka)?
- Is the happy path tested?
- Are error/edge cases tested?
- Is Testcontainers used where appropriate?
Return a pass/fail checklist.

Provide specific line-level fix examples as Java code blocks. Do not repeat the entire diff — reference files and lines precisely.
```

## Expected Output

A structured review report containing:
- 8-dimension quality scorecard
- Blocker/critical issues table with code fix examples
- Major issues table
- Minor/style issues table
- SonarQube alignment cross-reference
- Positive observations (2–4 bullet points)
- Java 21/Spring Boot 3.x modernization opportunity table
- Test coverage pass/fail checklist

## Benefits

- Produces an objective, scored review that removes reviewer subjectivity and personal style debates.
- Surfaces security and transaction issues that are easy to miss in large diffs.
- Identifies quick wins to modernize code to Java 21 and Spring Boot 3.x idioms during review.

## Related Prompts

- [tech-debt-classification.md](tech-debt-classification.md) — Classify recurring review findings as tech debt.
- [refactoring-strategy.md](refactoring-strategy.md) — Plan incremental refactoring for systemic issues.
- [../security/security-architecture-review.md](../security/security-architecture-review.md) — Deeper security analysis for critical services.
