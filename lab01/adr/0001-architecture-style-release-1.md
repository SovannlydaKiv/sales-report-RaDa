---
id: "0001"
title: "Architecture Style for Release 1"
date: 2026-10-09
status: Accepted
author: Kiv Sovannlyda (Member B)
---

# ADR-0001 – Architecture Style for Release 1

## Context

Angkor Mart head office requires a system that consolidates daily CSV sales
exports from three branch POS systems (Phnom Penh, Siem Reap, Battambang) into
a unified monthly report delivered to Finance Directors and Branch Managers.

### Key quality drivers evaluated

| Driver | Scenario |
|--------|----------|
| QA-1 Performance | Parse ≤ 3 M rows in ≤ 60 s (5 runs on reference hardware) |
| QA-4 Modifiability | Add a new renderer format in ≤ 2 developer-days |

### Candidate architectures

| ID | Style |
|----|-------|
| A  | **Modular Monolith Batch Job** (chosen) |
| B  | Event-Driven Microservices |
| C  | Multi-Process Pipeline |

---

## Decision

**Adopt the Modular Monolith Batch Job** as the architectural style for
Release 1.

The application is deployed as a single JVM process that executes on a
daily schedule. Internal structure is enforced by package-level dependency
rules (see `DependencyRulesTest`). Three packages form the logical modules:

```
edu.itc.salesreport.model    ← pure domain (no outbound deps)
edu.itc.salesreport.ingest   ← CSV loading (depends on model only)
edu.itc.salesreport.render   ← report output (depends on model only)
```

The entry point (`App.java`) is the only class that wires the three modules.

---

## Comparison Against Alternatives

### Option B – Event-Driven Microservices

| Aspect | Assessment |
|--------|-----------|
| ✅ Pro | Independent scaling of ingest vs. rendering workers |
| ✅ Pro | Natural fault isolation; one branch's failure does not block others |
| ❌ Con | Broker infrastructure (Kafka/RabbitMQ) adds significant operational cost |
| ❌ Con | At-least-once delivery semantics complicate idempotency (QA-3) |
| QA-1  | Potential throughput gains offset by serialization overhead |
| QA-4  | Adding a renderer requires a new microservice deployment |

### Option C – Multi-Process Pipeline

| Aspect | Assessment |
|--------|-----------|
| ✅ Pro | Parallelism is explicit; each stage is independently restartable |
| ✅ Pro | OS-level fault isolation |
| ❌ Con | Inter-process communication (pipes / temp files) adds I/O latency |
| ❌ Con | More complex deployment and monitoring than a single JVM |
| QA-1  | Additional marshalling overhead may negate parallelism gains |
| QA-4  | Adding a renderer requires a new process and pipeline reconfiguration |

---

## Rationale

### QA-1 (Performance)

A single JVM can process 3 M CSV rows in < 60 s on commodity hardware using
sequential streaming I/O with a `CsvTransactionParser` that avoids heap
pressure from object pooling. The batch completes within a predictable time
window (01:00–02:00 nightly schedule) without infrastructure dependencies.

### QA-4 (Modifiability)

The `ReportRenderer` **sealed interface** limits the closed set to
`TextReportRenderer`, `HtmlReportRenderer`, and `CsvReportRenderer`.
Adding a new format requires:
1. Implementing `ReportRenderer` in the `render` package (≤ 4 h).
2. Adding it to the `permits` clause (compiler-checked exhaustiveness).
3. Updating `forExtension()` (single-line change).

No infrastructure changes, no container rebuilds. This satisfies the
≤ 2 developer-day budget.

---

## Consequences

### Positive
- Simplest possible deployment: `mvn compile exec:java`.
- Compiler enforces the closed renderer set (QA-4 exhaustiveness guarantee).
- Zero external dependencies: suitable for air-gapped branch environments.
- Package dependency rules are machine-verifiable (`DependencyRulesTest`).

### Negative
- A single JVM is the scalability ceiling for Release 1; exceeding hardware
  limits will require migration to Option B or C (planned for Release 2).
- Adding a format requires recompilation and a new JAR deployment to every
  reporting server.

---

## Review Notes

> *[Space for Member A's review comments via GitHub PR]*

