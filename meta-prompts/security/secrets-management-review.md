---
name: Secrets Management Review
domain: security
complexity: L2
output-format: scorecard
token-estimate: medium
tags: vault, secrets, spring-boot, hashicorp, secret-rotation, kubernetes, ocp, security
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt when auditing how secrets (database credentials, API keys, certificates, service account tokens) are managed across Spring Boot services integrated with HashiCorp Vault on OpenShift. Ideal before a security audit, after a secret leak incident, or when onboarding new services to Vault.

## Prerequisites

- `[SERVICE_LIST]` — List of Spring Boot services in scope.
- `[VAULT_VERSION]` — HashiCorp Vault version (e.g., 1.15).
- `[VAULT_AUTH_METHOD]` — Auth method used (Kubernetes auth / AppRole / Token / OIDC).
- `[SECRET_TYPES]` — Types of secrets managed (DB credentials, API keys, TLS certs, JWT signing keys, etc.).
- `[SPRING_CLOUD_VAULT_VERSION]` — Spring Cloud Vault version, or "not used."
- `[SECRET_ROTATION_POLICY]` — Frequency and method of secret rotation (manual / dynamic / automated), or "unknown."
- `[VAULT_POLICIES]` — Paste Vault policy HCL snippets, or "not available."
- `[OCP_NAMESPACE]` — OCP namespace(s) where services are deployed.
- `[EXISTING_SECRET_SCAN]` — Snyk or other scanner findings on secret exposure, or "none."

## The Prompt

```
You are a principal security engineer expert in HashiCorp Vault integration with Spring Boot and OpenShift Container Platform.

Context:
- Services in scope: [SERVICE_LIST]
- Vault version: [VAULT_VERSION]
- Vault auth method: [VAULT_AUTH_METHOD]
- Secret types: [SECRET_TYPES]
- Spring Cloud Vault: [SPRING_CLOUD_VAULT_VERSION]
- Secret rotation policy: [SECRET_ROTATION_POLICY]
- Vault policies: [VAULT_POLICIES]
- OCP namespace: [OCP_NAMESPACE]
- Existing scan findings: [EXISTING_SECRET_SCAN]

Task:
Produce a Secrets Management Review with the following sections:

## 1. Secret Inventory and Risk Scorecard
For each service in [SERVICE_LIST]:
| Service | Secret Type(s) | Storage Location | Rotation Frequency | Vault-backed? | Score (1-5) |
Scoring: 5 = all secrets in Vault, dynamic, rotated < 24h. Deduct 1 per gap.

## 2. Vault Authentication Method Assessment
Evaluate [VAULT_AUTH_METHOD] against best practices:
| Criterion | Current State | Best Practice | Gap | Recommendation |
Criteria:
- Auth method appropriateness for Kubernetes/OCP (Kubernetes auth is preferred)
- Token TTL and renewal strategy
- Service account binding specificity (namespace + service account scoped)
- Audit log enabled for auth events
- Least-privilege policy per service (no wildcard path grants)

## 3. Secret Anti-Pattern Detection
Check for these anti-patterns across [SERVICE_LIST]:
| Anti-Pattern | Service Affected | Evidence | Risk (H/M/L) | Remediation |
Anti-patterns:
- Secrets in environment variables (not Vault agent / Spring Cloud Vault)
- Secrets in Kubernetes Secrets (not encrypted at rest with Vault KMS)
- Secrets in application.properties or application.yml committed to Git
- Long-lived static tokens (> 90 days)
- Shared secrets across services (no per-service credential isolation)
- Missing secret expiry/TTL
- Vault root token usage in non-emergency contexts

## 4. Spring Cloud Vault Integration Review
If [SPRING_CLOUD_VAULT_VERSION] is in use, assess:
| Configuration | Current Value | Recommended Value | Gap |
Configurations:
- vault.uri
- vault.authentication (should be KUBERNETES)
- vault.kubernetes.role (should be service-specific)
- vault.config.lifecycle.enabled (should be true for lease renewal)
- vault.config.lifecycle.min-renewal (recommended: 10s)
- Secret backend paths (database/, secret/, pki/)

Provide a corrected bootstrap.yml / application.yml snippet as a code block.

## 5. Dynamic Secret Assessment
For each [SECRET_TYPES]:
| Secret Type | Dynamic Secret Available in Vault? | Current Approach | Recommended Vault Backend | Migration Effort |
Dynamic secret backends to consider: database (PostgreSQL/MongoDB), PKI (certs), AWS/GCP (cloud creds).

## 6. Vault Policy Least-Privilege Review
Review [VAULT_POLICIES] and assess each policy:
| Policy Name | Paths Granted | Capabilities | Over-Permissive? | Recommended Scoping |

Provide corrected HCL policy snippet for any over-permissive policy found.

## 7. Secret Scanning in CI/CD
Assess how secret leakage is detected in the Harness pipeline:
- Is Snyk or Gitleaks integrated as a pre-commit or CI gate?
- Are OCP Secrets scanned for insecure configurations?
- Is Vault audit log monitored for anomalous access?
Return as a pass/fail checklist with recommendations.

## 8. Remediation Roadmap
| Rank | Finding | Severity (CRITICAL/HIGH/MEDIUM) | Effort (H/M/L) | Owner | Target Sprint |
```

## Expected Output

A structured secrets management review containing:
- Secret inventory scorecard (6 columns per service)
- Vault auth method assessment table
- Anti-pattern detection table (7 anti-patterns checked)
- Spring Cloud Vault configuration table with corrected YAML snippet
- Dynamic secret assessment table
- Vault policy review with corrected HCL snippets
- CI/CD secret scanning checklist
- Remediation roadmap sorted by severity

## Benefits

- Surfaces secrets stored outside Vault (env vars, Kubernetes Secrets, Git) that pose immediate breach risk.
- Identifies over-permissive Vault policies that violate least-privilege before an audit does.
- Provides ready-to-apply Spring Cloud Vault and HCL configuration corrections.

## Related Prompts

- [mtls-service-mesh-review.md](mtls-service-mesh-review.md) — Vault PKI for certificate management in Istio.
- [security-architecture-review.md](security-architecture-review.md) — Broader threat model incorporating secrets.
- [../devops-platform/cicd-pipeline-review.md](../devops-platform/cicd-pipeline-review.md) — Integrate secret scanning into Harness.
