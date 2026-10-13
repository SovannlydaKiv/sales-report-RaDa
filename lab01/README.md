# Lab-01: architecture foundations of the sales report system

Team: RaDa · Members: <Chiv Inthera (p20240019)>, <Kiv Sovannlyda (p20240001)>

## Files produced in this lab

| File | Purpose | Author(s) |
|------|---------|-----------|
| `lab01/quality-scenarios.md` | QA-1..QA-6, utility tree, drivers | Member A (QA-1..3), Member B (QA-4..6, tree) |
| `lab01/c4-context.mmd`, `lab01/c4-context.png` | C4 level 1 system context diagram | Member A |
| `lab01/c4-container.mmd`, `lab01/c4-container.png` | C4 level 2 container diagram, release 1 | Member B |
| `lab01/c4-notes.md` | Context and container rationale, QA mapping, client questions | Member A, Member B |
| `lab01/design-review.md` | Package cohesion, Ca/Ce/I coupling arithmetic, sealed vs open review | Member B |
| `lab01/adr/0001-architecture-style-release-1.md` | ADR-0001: Modular monolith batch style | Member B (reviewed by Member A) |
| `lab01/adr/0002-branch-to-head-office-data-flow.md` | ADR-0002: SFTP push and partial-report failure policy | Member A (reviewed by Member B) |
| `lab01/atam.md` | Challenge: ATAM-style trade-off analysis of candidates A, B, C | Member A, Member B |
| `pom.xml`, `src/**`, `tools/SampleData.java` | `sales-report` 0.1.0-SNAPSHOT model, parser, renderers, loader, tests | Member A, Member B |

## Inputs reused from earlier labs
None (first lab).

## Change log

| Date | Lab | Change | Commit or tag |
|------|-----|--------|---------------|
| 2026-10-13 | Lab-01 | Project created: model, parser, renderers, loader; 2 ADRs and ATAM analysis | `lab-01` |