# ADR-0001: Use a modular monolith batch architecture for release 1

Status: Accepted
Date: 2026-10-13 · Deciders: Inthera, Lyda

## Context
Release 1 of the Angkor Mart monthly sales report system must process ~3,000,000 rows per month in 60 seconds or less (**QA-1**), tolerate late branch files (**QA-3**), allow new output renderers in under 2 days (**QA-4**), and run unit tests in under 30 seconds (**QA-6**). Our team consists of two developers building on Java 25 and Maven 3.9 with a strict eight-week schedule.

## Decision
We will build Release 1 as a **modular monolith batch application** executing inside a single JVM, structured into strictly layered packages (`model`, `ingest`, `engine`, `render`, `ui`) enforced by automated build checks (`DependencyRulesTest`). All parsing, aggregation, and rendering occur via in-process method calls over immutable Java 25 records.

## Alternatives considered
- **Multi-process pipeline (coordinator and worker processes over pipes/sockets)**: Attractive for process-level fault isolation (prepared in Lab-05), but rejected for Release 1 because inter-process serialization of 3,000,000 `SaleTransaction` records adds unnecessary IPC latency (**QA-1**) and debugging complexity.
- **Event-driven microservices with a message broker**: Attractive for independent horizontal scaling (**QA-2**), but rejected because broker serialization, network hops, and multi-service deployment jeopardize the 60-second batch target (**QA-1**) and exceed a two-person team's operational capacity.

## Consequences
+ **+ (QA-1)** Zero network or IPC serialization overhead between parsing, aggregation, and rendering, easily meeting the 60-second wall-clock requirement on an 8-core server.
+ **+ (QA-4, QA-6)** Sealed Strategy interfaces (`ReportRenderer`) and Observer listeners (`LoadListener`) are verified at compile time and tested in milliseconds with `mvn test`.
- **− (QA-2)** Vertical scaling is bounded by a single machine's CPU cores and JVM heap size.
- **− (QA-3)** An unhandled out-of-memory error or JVM crash terminates the entire batch job rather than isolating failure to a single stage.

Revisit when: Monthly transaction volume exceeds 10,000,000 rows or external systems require real-time RPC/REST queries against live aggregations.

## Decision 
- The core model package maintains zero outgoing project dependencies (I = 0) so future desktop and web UI containers can reuse it directly.