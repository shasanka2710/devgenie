---
name: AI Code Review Integration
domain: ai-llm
complexity: L2
output-format: runbook
token-estimate: medium
tags: ai, code-review, llm, harness, sonarqube, spring-boot, github-actions, ci-cd, automation
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt when embedding AI-assisted code review into a CI/CD pipeline for Spring Boot services. Ideal when scaling code review capacity across many teams, when onboarding junior engineers who need faster feedback loops, or when establishing consistent review standards across microservices.

## Prerequisites

- `[PIPELINE_TOOL]` — CI/CD tool: Harness / GitHub Actions / GitLab CI.
- `[LLM_PROVIDER]` — LLM provider and model (e.g., OpenAI GPT-4o, Azure OpenAI GPT-4, AWS Bedrock Claude 3.5, local Ollama).
- `[REVIEW_SCOPE]` — What the AI should review: security / code quality / Spring Boot idioms / test coverage / all.
- `[CODE_CONTEXT]` — How to provide code context: PR diff only / full file / RAG-augmented with internal style guide.
- `[SONARQUBE_INTEGRATION]` — Whether SonarQube results should be fed into the AI review (yes/no).
- `[OUTPUT_DESTINATION]` — Where AI review comments go: PR comment / Slack / JIRA / all.
- `[APPROVAL_GATE]` — Whether AI review is advisory-only or blocks merge (advisory / blocking).
- `[JAVA_VERSION]` — Java version of the codebase.
- `[SPRING_BOOT_VERSION]` — Spring Boot version.
- `[MAX_DIFF_SIZE_LINES]` — Maximum diff size to review (e.g., 500 lines — larger diffs should be split).

## The Prompt

```
You are a principal engineer designing an AI-assisted code review integration for Spring Boot CI/CD pipelines.

Context:
- CI/CD tool: [PIPELINE_TOOL]
- LLM provider: [LLM_PROVIDER]
- Review scope: [REVIEW_SCOPE]
- Code context strategy: [CODE_CONTEXT]
- SonarQube integration: [SONARQUBE_INTEGRATION]
- Output destination: [OUTPUT_DESTINATION]
- Approval gate: [APPROVAL_GATE]
- Java version: [JAVA_VERSION]
- Spring Boot version: [SPRING_BOOT_VERSION]
- Max diff size: [MAX_DIFF_SIZE_LINES] lines

Task:
Produce an AI Code Review Integration Runbook with the following sections:

## 1. Architecture Overview
Describe the integration flow:
| Step | Component | Action | Output |
| 1. PR opened | [PIPELINE_TOOL] | Trigger AI review job | |
| 2. Diff extraction | Git | Extract PR diff | Patch file |
| 3. Context enrichment | RAG / SonarQube | Add style guide context + SonarQube findings | Augmented prompt |
| 4. LLM review | [LLM_PROVIDER] | Generate review | Structured JSON |
| 5. Post comments | [OUTPUT_DESTINATION] | Post findings as PR comments | Review comments |
| 6. Gate decision | [PIPELINE_TOOL] | Pass/fail based on severity | Pipeline status |

## 2. System Prompt Design for Code Review
Provide the production-quality system prompt for the AI code reviewer, specialized for Java [JAVA_VERSION] / Spring Boot [SPRING_BOOT_VERSION]:

```
[SYSTEM_PROMPT]
```

The system prompt must:
- Define the reviewer persona (Java principal engineer, opinionated, concise)
- Specify [REVIEW_SCOPE] focus areas
- Require structured JSON output with fields: severity (BLOCKER/MAJOR/MINOR/INFO), file, line, message, suggestion, category
- Instruct the LLM to cite Spring Boot version-specific APIs
- Include a "do not comment on" list (formatting, variable naming preferences, personal style)
- Include a hallucination mitigation instruction (only flag issues visible in the provided diff)

## 3. Diff Preprocessing Pipeline
For diffs larger than [MAX_DIFF_SIZE_LINES] lines:
| Step | Action | Tool | Output |
| 1 | Filter diff to changed files only | git diff --unified=3 | Reduced patch |
| 2 | Exclude generated code | File pattern exclusions | Filtered patch |
| 3 | Split large files | By class/method boundary | Chunked patches |
| 4 | Prioritize changed files | By risk (security, data access first) | Ordered review queue |

Provide a shell script (bash) or Java utility class for diff preprocessing.

## 4. [PIPELINE_TOOL] Integration
Provide the complete CI/CD configuration for the AI review step:

If Harness: Harness Shell Script step YAML calling the LLM API
If GitHub Actions: .github/workflows/ai-review.yml action
If GitLab CI: .gitlab-ci.yml job definition

Include:
- LLM API key injection from Vault (not hardcoded)
- Retry logic for LLM API rate limits
- Timeout handling (max 3 minutes per review)
- Structured JSON response parsing

## 5. SonarQube Context Injection
If [SONARQUBE_INTEGRATION] is yes:
- Fetch SonarQube issues for the PR branch using SonarQube API
- Inject top 10 issues as context in the LLM prompt
- Instruct LLM to cross-reference its findings with SonarQube issues
- Avoid duplicating SonarQube findings in AI comments (deduplicate by rule key)

Provide the SonarQube API query and JSON parsing snippet.

## 6. Response Parsing and Comment Formatting
For each LLM finding in the structured JSON output:
- Map severity to PR comment decoration: BLOCKER = ❌, MAJOR = ⚠️, MINOR = 💡, INFO = ℹ️
- Post inline PR comment at the correct file + line
- Add a summary comment at the PR root with: total findings by severity, top 3 issues

Provide the comment formatting code (Java or bash).

## 7. Guardrails and Quality Controls
| Guardrail | Implementation | Rationale |
| False positive rate control | Human override label to dismiss AI comments | Prevents reviewer fatigue |
| Hallucination detection | Require file+line evidence for every finding | No baseless assertions |
| Sensitive data in diff | Redact secrets before sending to LLM | Security |
| LLM cost control | Max tokens per review, model tiering | Cost management |
| Opt-out per PR | Label: skip-ai-review | Developer control |
| Audit log | Log all LLM requests/responses | Compliance |

## 8. Metrics and Continuous Improvement
| Metric | Measurement | Target |
| False positive rate | Human-dismissed findings / total findings | < 20% |
| True positive rate | Accepted findings / total findings | > 60% |
| Review latency | Time from PR open to review posted | < 3 min |
| LLM cost per PR | Tokens used × price | < $0.05 per PR |
| Developer satisfaction | Survey | > 4/5 |

Define a feedback loop: how accepted/dismissed findings are used to improve the system prompt over time.

## 9. Rollout Plan
| Phase | Scope | Gate Type | Duration | Success Criteria |
| Pilot | 2 services, advisory only | Advisory | 2 weeks | False positive < 30% |
| Expand | All services, advisory | Advisory | 4 weeks | Developer satisfaction > 3.5/5 |
| Gate | Critical services, blocking on BLOCKER only | Blocking | — | False positive < 15% |

Reference [LLM_PROVIDER] API. Use Spring Boot [SPRING_BOOT_VERSION] idioms in the system prompt. Flag data privacy concerns for externally-hosted LLM providers.
```

## Expected Output

A complete AI code review integration runbook containing:
- Architecture flow table
- Production system prompt for Java/Spring Boot review
- Diff preprocessing steps + code
- CI/CD configuration YAML (for the chosen pipeline tool)
- SonarQube context injection code
- PR comment formatting code
- Guardrails table
- Metrics and feedback loop table
- Phased rollout plan

## Benefits

- Provides a production-ready AI code review integration, not a toy demo — including guardrails, cost control, and a feedback loop.
- The specialized Spring Boot system prompt produces dramatically better reviews than a generic "review this code" prompt.
- Phased rollout plan reduces team resistance by starting with advisory-only mode.

## Related Prompts

- [rag-pipeline-design.md](rag-pipeline-design.md) — Use RAG to enrich the review with internal coding standards.
- [prompt-engineering-for-code.md](prompt-engineering-for-code.md) — Improve the system prompt quality over time.
- [../code-quality/spring-boot-code-review.md](../code-quality/spring-boot-code-review.md) — The human-driven review this AI integration augments.
