---
name: MongoDB Schema Review
domain: data-streaming
complexity: L2
output-format: scorecard
token-estimate: medium
tags: mongodb, schema-design, indexing, spring-data, document-model, aggregation, java
---

<!-- version: 1.0.0 -->

## When to Use

Use this prompt when reviewing or evolving a MongoDB collection schema for a Spring Boot service. Ideal before introducing a new collection, when query performance degrades, or when a schema must be evolved without downtime in a production system.

## Prerequisites

- `[COLLECTION_NAME]` — MongoDB collection name.
- `[CURRENT_SCHEMA]` — Paste the current document structure as a JSON/BSON example, or describe fields.
- `[ACCESS_PATTERNS]` — List of the 5 most important query patterns (e.g., "find orders by customerId sorted by createdAt DESC, limit 20").
- `[WRITE_TPS]` — Write operations per second.
- `[READ_TPS]` — Read operations per second.
- `[DOCUMENT_SIZE_KB]` — Average document size in KB.
- `[MONGODB_VERSION]` — MongoDB version (e.g., 7.0).
- `[SPRING_DATA_MONGODB_VERSION]` — Spring Data MongoDB version.
- `[EXISTING_INDEXES]` — List of existing indexes (name, fields, options), or "none."
- `[SHARDING_ENABLED]` — Whether MongoDB sharding is enabled (yes/no) and shard key if known.
- `[SCHEMA_VALIDATION_ENABLED]` — Whether MongoDB JSON Schema validation is configured (yes/no).

## The Prompt

```
You are a principal engineer expert in MongoDB schema design and Spring Data MongoDB.

Context:
- Collection: [COLLECTION_NAME]
- Current schema: [CURRENT_SCHEMA]
- Access patterns: [ACCESS_PATTERNS]
- Write TPS: [WRITE_TPS], Read TPS: [READ_TPS]
- Average document size: [DOCUMENT_SIZE_KB] KB
- MongoDB version: [MONGODB_VERSION]
- Spring Data MongoDB: [SPRING_DATA_MONGODB_VERSION]
- Existing indexes: [EXISTING_INDEXES]
- Sharding: [SHARDING_ENABLED]
- Schema validation: [SCHEMA_VALIDATION_ENABLED]

Task:
Produce a MongoDB Schema Review with the following sections:

## 1. Schema Design Scorecard
Score the current schema against MongoDB design principles (1–5, 5 = best practice):
| Dimension | Score | Finding | Recommendation |
Dimensions:
- Embedding vs referencing decisions (follow access patterns, avoid unbounded arrays)
- Document size (< 16 MB hard limit, < 1 MB soft recommendation)
- Field naming (camelCase consistency, no dots or dollar signs)
- Required field presence and defaults
- Polymorphic pattern usage (appropriate or accidental)
- Temporal fields (createdAt, updatedAt as Date, not string)
- Audit fields (createdBy, updatedBy if applicable)

## 2. Access Pattern Coverage Analysis
For each access pattern in [ACCESS_PATTERNS]:
| Access Pattern | Current Index Support | Query Type (find/aggregate) | Covered by Index? | Sort Supported? | Collection Scan Risk? | Recommendation |

## 3. Index Design Recommendations
| Index Name | Fields (in order) | Type (single/compound/text/geo/TTL) | Unique? | Sparse? | Partial Filter | Covers Access Pattern(s) | ESR Rule Applied? |

For each recommended index, provide the Spring Data MongoDB @Indexed / @CompoundIndex annotation AND the equivalent MongoDB shell command.

Flag any indexes in [EXISTING_INDEXES] that are:
- Redundant (prefix of a compound index)
- Never used (based on access pattern analysis)
- Missing for critical access patterns

## 4. Aggregation Pipeline Review
For the top 2 most complex access patterns from [ACCESS_PATTERNS], provide an optimized aggregation pipeline:
- Explain why $match is first (index usage)
- Place $project before $lookup to reduce document size
- Recommend $hint if query planner might choose a suboptimal index
Return as MongoDB shell aggregation code blocks.

## 5. Schema Validation
If [SCHEMA_VALIDATION_ENABLED] is no, provide a MongoDB JSON Schema validation document for the collection:
- Required fields
- Type enforcement for all fields
- Enum constraints where applicable
- Return as a db.runCommand({ collMod: ... }) code block.

## 6. Spring Data MongoDB Mapping Review
Review the Java @Document class implied by [CURRENT_SCHEMA]:
| Java Field | MongoDB Field | Annotation | Issue | Recommendation |
Check for:
- Missing @Field("fieldName") on non-matching Java names
- @DBRef usage (prefer manual references or embedded documents for performance)
- Missing @Indexed annotations matching index recommendations
- @Version for optimistic locking
- @CreatedDate / @LastModifiedDate via @EnableMongoAuditing

Provide a corrected Java @Document class as a code block.

## 7. Sharding Strategy Assessment
If [SHARDING_ENABLED] is yes:
| Concern | Current State | Recommendation |
| Shard key cardinality | | |
| Shard key monotonicity (avoid ObjectId as shard key) | | |
| Chunk distribution | | |
| Targeted vs scatter-gather queries | | |

## 8. Schema Evolution Strategy
For the next anticipated schema change (add a field, rename, remove):
- Lazy migration pattern (handle missing field in Java with @BsonProperty defaults)
- Bulk migration script approach (aggregation $merge)
- Zero-downtime migration steps (numbered)
- Spring Boot compatibility across old and new schema versions

## 9. Priority Recommendations
| Rank | Recommendation | Impact (H/M/L) | Effort (H/M/L) | Quick Win? |

Use MongoDB [MONGODB_VERSION] syntax. Reference Spring Data MongoDB [SPRING_DATA_MONGODB_VERSION] annotations. Flag any usage of deprecated MongoDB APIs.
```

## Expected Output

A structured schema review containing:
- Schema design scorecard (7 dimensions)
- Access pattern coverage table
- Index design table + Spring Data annotations + shell commands
- Aggregation pipeline code blocks (top 2 patterns)
- JSON Schema validation code block
- Spring Data @Document Java class review table + corrected code block
- Sharding strategy table (if applicable)
- Schema evolution runbook (numbered steps)
- Ranked priority recommendations

## Benefits

- Identifies collection scans hidden in access patterns that cause gradual performance degradation.
- Produces ready-to-apply index creation commands and Spring Data annotations.
- Provides a zero-downtime schema evolution plan that prevents application errors during migrations.

## Related Prompts

- [kafka-topic-design.md](kafka-topic-design.md) — Design the Kafka source for this MongoDB sink.
- [flink-pipeline-design.md](flink-pipeline-design.md) — Design the Flink pipeline that writes to this collection.
- [../code-quality/spring-boot-code-review.md](../code-quality/spring-boot-code-review.md) — Review the Spring Data repository layer.
