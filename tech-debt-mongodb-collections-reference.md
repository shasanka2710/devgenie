
# Tech Debt Platform – MongoDB Collections Reference Guide

> This document explains **each MongoDB collection used in the Tech Debt Platform**,
> its **responsibility**, and a **field-by-field explanation**.
>
> It is written slowly and deliberately, to support:
> - Architects
> - Backend developers
> - Platform maintainers
>
> This document should be treated as a **schema contract**, not casual notes.

---

## 1. Summary – MongoDB Collections & Responsibilities

| Collection Name | Primary Responsibility |
|-----------------|------------------------|
| applications | Defines what exists in the enterprise |
| tool_signal_mappings | Translates tool metrics into signals |
| signals | Stores observed, normalized facts |
| signal_scoring_rules | Defines how to score signals |
| debt_signal_contributions | Defines which signals contribute to which debt and with what weight |
| debt_dimension_weights | Defines importance of each debt dimension |
| scores | Stores final computed outcomes |

---

## 2. `applications` Collection

### Purpose
Defines **what applications and components exist** and should be scanned.

This collection answers the question:
> “What are we analyzing?”

This data is **organizational**, not analytical.

---

### Schema & Field Explanation

```json
{
  "_id": "app-123",
  "name": "payments-platform",
  "owner": "payments-team",
  "components": [
    {
      "component_id": "payments-api",
      "repo": "github.com/org/payments-api",
      "language": "java"
    }
  ],
  "active": true
}
```

| Field | Description | Expected Values | Why It Exists |
|------|------------|----------------|---------------|
| _id | Unique app identifier | String / UUID | Primary reference |
| name | Human-readable app name | String | UI & reports |
| owner | Owning team | Team name | Accountability |
| components | Deployable units | Array | Granular scoring |
| component_id | Component identifier | String | Join key |
| repo | Source repository | URL | Tool integration |
| language | Primary language | Enum/string | Tool selection |
| active | Scan eligibility | true/false | Control scanning |

---

## 3. `tool_signal_mappings` Collection

### Purpose
Defines **how tool-specific metrics are translated into enterprise signals**.

This is the **most critical configuration** in the system.

It answers:
> “When a tool says X, what do WE call it?”

---

### Schema

```json
{
  "_id": "sonar-coverage-v1",
  "tool": "sonarqube",
  "tool_parameter": "coverage",
  "signal": "test_coverage",
  "debt_dimension_hint": "code_debt",
  "confidence": 0.8,
  "rationale": "Coverage indicates test safety",
  "status": "active",
  "version": 1,
  "effective_from": "2025-01-01",
  "effective_to": null
}
```

| Field | Description | Expected Values | Purpose |
|------|------------|----------------|---------|
| tool | Source tool name | sonarqube, checkmarx | Lookup key |
| tool_parameter | Metric name | As per tool API | Translation trigger |
| signal | Normalized signal name | Stable identifier | Core concept |
| debt_dimension_hint | Suggested debt | Optional | UI hints |
| confidence | Trust factor | 0–1 | Noise control |
| rationale | Why mapping exists | Text | Auditability |
| status | Lifecycle state | active/deprecated | Safe evolution |
| version | Mapping version | Integer | Change tracking |
| effective_from | Start date | Date | Time-based logic |
| effective_to | End date | Date/null | Deprecation |

⚠️ Adapter logic **only reads active mappings**.

---

## 4. `signals` Collection

### Purpose
Stores **normalized observations (facts)**.

Signals answer:
> “What was observed, and when?”

Signals are **not opinions**.

---

### Schema

```json
{
  "_id": "sig-001",
  "application_id": "app-123",
  "component_id": "payments-api",
  "signal": "test_coverage",
  "value": 62,
  "unit": "percentage",
  "source_tool": "sonarqube",
  "collected_at": "2026-01-05T10:15:00Z"
}
```

| Field | Description | Expected Values | Purpose |
|------|------------|----------------|---------|
| application_id | App reference | ID | Traceability |
| component_id | Component reference | ID | Granularity |
| signal | Signal name | From mapping | Join key |
| value | Observed value | Number | Scoring input |
| unit | Measurement unit | %, count | Interpretation |
| source_tool | Origin tool | Tool name | Debugging |
| collected_at | Timestamp | ISO date | History |

---

## 5. `signal_scoring_rules` Collection

### Purpose
Defines **how signal values are judged**.

Answers:
> “Given this signal value, how good or bad is it?”

---

### Schema

```json
{
  "_id": "rule-test-coverage-v1",
  "signal": "test_coverage",
  "scoring_type": "range",
  "ranges": [
    { "min": 80, "score": 100 },
    { "min": 60, "score": 60 },
    { "min": 0, "score": 30 }
  ],
  "status": "active",
  "version": 1
}
```

| Field | Description | Expected Values | Purpose |
|------|------------|----------------|---------|
| signal | Signal name | Existing signal | Rule binding |
| scoring_type | Rule model | range/inverse | Generic logic |
| ranges | Score brackets | min/score | Deterministic |
| max_acceptable | Threshold | Number | Inverse logic |
| status | Rule state | active | Control |
| version | Rule version | Integer | Audit |

---

## 6. `debt_signal_contributions` Collection

### Purpose
Defines **which signals contribute to which debt and with what weight**.

Answers:
> “Which signals matter for this debt, and how much?”

---

### Schema

```json
{
  "_id": "code-debt-signals-v1",
  "debt_dimension": "code_debt",
  "signals": {
    "maintainability_issues": 0.35,
    "bug_count": 0.30,
    "test_coverage": 0.15,
    "security_findings_high": 0.20
  },
  "version": 1,
  "status": "active"
}
```

| Field | Description | Expected Values | Purpose |
|------|------------|----------------|---------|
| debt_dimension | Debt identifier | code_debt | Aggregation key |
| signals | Signal-weight map | signal → weight | Contextual weighting |
| version | Policy version | Integer | Evolution |
| status | Lifecycle | active | Safe rollout |

✔ Same signal can appear in multiple documents with different weights.

---

## 7. `debt_dimension_weights` Collection

### Purpose
Defines **importance of each debt dimension in final score**.

Answers:
> “What matters more to the business?”

---

### Schema

```json
{
  "_id": "debt-weights-v1",
  "weights": {
    "code_debt": 0.35,
    "test_debt": 0.25,
    "security_debt": 0.40
  },
  "effective_from": "2026-01-01"
}
```

| Field | Description | Expected Values | Purpose |
|------|------------|----------------|---------|
| weights | Debt-weight map | debt → weight | Final aggregation |
| effective_from | Start date | Date | Versioning |

---

## 8. `scores` Collection

### Purpose
Stores **final computed outcomes**.

Answers:
> “What is the current health score?”

---

### Schema

```json
{
  "_id": "score-001",
  "application_id": "app-123",
  "component_id": "payments-api",
  "signal_scores": {
    "test_coverage": 60,
    "bug_count": 50
  },
  "debt_scores": {
    "code_debt": 58.2,
    "test_debt": 65
  },
  "overall_score": 55.8,
  "calculated_at": "2026-01-05T10:20:00Z"
}
```

| Field | Description | Purpose |
|------|------------|---------|
| signal_scores | Scored signals | Drill-down |
| debt_scores | Debt outcomes | Explainability |
| overall_score | Final score | Ranking |
| calculated_at | Timestamp | History |

---

## 9. Final Design Principle (Read Twice)

> **Collections store decisions.  
> Layers apply logic.  
> Configuration drives meaning.**

---

END OF DOCUMENT
