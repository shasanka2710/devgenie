---
name: Release Notes Generation
domain: documentation
complexity: L1
output-format: narrative
token-estimate: low
tags: release-notes, changelog, documentation, spring-boot, harness, versioning, communication
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt to generate structured release notes from a list of commits, JIRA tickets, or PR descriptions. Ideal for every production release, when communicating changes to non-technical stakeholders, or when building an automated release notes step in a Harness pipeline.

## Prerequisites

- `[SERVICE_NAME]` — Name of the Spring Boot service being released.
- `[VERSION]` — Release version (semver, e.g., 2.4.1).
- `[PREVIOUS_VERSION]` — Previous version being replaced.
- `[RELEASE_DATE]` — ISO-8601 date of the release.
- `[COMMITS_OR_TICKETS]` — Paste a list of Git commits (hash + message) or JIRA ticket summaries included in this release.
- `[DEPLOYMENT_TARGET]` — OCP environment this release is deployed to.
- `[BREAKING_CHANGES]` — List of known breaking changes, or "none."
- `[KNOWN_ISSUES]` — Known issues or limitations in this release, or "none."
- `[AUDIENCE]` — Primary audience: engineering / product / customers / all.

## The Prompt

```
You are a principal engineer generating structured, professional release notes for a Spring Boot service.

Context:
- Service: [SERVICE_NAME]
- Version: [VERSION] (upgrading from [PREVIOUS_VERSION])
- Release date: [RELEASE_DATE]
- Deployment target: [DEPLOYMENT_TARGET]
- Breaking changes: [BREAKING_CHANGES]
- Known issues: [KNOWN_ISSUES]
- Audience: [AUDIENCE]

Commits / Tickets:
[COMMITS_OR_TICKETS]

Task:
Produce release notes in the following format. Adapt the language to [AUDIENCE]:
- Engineering: technical detail, config changes, migration steps
- Product: feature descriptions, user impact, business value
- Customers: plain English, no internal jargon, benefit-focused

---

# [SERVICE_NAME] v[VERSION] Release Notes

**Released:** [RELEASE_DATE]  
**Environment:** [DEPLOYMENT_TARGET]  
**Previous Version:** [PREVIOUS_VERSION]  

---

## Summary
Write 2–3 sentences summarizing the most significant changes and their business impact.

## ⚠️ Breaking Changes
If [BREAKING_CHANGES] is not "none", list each breaking change:
| Change | Affected Component | Migration Steps | Deadline for Migration |

If none: *No breaking changes in this release.*

## ✨ New Features
Group new features by category (API, Data Processing, Observability, Security, etc.):
| Feature | Description | Ticket | Impact |

## 🐛 Bug Fixes
| Bug Fixed | Root Cause (brief) | Ticket | Severity |

## 🔒 Security Updates
| CVE / Finding | Dependency | Previous Version | Fixed Version | Severity |

## ⚡ Performance Improvements
| Improvement | Before | After | Ticket |

## 🔧 Configuration Changes
If any new, changed, or removed configuration properties:
| Property | Change Type (Added/Changed/Removed) | Old Value / Default | New Value / Default | Action Required? |

## 🏗️ Dependency Updates
| Dependency | Old Version | New Version | Reason |

## Known Issues
If [KNOWN_ISSUES] is not "none":
| Issue | Workaround | Expected Fix Version |

If none: *No known issues in this release.*

## Upgrade Instructions
Numbered steps for upgrading from [PREVIOUS_VERSION] to [VERSION]:
1. (Configuration changes required)
2. (Database migration steps if applicable)
3. (API consumer updates if applicable)
4. (Verification steps)

## Rollback Instructions
Steps to roll back to [PREVIOUS_VERSION] if needed:
1. Trigger rollback in Harness pipeline ([SERVICE_NAME] → Rollback stage)
2. Verify previous version is running: oc get deployment [SERVICE_NAME] -o jsonpath='{.spec.template.spec.containers[0].image}'
3. Confirm health: curl -s [DEPLOYMENT_TARGET]/actuator/health | jq .status

---

Classify commits using Conventional Commits convention (feat, fix, perf, security, chore, refactor, docs). Group uncategorized commits under "Other Changes." Do not include internal tooling commits (chore: update Gradle wrapper, etc.) unless they affect consumers.
```

## Expected Output

A complete, professional release notes document containing:
- 2–3 sentence executive summary
- Breaking changes table (if applicable)
- New features grouped by category
- Bug fixes table
- Security updates table
- Performance improvements table
- Configuration changes table
- Dependency updates table
- Known issues table (if applicable)
- Numbered upgrade instructions
- Copy-paste rollback instructions

## Benefits

- Produces audience-appropriate release notes that eliminate the need to write separate engineering and product communications.
- Surfaces security updates and breaking changes prominently, reducing the chance they are missed.
- Includes OCP-specific rollback instructions that the on-call engineer can execute immediately if needed.

## Related Prompts

- [adr-generation.md](adr-generation.md) — Record architectural decisions made in this release.
- [technical-specification.md](technical-specification.md) — Write the spec for features in a future release.
- [../devops-platform/release-strategy-review.md](../devops-platform/release-strategy-review.md) — Review and improve the overall release strategy.
