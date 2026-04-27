---
name: CI/CD Pipeline Review
domain: devops-platform
complexity: L2
output-format: scorecard
token-estimate: medium
tags: harness, cicd, pipeline, sonarqube, snyk, ocp, deployment, quality-gates
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt when auditing or redesigning a Harness CI/CD pipeline for a Spring Boot service. Ideal before onboarding a new service to Harness, after a failed deployment caused by pipeline configuration issues, or during a platform engineering review cycle.

## Prerequisites

- `[SERVICE_NAME]` — Name of the Spring Boot service.
- `[PIPELINE_YAML]` — Paste the Harness pipeline YAML, or describe the pipeline stages.
- `[DEPLOYMENT_TARGET]` — OCP namespace and cluster (dev / staging / production).
- `[DEPLOYMENT_STRATEGY]` — Rolling / Blue-Green / Canary.
- `[SONARQUBE_GATE]` — SonarQube Quality Gate configuration (pass/fail criteria), or "not configured."
- `[SNYK_INTEGRATION]` — Whether Snyk is integrated (yes/no) and at which stage.
- `[VAULT_SECRETS]` — How Vault secrets are injected (Harness Vault connector / init container / none).
- `[ROLLBACK_STRATEGY]` — How failed deployments are rolled back.
- `[APPROVALS]` — Manual approval gates configured (stages and approvers), or "none."
- `[ARTIFACT_REGISTRY]` — Container registry used (Quay / Docker Hub / Nexus / AWS ECR).

## The Prompt

```
You are a principal DevOps engineer expert in Harness CI/CD for Spring Boot services on OpenShift Container Platform.

Context:
- Service: [SERVICE_NAME]
- Deployment target: [DEPLOYMENT_TARGET]
- Deployment strategy: [DEPLOYMENT_STRATEGY]
- SonarQube gate: [SONARQUBE_GATE]
- Snyk integration: [SNYK_INTEGRATION]
- Vault secrets injection: [VAULT_SECRETS]
- Rollback strategy: [ROLLBACK_STRATEGY]
- Approval gates: [APPROVALS]
- Artifact registry: [ARTIFACT_REGISTRY]

Pipeline configuration:
[PIPELINE_YAML]

Task:
Produce a CI/CD Pipeline Review with the following sections:

## 1. Pipeline Quality Scorecard
Score the pipeline against each dimension (1–5, 5 = best practice):
| Dimension | Score | Finding | Recommendation |
Dimensions:
- Stage isolation (each stage independently runnable)
- Quality gate enforcement (SonarQube, Snyk — blocking vs advisory)
- Secret management (no hardcoded secrets, Vault integration correct)
- Test execution (unit, integration, contract tests present and failing the pipeline on failure)
- Artifact immutability (same artifact promoted through environments, not rebuilt)
- Rollback automation (automated rollback on health check failure, not just manual)
- Approval gates (appropriate for environment, not bypassed)
- OCP manifest validation (manifest linting before deployment)
- Observability (deployment events sent to monitoring system)

## 2. Stage-by-Stage Analysis
For each stage in the pipeline:
| Stage | Purpose | Issues Found | Severity | Recommendation |

## 3. Quality Gate Configuration Review
For SonarQube [SONARQUBE_GATE]:
| Metric | Current Threshold | Recommended Threshold | Blocking? |
| Coverage on New Code | | ≥ 80% | Yes |
| Duplicated Lines on New Code | | ≤ 3% | Yes |
| Security Rating | | A | Yes |
| Reliability Rating | | A | Yes |
| Maintainability Rating | | A | Advisory |
| Blocker Issues | | 0 | Yes |

For Snyk [SNYK_INTEGRATION]:
| Check | Configured? | Severity Threshold | Blocking? | Recommendation |
| SAST | | | | |
| SCA (dependency) | | | | |
| Container image scan | | | | |
| IaC scan (OCP manifests) | | | | |

## 4. Secret Management Assessment
Evaluate [VAULT_SECRETS] injection approach:
| Secret | Injection Method | Secure? | Recommendation |

Flag any secrets hardcoded in pipeline YAML or environment variables.

## 5. Deployment Strategy Assessment
Evaluate [DEPLOYMENT_STRATEGY] for [DEPLOYMENT_TARGET]:
| Criterion | Score (1-5) | Notes |
| Zero-downtime guarantee | | |
| Rollback speed | | |
| Traffic control granularity | | |
| Istio/OCP Route integration | | |
| Health check before cutover | | |

If Canary, specify the traffic split progression (e.g., 10% → 25% → 50% → 100%) and the automated promotion/rollback criteria.

## 6. Rollback Runbook
Step-by-step rollback procedure for [DEPLOYMENT_STRATEGY] on OCP:
1. Trigger condition (health check threshold, manual abort)
2. Automated rollback action
3. Manual override steps
4. Verification steps
5. Incident notification

## 7. Missing Pipeline Stages
Identify pipeline stages that should exist but are absent:
| Missing Stage | Purpose | Priority (H/M/L) | Harness Step Type |
Common missing stages: DAST, contract test execution, database migration gate, smoke test post-deploy, changelog/release notes generation.

## 8. Priority Recommendations
| Rank | Recommendation | Impact (H/M/L) | Effort (H/M/L) | Harness Feature |

Provide Harness YAML snippets for any recommended stage additions. Reference Harness Delegate 23.x+ APIs.
```

## Expected Output

A structured pipeline review containing:
- 9-dimension quality scorecard
- Stage-by-stage analysis table
- SonarQube and Snyk quality gate tables
- Secret management assessment table
- Deployment strategy scorecard
- Step-by-step rollback runbook
- Missing stage identification table
- Ranked priority recommendations with Harness YAML snippets

## Benefits

- Surfaces quality gate misconfigurations that allow broken code to reach production.
- Identifies double-build anti-patterns that break artifact immutability and reproducibility.
- Produces a concrete rollback runbook before a deployment failure makes one urgently needed.

## Related Prompts

- [ocp-migration-strategy.md](ocp-migration-strategy.md) — Plan the OCP migration the pipeline will deploy to.
- [release-strategy-review.md](release-strategy-review.md) — Align pipeline stages with the release strategy.
- [../security/secrets-management-review.md](../security/secrets-management-review.md) — Deep-dive on Vault secret injection.
