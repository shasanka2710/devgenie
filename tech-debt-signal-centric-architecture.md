
# Tech Debt Platform – Signal-Centric Architecture Guide

## 1. Purpose of This Document
This document captures the **complete architectural reasoning, concepts, and design decisions**
for building an **enterprise-grade Tech Debt Platform** using a **signal-centric, configuration-driven approach**.

It is intended for:
- Architects
- Senior engineers
- Platform owners

This document is tool-agnostic, evolution-friendly, and suitable for long-term enterprise use.

---

## 2. Core Problem Statement

We are **not** building integrations for tools like SonarQube, Checkmarx, or BlackDuck.

We are building a **debt intelligence platform** that:
- Consumes signals from many tools
- Normalizes them into enterprise-owned concepts
- Scores them consistently
- Evolves safely over time

> Tools change. Signals remain. Debt interpretation evolves.

---

## 3. Mental Model (Very Important)

### The Fundamental Pipeline

```
Tool Metric → Signal → Signal Score → Debt Score → Application Score
```

### Key Principles
- Tools are replaceable
- Signals are owned by us
- Scoring is policy, not logic
- Configuration drives behavior
- Code remains generic and boring

---

## 4. Signals (The Most Important Concept)

### What Is a Signal?
A **signal** is a tool-independent statement about software health.

Examples:
- test_coverage
- bug_count
- maintainability_issues
- security_findings_high

Signals:
- Are neutral facts
- Do not contain weights
- Do not contain debt references
- Are reusable across multiple debts

> Signals are facts, not opinions.

---

## 5. Why Signals Do NOT Reference Debt

Signals answer:
> “What was observed?”

Debt answers:
> “What does it mean in context?”

Mixing them would:
- Break reusability
- Prevent evolution
- Lock assumptions permanently

Therefore:
- Signals remain debt-agnostic
- Debt association lives in configuration

---

## 6. Tool → Signal Mapping

### Purpose
Translate tool-specific metrics into enterprise-owned signals.

### Example: SonarQube

Sonar metric → Signal mapping:

```json
{
  "tool": "sonarqube",
  "tool_parameter": "coverage",
  "signal": "test_coverage",
  "status": "active"
}
```

### Key Characteristics
- Versioned
- Auditable
- Confidence-aware
- Replaceable without code changes

---

## 7. Adapter Layer (How Translation Happens)

### Adapter Responsibility
> Convert raw tool responses into normalized signals using configuration.

### Adapter Algorithm (Conceptual)

```
Load active mappings for tool
For each metric in tool response:
    If mapping exists:
        Emit signal
    Else:
        Ignore metric
```

### Adapter Must NOT
- Score signals
- Apply weights
- Know debt logic

Adapters translate — they do not think.

---

## 8. Signals Collection (Facts Layer)

### Purpose
Store **what was observed**.

### Example

```json
{
  "application_id": "app-123",
  "component_id": "payments-api",
  "signal": "test_coverage",
  "value": 62,
  "source_tool": "sonarqube",
  "collected_at": "2026-01-05T10:15:00Z"
}
```

This enables:
- Re-scoring
- Auditing
- Trend analysis

---

## 9. Signal Scoring Rules

### Purpose
Judge signal values consistently.

> “Given this signal value, how good or bad is it?”

### Example

```json
{
  "signal": "test_coverage",
  "scoring_type": "range",
  "ranges": [
    { "min": 80, "score": 100 },
    { "min": 60, "score": 60 },
    { "min": 0, "score": 30 }
  ]
}
```

### Important Rules
- One scoring rule per signal
- No tool names allowed
- Pure interpretation logic

---

## 10. Signal Scores

After scoring, signals become:

```json
{
  "test_coverage": 60,
  "bug_count": 50
}
```

Still:
- No debt
- No weighting
- Just judgement

---

## 11. Debt Signal Contributions (Critical Design)

### Purpose
Define:
1. Which signals contribute to a debt
2. How important each signal is *for that debt*

### Example: Code Debt

```json
{
  "debt_dimension": "code_debt",
  "signals": {
    "maintainability_issues": 0.35,
    "bug_count": 0.30,
    "test_coverage": 0.15,
    "security_findings_high": 0.20
  }
}
```

### Example: Test Debt

```json
{
  "debt_dimension": "test_debt",
  "signals": {
    "test_coverage": 0.60,
    "test_pass_rate": 0.40
  }
}
```

### Key Insight
The **same signal** can:
- Contribute to multiple debts
- With different weights

Weights are contextual, not intrinsic.

---

## 12. Debt Aggregation

### Responsibility
> Combine signal scores into a debt score.

### Example

```
code_debt =
(test_coverage × 0.15) +
(bug_count × 0.30) +
(maintainability × 0.35)
```

Debt is an **opinion**, not a fact.

---

## 13. Application-Level Weights

### Purpose
Reflect enterprise priorities.

### Example

```json
{
  "code_debt": 0.35,
  "test_debt": 0.25,
  "security_debt": 0.40
}
```

Leadership can change this without touching code.

---

## 14. Final Scores

### Stored Outcome

```json
{
  "application_id": "app-123",
  "overall_score": 55.8,
  "debt_scores": {
    "code_debt": 58.2,
    "test_debt": 65
  }
}
```

UI consumes this directly.

---

## 15. MongoDB Collections Summary

| Collection | Purpose |
|-----------|--------|
| applications | What exists |
| tool_signal_mappings | Tool → Signal translation |
| signals | Observed facts |
| signal_scoring_rules | How to judge signals |
| debt_signal_contributions | Signal → Debt weighting |
| debt_dimension_weights | Debt → App weighting |
| scores | Final outcomes |

---

## 16. End-to-End Runtime Flow

1. ETL fetches tool data
2. Adapter normalizes metrics → signals
3. Signals stored as facts
4. Scoring engine applies signal rules
5. Debt aggregator applies signal weights
6. App scorer applies debt weights
7. Results stored and shown in UI

Each step uses **only the configuration relevant to its responsibility**.

---

## 17. Final Architectural Laws

1. Signals are facts
2. Scoring is judgment
3. Debt is contextual
4. Weights are policy
5. Tools must disappear early
6. Code must not encode business meaning

If these laws hold, the platform will scale.

---

## 18. One-Sentence Summary

> We normalize tool data into signals, score signals consistently, interpret them contextually into debt, and aggregate debt into an explainable application health score — all driven by configuration, not code.
