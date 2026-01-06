
# Tech Debt Platform – Developer Playbook

## 1. Purpose of This Playbook
This playbook is a **practical, developer-facing guide** for working on the Tech Debt Platform.

It explains:
- How the system is structured
- What each layer is responsible for
- How to add or modify integrations safely
- What NOT to do

This is **not** an architecture debate document.
This is **how you build and extend the system correctly**.

---

## 2. Core Rule (Memorize This)

> **Never encode business meaning in code.  
> All meaning lives in configuration.**

---

## 3. System Overview (Developer Mental Model)

```
Tool → Adapter → Signal → Score → Debt → App Score → UI
```

---

## 4. Layer-by-Layer Responsibilities

| Layer | Responsibility | Do | Do NOT |
|----|----|----|----|
| ETL | Fetch raw tool data | Call APIs | Interpret data |
| Adapter | Tool → Signal | Use mappings | Hard-code metrics |
| Signals | Store facts | Persist raw signals | Add weights |
| Scoring | Signal → Score | Apply rules | Know tools |
| Debt | Signals → Debt | Apply weights | Read raw metrics |
| App Score | Debt → Final | Aggregate | Guess meaning |
| UI | Visualization | Display results | Recalculate scores |

---

## 5. MongoDB Collections (What You Touch)

### applications
Defines what exists.
```json
{ "application_id": "app-123", "components": [] }
```

### tool_signal_mappings
Defines how tools translate to signals.
```json
{
  "tool": "sonarqube",
  "tool_parameter": "coverage",
  "signal": "test_coverage",
  "status": "active"
}
```

### signals
Stores observed facts.
```json
{
  "signal": "test_coverage",
  "value": 62
}
```

### signal_scoring_rules
Defines how to score signals.
```json
{
  "signal": "test_coverage",
  "scoring_type": "range"
}
```

### debt_signal_contributions
Defines signal importance per debt.
```json
{
  "debt_dimension": "code_debt",
  "signals": { "bug_count": 0.3 }
}
```

### debt_dimension_weights
Defines debt importance.
```json
{
  "code_debt": 0.35
}
```

### scores
Stores final results.

---

## 6. Adding a New Tool (Step-by-Step)

### Step 1: Identify Signals
Decide what risk the tool represents.

### Step 2: Add Tool → Signal Mappings
Add entries to `tool_signal_mappings`.

### Step 3: Adapter Automatically Works
No code change required if mappings exist.

### Step 4: Verify Signal Scoring Rules
Ensure rules exist for new signals.

### Step 5: Update Debt Signal Contributions
Decide how strongly the signal affects debt.

---

## 7. Changing Logic Without Code

| Change Needed | Update Collection |
|-------------|------------------|
| Metric meaning | tool_signal_mappings |
| Signal scoring | signal_scoring_rules |
| Signal importance | debt_signal_contributions |
| Debt priority | debt_dimension_weights |

---

## 8. Common Mistakes

❌ Hard-coding tool logic  
❌ Adding weights inside signals  
❌ Duplicating signals  
❌ Deleting mappings instead of deprecating

---

## 9. Debugging Guide

- Missing signal → Check mappings
- Wrong score → Check scoring rules
- Wrong debt → Check debt_signal_contributions

---

## 10. Golden Rules

1. Adapters translate, they don’t think
2. Signals are facts
3. Debt is contextual
4. Configuration drives behavior

---

END OF PLAYBOOK
