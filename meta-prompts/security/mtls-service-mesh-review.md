---
name: mTLS Service Mesh Review
domain: security
complexity: L3
output-format: scorecard
token-estimate: high
tags: mtls, istio, service-mesh, zero-trust, security, certificate-management, ocp
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt when auditing the mutual TLS (mTLS) posture of services running within an Istio service mesh on OpenShift Container Platform (OCP). Most valuable before a security audit, after adding new services to the mesh, or following an Istio upgrade.

## Prerequisites

- `[NAMESPACE_LIST]` — OCP namespaces in scope for the review.
- `[SERVICE_LIST]` — List of services within the mesh to evaluate.
- `[ISTIO_VERSION]` — Istio version deployed (e.g., 1.20).
- `[PEER_AUTH_POLICIES]` — Paste existing PeerAuthentication YAML resources, or "not configured."
- `[DESTINATION_RULES]` — Paste existing DestinationRule YAML resources, or "not configured."
- `[AUTHZ_POLICIES]` — Paste existing AuthorizationPolicy YAML resources, or "not configured."
- `[CERT_PROVIDER]` — Certificate provider (Istio built-in CA / cert-manager / Vault PKI).
- `[CERT_ROTATION_INTERVAL]` — Certificate rotation interval (e.g., 24h), or "unknown."
- `[PERMISSIVE_MODE_SERVICES]` — Services with PeerAuthentication in PERMISSIVE mode, or "none."

## The Prompt

```
You are a principal security engineer expert in Istio service mesh mTLS configuration on OpenShift Container Platform.

Context:
- Namespaces in scope: [NAMESPACE_LIST]
- Services in scope: [SERVICE_LIST]
- Istio version: [ISTIO_VERSION]
- Certificate provider: [CERT_PROVIDER]
- Certificate rotation interval: [CERT_ROTATION_INTERVAL]
- Services in PERMISSIVE mode: [PERMISSIVE_MODE_SERVICES]
- PeerAuthentication policies: [PEER_AUTH_POLICIES]
- DestinationRules: [DESTINATION_RULES]
- AuthorizationPolicies: [AUTHZ_POLICIES]

Task:
Produce a structured mTLS Service Mesh Security Review with the following sections:

## 1. mTLS Coverage Scorecard
For each service in [SERVICE_LIST]:
| Service | Namespace | PeerAuthentication Mode | DestinationRule TLS Mode | AuthzPolicy Exists? | Inbound mTLS | Outbound mTLS | Score (1-5) |
Scoring: 5 = STRICT mode, AuthzPolicy present, cert rotation < 24h. Deduct 1 per gap.

## 2. Zero-Trust Posture Assessment
Score the overall mesh posture against zero-trust principles (1–5):
| Principle | Score | Evidence | Gap | Recommendation |
Principles:
- Verify explicitly (every call authenticated with mTLS)
- Least privilege (AuthorizationPolicies restrict to minimum required paths/methods)
- Assume breach (audit logging, anomaly detection, no implicit trust within namespace)
- Workload identity (SPIFFE/SPIRE identity per workload, not per namespace)
- Certificate lifecycle (automated rotation, short TTL, revocation capability)

## 3. PERMISSIVE Mode Risk Register
For each service in [PERMISSIVE_MODE_SERVICES]:
| Service | Justification for PERMISSIVE | Risk (H/M/L) | Migration Path to STRICT | Target Date |

## 4. AuthorizationPolicy Gap Analysis
For each service without an AuthorizationPolicy:
| Service | Exposed Operations | Potential Unauthorized Access | Recommended AuthzPolicy | Priority |

Provide YAML snippets for the top 3 missing AuthorizationPolicies.

## 5. Certificate Management Review
Assess [CERT_PROVIDER] and [CERT_ROTATION_INTERVAL] against best practices:
| Concern | Current State | Best Practice | Gap | Remediation |
Concerns:
- Root CA trust anchor rotation
- Workload certificate TTL (recommended < 24h)
- Certificate revocation (OCSP or CRL)
- Vault PKI integration (if applicable)
- OCP cert-manager integration
- Audit trail for certificate issuance

## 6. Istio Security Configuration Gaps
Review the provided policies for these specific misconfigurations:
| Misconfiguration | Present? | Evidence | CVE or Security Reference | Fix |
Misconfigurations to check:
- Missing namespace-level STRICT PeerAuthentication (allowing PERMISSIVE as default)
- DestinationRule with TLSmode=DISABLE
- AuthzPolicy with allow-all rule (action: ALLOW with no conditions)
- JWT principal missing from AuthzPolicy for external-facing services
- Egress gateway bypassing mTLS for external calls

## 7. OCP-Specific Hardening Recommendations
List OCP/OpenShift-specific hardening steps:
1. NetworkPolicy to complement Istio AuthzPolicy (defense in depth)
2. SecurityContextConstraints (SCC) for Istio sidecar
3. OCP Service CA vs Istio CA trust hierarchy
4. Namespace isolation with Istio Sidecar resource scoping

## 8. Remediation Roadmap
| Rank | Finding | Severity (CRITICAL/HIGH/MEDIUM) | Effort (H/M/L) | Owner | Target Sprint |

All YAML snippets must be valid Istio [ISTIO_VERSION] CRD format. Flag any deprecated Istio APIs.
```

## Expected Output

A structured mTLS security review containing:
- mTLS coverage scorecard (8 columns, one row per service)
- Zero-trust posture scorecard (5 principles)
- PERMISSIVE mode risk register
- AuthorizationPolicy gap analysis with YAML snippets
- Certificate management review table
- Istio misconfiguration checklist
- OCP-specific hardening recommendations (numbered)
- Remediation roadmap sorted by severity

## Benefits

- Produces a single-pass, evidence-based mTLS audit that replaces hours of manual policy review.
- Identifies PERMISSIVE mode services that are the most common mTLS security gap in real meshes.
- Surfaces Istio misconfigurations that are not flagged by default security scanners.

## Related Prompts

- [secrets-management-review.md](secrets-management-review.md) — Audit Vault-backed certificate management.
- [security-architecture-review.md](security-architecture-review.md) — Broader threat model incorporating the mesh.
- [../devops-platform/ocp-migration-strategy.md](../devops-platform/ocp-migration-strategy.md) — Include mesh hardening in OCP migration.
