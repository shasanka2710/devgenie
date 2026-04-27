---
name: Security Architecture Review
domain: security
complexity: L3
output-format: adr
token-estimate: high
tags: security, threat-modelling, owasp, spring-boot, zero-trust, stride, snyk, architecture
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt for an end-to-end security architecture review of a new or significantly changed service. Ideal before a major feature launch, after a security incident, or as part of a periodic security architecture review cycle. Produces a threat model and a security ADR that can be presented to a security review board.

## Prerequisites

- `[SERVICE_NAME]` — Name of the service or feature being reviewed.
- `[ARCHITECTURE_DIAGRAM_DESCRIPTION]` — Textual description of the architecture: components, data flows, external integrations, trust boundaries.
- `[DATA_CLASSIFICATION]` — Sensitivity of data handled (PII / financial / public / internal).
- `[EXTERNAL_INTEGRATIONS]` — List of external systems the service integrates with.
- `[AUTH_MECHANISM]` — Authentication and authorization mechanism (OAuth2 + JWT / mTLS / API Key).
- `[DEPLOYMENT_PLATFORM]` — OCP namespace, Istio mesh, network zone.
- `[SNYK_REPORT]` — Snyk dependency and container scan findings, or "not available."
- `[COMPLIANCE_REQUIREMENTS]` — Applicable standards (PCI-DSS / SOC2 / GDPR / HIPAA / internal), or "none."
- `[PREVIOUS_CVES]` — Known CVEs affecting this service or its dependencies, or "none."

## The Prompt

```
You are a principal security architect conducting a formal security architecture review and threat model.

Context:
- Service: [SERVICE_NAME]
- Architecture: [ARCHITECTURE_DIAGRAM_DESCRIPTION]
- Data classification: [DATA_CLASSIFICATION]
- External integrations: [EXTERNAL_INTEGRATIONS]
- Authentication/authorization: [AUTH_MECHANISM]
- Deployment: [DEPLOYMENT_PLATFORM]
- Snyk findings: [SNYK_REPORT]
- Compliance requirements: [COMPLIANCE_REQUIREMENTS]
- Known CVEs: [PREVIOUS_CVES]

Task:
Produce a Security Architecture Review as a formal ADR with the following sections:

## ADR-SEC-001: Security Architecture of [SERVICE_NAME]

### Status
Proposed

### Context
Describe the security context: data sensitivity, threat actors, trust boundaries, and compliance obligations.

## 1. Trust Boundary Map
Identify all trust boundaries in the architecture:
| Boundary | From Component | To Component | Protocol | Auth Mechanism | Encrypted in Transit? | Encrypted at Rest? |

## 2. STRIDE Threat Model
Apply STRIDE to each trust boundary and key component:
| Threat Category | Component / Boundary | Threat Description | Attack Vector | Likelihood (H/M/L) | Impact (H/M/L) | Risk Score | Existing Control | Residual Risk |
STRIDE categories: Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege.

## 3. OWASP Top 10 Assessment
Map the service to OWASP Top 10 2021:
| OWASP Category | Applicable? | Evidence / Concern | Current Control | Gap | Recommendation |

## 4. Dependency and Container Security
Based on [SNYK_REPORT]:
| CVE | Dependency | CVSS Score | Exploitability | Fix Version | Priority | Sprint Target |

If Snyk report not available, list the top 5 dependency categories to scan for this stack (Spring Boot, Kafka client, MongoDB driver, Istio sidecar base image).

## 5. Secrets and Credential Security
Assess secrets handling:
| Secret Type | Storage | Rotation | Vault-backed? | Risk | Recommendation |

## 6. Authorization Design Review
Review the authorization model in depth:
- Is RBAC or ABAC used? Is it sufficient for [DATA_CLASSIFICATION]?
- Are Spring Security method-level annotations (@PreAuthorize, @PostFilter) used correctly?
- Is Istio AuthorizationPolicy layered on top?
- Are there any privilege escalation paths?
Return as a numbered findings list with severity.

## 7. Audit and Non-Repudiation
| Action | Audit Logged? | Log Contents | Tamper-Proof? | Retention Period | Gap |
Key actions: authentication events, authorization denials, data access for PII, admin operations, configuration changes.

## 8. Compliance Gap Analysis
For each requirement in [COMPLIANCE_REQUIREMENTS]:
| Requirement | Control ID | Current State | Gap | Evidence Needed | Owner |

## 9. Penetration Test Scope Recommendation
List the top 5 attack scenarios for a penetration test team:
| # | Attack Scenario | Target Component | Expected Finding | Test Technique |

## 10. Security Decision and Residual Risk
State the security architecture decision: approved / approved with conditions / rejected.
List all accepted residual risks with business owner sign-off requirement.

### Consequences
List security trade-offs accepted in the design.

Use OWASP ASVS Level 2 as the baseline control set. Reference Spring Security 6.x APIs. Flag any Spring Boot auto-configuration defaults that weaken security (e.g., CSRF disabled for REST, H2 console enabled).
```

## Expected Output

A formal security ADR document containing:
- Trust boundary map table
- STRIDE threat model table (all categories × all boundaries)
- OWASP Top 10 gap table
- CVE table from Snyk
- Secrets handling assessment
- Authorization design findings
- Audit/non-repudiation table
- Compliance gap table
- Pen test scope recommendation
- Security decision with residual risk acceptance

## Benefits

- Produces a board-ready security ADR in one pass, replacing multi-day manual threat modelling workshops.
- STRIDE × trust boundary matrix ensures no threat vector is missed.
- Maps directly to compliance requirements, reducing audit preparation effort.

## Related Prompts

- [mtls-service-mesh-review.md](mtls-service-mesh-review.md) — Deep-dive on Istio mTLS posture.
- [secrets-management-review.md](secrets-management-review.md) — Deep-dive on Vault secrets hygiene.
- [../architecture/api-contract-design.md](../architecture/api-contract-design.md) — Security review of the API layer.
