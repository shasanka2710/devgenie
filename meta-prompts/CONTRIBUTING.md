# Contributing to the Meta-Prompt Library

Thank you for improving this library. The goal is to keep every prompt immediately useful for a Java principal engineer — not generic, not theoretical.

---

## Prompt Quality Standards

A prompt is **ready to merge** when it satisfies all of the following:

### 1. Specificity
- References at least one concrete technology from the stack (Spring Boot, Kafka, Flink, MongoDB, OCP, Harness, Vault, Istio, SonarQube, Snyk).
- Avoids generic advice like "follow best practices." Instead, name the practice (e.g., "apply Bulkhead pattern via Resilience4j").
- Includes version-specific nuances where they materially affect the output (e.g., Spring Boot 3.x native image constraints, Kafka 3.x KRaft mode).

### 2. Parameterization
- Every variable piece of context uses a `[PLACEHOLDER]` marker in ALL_CAPS with underscores (e.g., `[SERVICE_NAME]`, `[KAFKA_TOPIC_NAME]`).
- The **Prerequisites** section must list every placeholder and explain what information is needed to fill it.
- No placeholder should be ambiguous — add an inline example if needed (e.g., `[THROUGHPUT_TPS e.g. 5000]`).

### 3. Output Structure
- The **Expected Output** section must specify the exact format: table columns, scorecard dimensions, runbook step numbering, etc.
- The prompt text itself must instruct the LLM to produce that format (e.g., "Return a markdown table with columns: …").
- L1 prompts: single structured block. L2 prompts: 2-4 structured sections. L3 prompts: full document with ToC.

### 4. Front-matter Completeness
Every file must have a valid YAML front-matter block:

```yaml
---
name: [Prompt Name]
domain: [architecture | code-quality | security | data-streaming | devops-platform | observability | documentation | ai-llm]
complexity: [L1 | L2 | L3]
output-format: [markdown-table | scorecard | runbook | adr | narrative]
token-estimate: [low | medium | high]
tags: [comma-separated tags]
---
```

Allowed `output-format` values: `markdown-table`, `scorecard`, `runbook`, `adr`, `narrative`.  
Token estimate guideline: low < 1 500 tokens output, medium 1 500–4 000, high > 4 000.

---

## Adding a New Prompt — Step-by-Step

1. **Pick the right domain folder.** If none fit, propose a new folder in the PR description.
2. **Copy the template** from the README and name the file `kebab-case-description.md`.
3. **Fill every section** — no section may be left as a placeholder or TODO.
4. **Self-review checklist** before opening a PR:
   - [ ] Front-matter is valid YAML.
   - [ ] All `[PLACEHOLDER]` values are listed in Prerequisites.
   - [ ] The Prompt section instructs the LLM on the exact output format.
   - [ ] Expected Output describes columns / dimensions / sections explicitly.
   - [ ] At least two Related Prompts are listed (or a note explaining why none exist).
   - [ ] You have tested the prompt (see Testing section below).
5. **Open a PR** against `main`. Title format: `[domain] Add <prompt-name>`.
6. **Request review** from at least one principal engineer.

---

## Review Process

| Step | Owner | Criteria |
|---|---|---|
| Automated lint | CI | Front-matter parseable, no broken internal links |
| Domain review | Domain champion | Accuracy, specificity, placeholder completeness |
| Cross-domain review | Any principal engineer | Overlap with existing prompts, Related Prompts correct |
| Merge | Maintainer | All checks green, at least one approval |

Reviewers should run the prompt against a real scenario before approving (see Testing below).

---

## Versioning Convention

Prompt files are versioned via Git. For breaking changes to an existing prompt (changed placeholders, changed output format), increment the version in a comment at the top of the file:

```
<!-- version: 1.2.0 -->
```

Version format: `MAJOR.MINOR.PATCH`
- **MAJOR** — output format changes or placeholder set changes (consumers must update).
- **MINOR** — new optional section, additional guidance, improved specificity.
- **PATCH** — typo fixes, wording clarifications, minor example updates.

Do **not** rename the file when bumping versions; use Git history.

---

## Testing — How to Validate a Prompt

A prompt is validated when a real LLM run produces an output that satisfies the Expected Output description. Follow these steps:

### Step 1 — Prepare Test Input
- Fill every `[PLACEHOLDER]` with realistic values from a representative project.
- Use the same LLM model consistently across the library (preferred: GPT-4o or Claude 3.5 Sonnet at default temperature).

### Step 2 — Run and Capture Output
- Paste the completed prompt into the LLM.
- Save the raw output in `/tmp/prompt-test-<prompt-name>-<date>.md` for your reference (do not commit raw LLM outputs).

### Step 3 — Score the Output
Evaluate the output against these dimensions (score 1–5 each):

| Dimension | Criteria |
|---|---|
| Specificity | Output references stack-specific technologies, not generic patterns |
| Completeness | All expected sections / table columns are present |
| Actionability | A team could act on the output with no further research |
| Format compliance | Matches the declared `output-format` exactly |

A prompt passes validation when **all four dimensions score ≥ 4**.

### Step 4 — Document Results
Add a one-line test note in the PR description:

```
Tested with GPT-4o on 2025-04-01. All dimensions scored ≥ 4. Output attached as PR comment.
```

---

## Deprecating a Prompt

If a prompt becomes obsolete (e.g., technology removed from the stack):
1. Add a deprecation notice at the top of the file: `> ⚠️ **Deprecated** as of [DATE]. Replaced by [LINK].`
2. Update the README index to mark it deprecated.
3. Open a PR; the file is removed after one release cycle.
