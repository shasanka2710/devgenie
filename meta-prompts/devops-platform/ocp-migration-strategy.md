---
name: OCP Migration Strategy
domain: devops-platform
complexity: L3
output-format: runbook
token-estimate: high
tags: openshift, ocp, kubernetes, migration, containerization, spring-boot, istio, harness
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt when planning the migration of Spring Boot workloads to OpenShift Container Platform (OCP). Ideal at the start of a lift-and-shift or re-platform initiative, or when adding a new service to an existing OCP cluster with Istio and Harness.

## Prerequisites

- `[WORKLOAD_LIST]` — List of Spring Boot services to migrate with their current hosting (VM / bare metal / legacy Kubernetes / another OCP cluster).
- `[OCP_VERSION]` — Target OCP version (e.g., 4.14).
- `[ISTIO_ENABLED]` — Whether Istio service mesh is present on target OCP (yes/no).
- `[HARNESS_DELEGATE_NAMESPACE]` — OCP namespace where the Harness Delegate is deployed.
- `[VAULT_INTEGRATION]` — HashiCorp Vault integration method (Vault Agent Injector / CSI Provider / Spring Cloud Vault).
- `[CURRENT_RESOURCE_PROFILE]` — CPU and memory of each service (requests/limits).
- `[PERSISTENT_STORAGE]` — Services with stateful storage requirements (PVCs, databases).
- `[NETWORK_DEPENDENCIES]` — External network dependencies (on-premises APIs, databases, message brokers).
- `[COMPLIANCE_REQUIREMENTS]` — Security/compliance requirements (PCI, SOC2, etc.).
- `[MIGRATION_TIMELINE]` — Available migration window (e.g., 3 months, one quarter).

## The Prompt

```
You are a principal platform engineer expert in OpenShift Container Platform 4.x, Istio, and Spring Boot containerization.

Context:
- Workloads to migrate: [WORKLOAD_LIST]
- Target OCP version: [OCP_VERSION]
- Istio enabled: [ISTIO_ENABLED]
- Harness Delegate namespace: [HARNESS_DELEGATE_NAMESPACE]
- Vault integration: [VAULT_INTEGRATION]
- Current resource profile: [CURRENT_RESOURCE_PROFILE]
- Persistent storage: [PERSISTENT_STORAGE]
- Network dependencies: [NETWORK_DEPENDENCIES]
- Compliance requirements: [COMPLIANCE_REQUIREMENTS]
- Migration timeline: [MIGRATION_TIMELINE]

Task:
Produce an OCP Migration Strategy Runbook with the following sections:

## 1. Migration Readiness Assessment
Score each workload in [WORKLOAD_LIST] for migration readiness (1–5, 5 = ready to migrate now):
| Service | Containerized? | 12-Factor Compliance | Stateless? | Health Endpoints? | Config Externalized? | Secrets Vault-ready? | Readiness Score |

12-Factor items to check: config from env, stateless processes, explicit port binding, disposability (fast startup/graceful shutdown).

## 2. Container Image Design
For each Spring Boot service:
| Service | Base Image | JVM Configuration | Image Size Target | Build Tool | OCP SCC Compatible? |

Provide a multi-stage Dockerfile template optimized for OCP:
- Use Red Hat UBI 8/9 minimal as base (OCP compatible)
- Spring Boot 3.x layered JAR
- Non-root user (OCP security requirement)
- JVM flags for container-aware memory: -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
- Health check ENTRYPOINT

## 3. OCP Resource Manifest Design
For each service, specify:
| Resource | Name | Namespace | Key Configuration |
Resources to define: Deployment, Service, Route (if externally exposed), ConfigMap, ServiceAccount, NetworkPolicy, HorizontalPodAutoscaler, PodDisruptionBudget.

Provide a Deployment YAML template with:
- Resource requests and limits calculated from [CURRENT_RESOURCE_PROFILE]
- Liveness probe (/actuator/health/liveness)
- Readiness probe (/actuator/health/readiness)
- Startup probe (for slow-starting services)
- topologySpreadConstraints for HA across OCP nodes
- securityContext (runAsNonRoot, readOnlyRootFilesystem, allowPrivilegeEscalation: false)

## 4. Istio Service Mesh Integration
If [ISTIO_ENABLED] is yes:
For each service:
| Configuration | Resource | Spec |
| Sidecar injection | Namespace label | istio-injection: enabled |
| PeerAuthentication | mTLS mode | STRICT |
| DestinationRule | TLS mode | ISTIO_MUTUAL |
| VirtualService | Routing | |
| AuthorizationPolicy | Access control | |

Provide annotated YAML for each Istio resource.

## 5. Vault Integration Runbook
For [VAULT_INTEGRATION]:
Step-by-step setup instructions for the chosen method:
1. Vault Kubernetes auth method configuration (vault write auth/kubernetes/config ...)
2. Vault role creation scoped to OCP service account
3. Vault policy creation (least privilege)
4. OCP ServiceAccount annotation or Spring Cloud Vault configuration
5. Verification steps

## 6. Harness Pipeline Integration
Step-by-step to onboard each service to Harness:
1. Register OCP connector in Harness
2. Create Kubernetes deployment step configuration
3. Attach Harness Delegate in [HARNESS_DELEGATE_NAMESPACE]
4. Configure OCP namespace override per environment
5. Set up Harness Kubernetes Rolling / Canary Deploy step
6. Configure health check with OCP readiness endpoint
7. Rollback strategy configuration

## 7. Network Dependency Resolution
For each dependency in [NETWORK_DEPENDENCIES]:
| Dependency | Type | Current Access Method | OCP Access Method | NetworkPolicy Required? | Egress Gateway Required? |

## 8. Migration Wave Plan
Group workloads into migration waves based on readiness score and dependencies:
| Wave | Services | Prerequisites | Duration | Rollback Plan | Success Criteria |

## 9. Compliance and Security Hardening
For [COMPLIANCE_REQUIREMENTS]:
| Requirement | OCP Control | Implementation | Verification |
Controls to address: Pod Security Admission, SecurityContextConstraints, NetworkPolicy, audit logging, image signing (Cosign/Sigstore), SBOM generation.

## 10. Go-Live Checklist
| # | Checkpoint | Owner | Verified? |
| 1 | Container image passes Snyk scan (0 CRITICAL/HIGH CVEs) | DevOps | |
| 2 | All health probes passing | Dev | |
| 3 | Vault secrets injected and application starts | Platform | |
| 4 | Istio mTLS STRICT mode verified | Security | |
| 5 | Harness pipeline end-to-end tested in staging | DevOps | |
| 6 | HPA and PDB configured | Platform | |
| 7 | SLO alerts firing correctly in staging | Ops | |
| 8 | Rollback tested in staging | DevOps | |
| 9 | Runbook documented and reviewed | Team | |
| 10 | Compliance sign-off obtained | Security/Compliance | |

Use OCP [OCP_VERSION] APIs. Reference Spring Boot Actuator for health endpoints. All YAML must be valid OCP 4.x resource manifests.
```

## Expected Output

A complete OCP migration runbook containing:
- Migration readiness scorecard per service
- Container image design table + multi-stage Dockerfile template
- OCP resource manifest table + Deployment YAML template
- Istio integration YAML resources
- Vault integration step-by-step runbook
- Harness pipeline onboarding steps
- Network dependency resolution table
- Migration wave plan table
- Compliance/security hardening table
- Go-live checklist

## Benefits

- Prevents the most common OCP migration failure: non-root user incompatibility with existing container images.
- Produces complete OCP manifests with correct security contexts, avoiding SecurityContextConstraints violations.
- Provides a wave-based migration plan with rollback criteria that de-risks large migrations.

## Related Prompts

- [cicd-pipeline-review.md](cicd-pipeline-review.md) — Review the Harness pipeline once OCP is configured.
- [../security/mtls-service-mesh-review.md](../security/mtls-service-mesh-review.md) — Audit the Istio mTLS posture post-migration.
- [../security/secrets-management-review.md](../security/secrets-management-review.md) — Audit Vault integration quality.
