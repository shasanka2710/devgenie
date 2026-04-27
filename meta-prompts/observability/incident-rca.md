---
name: Incident RCA
domain: observability
complexity: L2
output-format: narrative
token-estimate: medium
tags: rca, post-mortem, incident, blameless, spring-boot, observability, kafka, timeline
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt to write a structured, blameless post-incident root cause analysis (RCA) after a production incident involving a Spring Boot service. Ideal within 24–48 hours of incident resolution when logs, traces, and metrics are still fresh.

## Prerequisites

- `[INCIDENT_ID]` — Incident identifier (e.g., INC-2025-042).
- `[SERVICE_NAME]` — Primary service affected.
- `[INCIDENT_START]` — ISO-8601 timestamp when the incident began (or when first detected).
- `[INCIDENT_END]` — ISO-8601 timestamp when service was fully restored.
- `[INCIDENT_DESCRIPTION]` — 2–3 sentence plain-English description of what happened.
- `[USER_IMPACT]` — Who was affected and how (e.g., "5,000 users unable to place orders for 47 minutes").
- `[TIMELINE_EVENTS]` — Bullet list of timestamped events from detection to resolution.
- `[ROOT_CAUSE_HYPOTHESIS]` — Initial hypothesis about the root cause, or "unknown."
- `[MONITORING_DATA]` — Key metrics and log snippets (error rates, latency graphs description, log error samples).
- `[CONTRIBUTING_FACTORS]` — Known contributing factors or "investigate."
- `[RESPONDERS]` — Names/roles of engineers involved in the response.

## The Prompt

```
You are a principal engineer writing a blameless post-incident RCA for a production incident.

Context:
- Incident: [INCIDENT_ID]
- Affected service: [SERVICE_NAME]
- Start: [INCIDENT_START]
- End: [INCIDENT_END]
- Description: [INCIDENT_DESCRIPTION]
- User impact: [USER_IMPACT]
- Initial root cause hypothesis: [ROOT_CAUSE_HYPOTHESIS]
- Monitoring data: [MONITORING_DATA]
- Contributing factors: [CONTRIBUTING_FACTORS]
- Responders: [RESPONDERS]

Timeline:
[TIMELINE_EVENTS]

Task:
Produce a complete blameless Post-Incident RCA document with the following sections:

## Incident Summary
| Field | Value |
| Incident ID | [INCIDENT_ID] |
| Severity | (derive from user impact) |
| Duration | (calculate from start/end) |
| MTTR | (minutes) |
| Services Affected | [SERVICE_NAME] |
| User Impact | [USER_IMPACT] |
| Responders | [RESPONDERS] |

## Executive Summary (3 sentences, non-technical)
Write a plain-English summary suitable for leadership: what happened, how it was resolved, and what is being done to prevent recurrence.

## Detailed Timeline
Expand [TIMELINE_EVENTS] into a detailed, precise timeline:
| Time (UTC) | Event | Who | Evidence (log/metric link) | Action Taken |
Include: first signal, detection, escalation, diagnosis, mitigation start, mitigation complete, full restoration, all-clear declaration.

## Root Cause Analysis: 5 Whys
Starting from the user-visible symptom, apply 5 Whys:
| Why # | Question | Answer | Evidence |
| Why 1 | Why did users experience [symptom]? | | |
| Why 2 | Why did [answer to Why 1] happen? | | |
| Why 3 | | | |
| Why 4 | | | |
| Why 5 | | | |
**Root Cause:** (the deepest systemic Why)

## Contributing Factors
| Factor | Category (Technical/Process/Organizational) | How It Contributed |
Categories to consider: insufficient alerting, missing circuit breaker, deployment process gap, knowledge gap, toil/manual process, dependency on upstream service.

## Impact Analysis
| Dimension | Value |
| Requests Failed | |
| Users Affected | |
| Revenue Impact (est.) | |
| SLO Breach | (was the error budget consumed?) |
| Data Integrity Impact | |

## What Went Well
List 3–5 things the team did effectively during the incident response. Be specific.

## What Could Be Improved
List 3–5 specific process or technical gaps revealed by the incident.

## Action Items
| # | Action | Type (Prevent/Detect/Respond) | Owner | Due Date | Linked Ticket |
| 1 | | Prevent | | | |
| 2 | | Detect | | | |
| 3 | | Respond | | | |
Every action must be: specific, assigned, time-bound, and linked to a JIRA/GitHub ticket.

Classify actions by type:
- **Prevent**: eliminates the root cause from recurring
- **Detect**: improves time-to-detect (alerting, dashboards)
- **Respond**: improves time-to-mitigate (runbooks, automation)

## Monitoring and Alerting Gaps
| Missing Signal | What It Would Have Detected | Recommended Implementation |
Based on [MONITORING_DATA], identify what monitoring would have caught the issue sooner.

## Runbook Update
List runbooks that must be created or updated as a result of this incident:
| Runbook | Status (Create/Update) | Assigned To |

Write in a blameless tone. Attribute failures to systems, processes, and conditions — never to individuals.
```

## Expected Output

A complete blameless RCA document containing:
- Incident summary table
- 3-sentence executive summary (leadership-ready)
- Detailed timeline table with evidence
- 5 Whys analysis table
- Contributing factors table
- Impact analysis table
- What went well (3–5 bullet points)
- What could be improved (3–5 bullet points)
- Action items table (Prevent/Detect/Respond, assigned and time-bound)
- Monitoring gaps table
- Runbook update list

## Benefits

- Produces a complete, leadership-ready RCA in one pass instead of the typical multi-day document drafting.
- 5 Whys structure forces identification of systemic root causes rather than proximate causes.
- Classifying action items by Prevent/Detect/Respond ensures a balanced improvement plan.

## Related Prompts

- [observability-design.md](observability-design.md) — Implement the monitoring gaps identified in the RCA.
- [slo-definition.md](slo-definition.md) — Update SLOs based on the impact analysis.
- [../architecture/resilience-pattern-review.md](../architecture/resilience-pattern-review.md) — Apply resilience improvements identified in the RCA.
