---
name: Prompt Engineering for Code
domain: ai-llm
complexity: L1
output-format: narrative
token-estimate: low
tags: prompt-engineering, llm, code-generation, java, spring-boot, few-shot, chain-of-thought
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt when you need to craft or improve a prompt for a code generation, code review, or code transformation task involving Java/Spring Boot. Ideal when a prompt is producing inconsistent, incomplete, or incorrect output, or when building a new AI-assisted engineering workflow from scratch.

## Prerequisites

- `[TASK_TYPE]` — What the prompt is for: code generation / code review / refactoring / test generation / documentation / debugging / SQL/MongoDB query generation.
- `[TARGET_TECHNOLOGY]` — Technology context (e.g., Spring Boot 3.2, Java 21, Kafka 3.6, Flink 1.18, MongoDB 7.0).
- `[EXISTING_PROMPT]` — The current prompt that needs improvement, or "new prompt."
- `[PROBLEM_WITH_EXISTING]` — What is wrong with the current output (too verbose, missing error handling, wrong framework version, hallucinating APIs), or "n/a."
- `[OUTPUT_FORMAT_DESIRED]` — What format the output should be in (Java code block, markdown table, JSON, numbered steps).
- `[QUALITY_CRITERIA]` — What makes a good output for this task (e.g., compilable, uses constructor injection, includes unit test, follows Conventional Commits).
- `[EXAMPLE_GOOD_OUTPUT]` — A short example of what a good output looks like, or "none."

## The Prompt

```
You are a principal engineer expert in prompt engineering for software development tasks.

Context:
- Task type: [TASK_TYPE]
- Technology: [TARGET_TECHNOLOGY]
- Existing prompt: [EXISTING_PROMPT]
- Problem with existing output: [PROBLEM_WITH_EXISTING]
- Desired output format: [OUTPUT_FORMAT_DESIRED]
- Quality criteria: [QUALITY_CRITERIA]
- Example good output: [EXAMPLE_GOOD_OUTPUT]

Task:
Produce a Prompt Engineering Guide for this specific task with the following sections:

## 1. Prompt Quality Diagnosis
If [EXISTING_PROMPT] is provided, diagnose its weaknesses:
| Weakness | Evidence | Impact on Output | Fix |
Common weaknesses to check:
- Missing role definition (LLM doesn't know it's a Java expert)
- Ambiguous task (no specific output format instructed)
- Missing technology version (LLM defaults to older APIs)
- No quality constraints (LLM skips error handling, logging)
- Missing negative instructions (what NOT to do)
- No few-shot examples (LLM guesses output style)
- Asking for too much in one prompt (exceeds context or focus)

## 2. Prompt Engineering Principles Applied
Apply these principles to [TASK_TYPE] for [TARGET_TECHNOLOGY]:

### Principle 1: Persona + Context First
Write a persona line and context block:
```
[IMPROVED_PERSONA_CONTEXT]
```

### Principle 2: Explicit Output Format
Write the output format instruction:
```
[OUTPUT_FORMAT_INSTRUCTION]
```

### Principle 3: Quality Constraints
List the quality constraints to include in the prompt:
| Constraint | Instruction Text to Add |
| Use Spring Boot [version] APIs | "Use Spring Boot 3.x APIs only. Do not use deprecated Spring 5 APIs." |
| Constructor injection | "Use constructor injection only, never @Autowired on fields." |
| Logging | "Include SLF4J logger with structured log statements at INFO and ERROR levels." |
| Exception handling | "Handle all checked exceptions. Do not swallow exceptions." |
| Java version | "Use Java 21 features where appropriate (records, sealed classes, pattern matching)." |
| Test | "Include a JUnit 5 test class with @ExtendWith(MockitoExtension.class)." |

### Principle 4: Chain of Thought (for complex tasks)
If [TASK_TYPE] is complex (code review, architecture, debugging):
Add a chain-of-thought instruction:
```
[CHAIN_OF_THOUGHT_INSTRUCTION]
```
Example: "Before writing the code, reason step by step: 1) What is the input type? 2) What are the edge cases? 3) What Spring beans are needed? Then write the code."

### Principle 5: Few-Shot Examples
Provide 1–2 few-shot examples in the prompt structure:
```
[FEW_SHOT_EXAMPLE]
```

### Principle 6: Negative Instructions
List what the LLM should NOT do:
```
[NEGATIVE_INSTRUCTIONS]
```
Example: "Do not use @Autowired field injection. Do not use System.out.println. Do not import com.sun.* packages. Do not generate placeholder TODO comments."

## 3. Improved Prompt
Provide the complete, improved prompt incorporating all principles:

```
[COMPLETE_IMPROVED_PROMPT]
```

## 4. Prompt Variants
Provide 3 variants of the improved prompt for different contexts:
| Variant | Use Case | Key Difference from Base Prompt |
| Concise | Quick code snippets, token-limited context | Shorter, fewer constraints |
| Detailed | Full feature implementation | More few-shot examples, more quality constraints |
| Review mode | Code review / audit tasks | Adds scoring rubric, structured JSON output |

## 5. Prompt Testing Checklist
Before using this prompt in a pipeline:
- [ ] Test with a trivial input — does output format match expectation?
- [ ] Test with a complex input — does reasoning stay grounded?
- [ ] Test with an edge case — does the LLM say "I don't know" vs hallucinate?
- [ ] Test with an adversarial input — does prompt injection attempt change behavior?
- [ ] Compare output across 3 LLM runs — is it consistent?
- [ ] Verify output compiles (for code generation tasks)
- [ ] Measure token usage — is it within budget?

## 6. Prompt Maintenance Guidelines
| Trigger | Action |
| LLM model upgrade | Re-test all prompts with new model, update version-specific instructions |
| Technology version change | Update [TARGET_TECHNOLOGY] references and few-shot examples |
| Quality regression (output worsens) | Add more specific negative instructions or few-shot examples |
| New anti-pattern emerges | Add to negative instructions and quality constraints |
| Prompt length exceeds 2000 tokens | Split into specialized sub-prompts |

Provide specific, immediately usable prompt text — not abstract advice. Every instruction in the improved prompt must have a clear reason tied to [TARGET_TECHNOLOGY] behavior.
```

## Expected Output

A complete prompt engineering guide containing:
- Weakness diagnosis table for the existing prompt
- 6 engineering principles applied with generated prompt sections
- A complete, copy-paste improved prompt
- 3 prompt variants (concise, detailed, review mode) with use cases
- Prompt testing checklist
- Prompt maintenance guidelines table

## Benefits

- Transforms a mediocre, inconsistent prompt into a precise, reliable one by applying systematic prompt engineering principles.
- Technology-specific quality constraints (Spring Boot version, Java idioms) prevent the most common AI code generation failures.
- Prompt testing checklist ensures consistency before the prompt enters a production pipeline.

## Related Prompts

- [ai-code-review-integration.md](ai-code-review-integration.md) — Apply improved prompts in the AI code review pipeline.
- [rag-pipeline-design.md](rag-pipeline-design.md) — Use RAG to enrich prompts with internal context.
- [../code-quality/spring-boot-code-review.md](../code-quality/spring-boot-code-review.md) — Human review criteria that should inform AI prompt quality constraints.
