---
name: Release Strategy Review
domain: devops-platform
complexity: L2
output-format: markdown-table
token-estimate: medium
tags: release, deployment, blue-green, canary, feature-flags, harness, ocp, rollback
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt when reviewing or redesigning the release strategy for a Spring Boot service on OCP with Harness CI/CD. Ideal before a major feature release, after a failed deployment, when adopting a new deployment pattern (canary, blue-green), or during a platform engineering review.

## Prerequisites

- `[SERVICE_NAME]` — Name of the Spring Boot service.
- `[CURRENT_DEPLOYMENT_STRATEGY]` — Current strategy: Rolling / Blue-Green / Canary / In-place.
- `[RELEASE_CADENCE]` — How often releases happen (e.g., weekly, on-demand, biweekly).
- `[ENVIRONMENTS]` — Environment pipeline (e.g., dev → staging → prod).
- `[FEATURE_FLAG_TOOL]` — Feature flag tool in use (LaunchDarkly / Unleash / Spring @ConditionalOnProperty / none).
- `[CURRENT_ROLLBACK_TIME_MINUTES]` — Current mean time to rollback in minutes.
- `[CHANGE_FAILURE_RATE_PERCENT]` — Percentage of deployments that require rollback (if known), or "unknown."
- `[ISTIO_ENABLED]` — Whether Istio traffic management is available (yes/no).
- `[DATABASE_MIGRATION_TOOL]` — Tool for DB migrations (Flyway / Liquibase / manual / none).
- `[TEAM_SIZE]` — Number of engineers on the team.

## The Prompt

```
You are a principal platform engineer expert in release strategy, Harness CI/CD, and OpenShift Container Platform.

Context:
- Service: [SERVICE_NAME]
- Current deployment strategy: [CURRENT_DEPLOYMENT_STRATEGY]
- Release cadence: [RELEASE_CADENCE]
- Environments: [ENVIRONMENTS]
- Feature flag tool: [FEATURE_FLAG_TOOL]
- Current rollback time: [CURRENT_ROLLBACK_TIME_MINUTES] minutes
- Change failure rate: [CHANGE_FAILURE_RATE_PERCENT]%
- Istio enabled: [ISTIO_ENABLED]
- Database migration tool: [DATABASE_MIGRATION_TOOL]
- Team size: [TEAM_SIZE]

Task:
Produce a Release Strategy Review with the following sections:

## 1. DORA Metrics Baseline
Estimate or assess current DORA metrics:
| Metric | Current Value | Elite Benchmark | Gap | Improvement Lever |
| Deployment Frequency | [RELEASE_CADENCE] | On-demand (multiple/day) | | |
| Lead Time for Changes | | < 1 hour | | |
| Change Failure Rate | [CHANGE_FAILURE_RATE_PERCENT]% | < 5% | | |
| Mean Time to Restore | [CURRENT_ROLLBACK_TIME_MINUTES] min | < 1 hour | | |

## 2. Deployment Strategy Assessment
Score [CURRENT_DEPLOYMENT_STRATEGY] and the top 2 alternatives:
| Strategy | Zero Downtime | Rollback Speed | Traffic Control | Istio Required | DB Migration Safe? | Score (1-5) |
| [CURRENT_DEPLOYMENT_STRATEGY] | | | | | | |
| Blue-Green | | | | | | |
| Canary | | | | | | |

Recommend the optimal strategy given the context. Justify.

## 3. Canary Release Design (if recommended)
If Canary is recommended:
| Phase | Traffic % | Duration | Automated Promotion Criteria | Rollback Criteria |
| Phase 1 | 5% | 15 min | Error rate < 0.1%, p99 < SLA | Error rate > 0.5% OR p99 > 2× SLA |
| Phase 2 | 25% | 15 min | | |
| Phase 3 | 50% | 15 min | | |
| Full | 100% | — | — | — |

Provide Istio VirtualService YAML for traffic splitting (if [ISTIO_ENABLED] = yes).

## 4. Feature Flag Strategy
Evaluate [FEATURE_FLAG_TOOL] and recommend:
| Flag Type | Use Case | Implementation | Lifecycle (sprints until removal) |
| Release flag | Gate new feature in production | | |
| Ops flag | Kill switch for degraded mode | | |
| Experiment flag | A/B testing | | |
| Permission flag | Role-based feature access | | |

Provide a Spring Boot @ConditionalOnProperty or Unleash integration code snippet.

## 5. Database Migration Safety Review
For [DATABASE_MIGRATION_TOOL]:
| Concern | Current Practice | Risk | Recommendation |
| Backward-compatible migrations (expand-contract pattern) | | | |
| Migration tested in staging before production | | | |
| Rollback script for each migration | | | |
| Migration running before or after deployment | | | |
| Long-running migrations blocking deploys | | | |

## 6. Environment Promotion Gates
For each environment boundary in [ENVIRONMENTS]:
| Gate | From Env | To Env | Required Checks | Approval Required? | Automated? |
| Dev → Staging | | | Unit tests, SonarQube, Snyk | No | Yes |
| Staging → Prod | | | Integration tests, perf tests, security scan | Yes | Semi |

## 7. Rollback Runbook
For [CURRENT_DEPLOYMENT_STRATEGY] on OCP with Harness:
| # | Step | Command / Harness Action | Time Estimate |
| 1 | Trigger rollback | Harness → Rollback stage | < 1 min |
| 2 | Traffic shift to previous version | | |
| 3 | Verify health endpoints | oc get pods / curl /actuator/health | 2 min |
| 4 | Confirm rollback complete | | |
| 5 | Notify stakeholders | | |

Target total rollback time: < 5 minutes.

## 8. Release Communication Plan
| Audience | Channel | Pre-release Notice | Post-release Notice | Rollback Notice |
| Engineering | Slack #deploys | 30 min | Immediate | Immediate |
| Product | | | | |
| Support | | | | |
| Management | | Major only | Major only | Any failure |

## 9. Priority Improvements
| Rank | Improvement | Expected DORA Impact | Effort (H/M/L) | Owner |

Reference Harness CD 0.x canary deployment step. Reference Istio VirtualService weight-based routing. Provide actionable configuration, not theory.
```

## Expected Output

A structured release strategy review containing:
- DORA metrics baseline table with gaps
- Deployment strategy comparison scorecard
- Canary traffic phase table + Istio VirtualService YAML
- Feature flag strategy table with code snippet
- Database migration safety review table
- Environment promotion gates table
- Step-by-step rollback runbook (target < 5 min)
- Release communication plan table
- Ranked improvement list

## Benefits

- Quantifies DORA metric gaps and maps them to specific process improvements.
- Produces a Canary release configuration that reduces change failure rate without increasing deployment friction.
- Provides a < 5-minute rollback runbook that is rehearsable before it is needed.

## Related Prompts

- [cicd-pipeline-review.md](cicd-pipeline-review.md) — Audit the Harness pipeline that executes this release strategy.
- [ocp-migration-strategy.md](ocp-migration-strategy.md) — Migrate workloads before applying the new release strategy.
- [../observability/slo-definition.md](../observability/slo-definition.md) — Define the SLOs that drive canary promotion/rollback decisions.
