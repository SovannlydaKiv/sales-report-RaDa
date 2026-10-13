# Quality-attribute scenarios: Angkor Mart monthly sales report

## Stakeholders
- **Head-office analyst**: Views and exports monthly branch and chain-wide sales reports in PDF and Excel formats.
- **Branch manager (PNH, REP, BTB)**: Views monthly sales performance and anomalies strictly for their own assigned branch via a web page.
- **Finance director**: Receives automated e-mail notifications when the monthly report is validated and ready.
- **Operations person**: Schedules, monitors, and troubleshoots the nightly CSV ingestion and monthly report batch execution.
- **Development team**: Designs, tests, and maintains the `sales-report` system across releases.

## Scenarios

| ID | Attribute | Source | Stimulus | Artifact | Environment | Response | Response measure | Rank |
|----|-----------|--------|----------|----------|-------------|----------|------------------|------|
| QA-1 | Performance | Scheduler, 06:00 on day 2 of M+1 | Starts the monthly report calculation for ~3,000,000 rows (90 CSV files) | Report engine | Normal operation, 8-core server with NVMe SSD, all branch CSV files present | All rows parsed, aggregated with `BigDecimal`, report files written, and "ready" event published | <= 60 s wall-clock time, median of 5 runs after 2 JVM warm-up runs | (H,H) |
| QA-2 | Scalability | Angkor Mart management | Opens a 4th branch (adds ~1,000,000 rows/month, increasing load by 33%) | Report engine and ingestion pipeline | Normal operation on existing 8-core server hardware | Processes all 4 branches (~4,000,000 rows) without out-of-memory errors or code changes | Total wall-clock run time increases by <= 35% (<= 81 s median over 5 runs) | (M,M) |
| QA-3 | Availability | Branch internet link (REP) | Network outage delays REP daily CSV upload by 2 hours past the 06:00 deadline on day 2 of M+1 | Ingestion job and report engine | Degraded network mode; PNH and BTB files are complete in landing zone | Publishes partial report for PNH and BTB on schedule; automatically merges REP and re-runs idempotently when file arrives | Partial report published by 07:00; full report updated within <= 5 min of late file arrival with 0 duplicate rows | (H,M) |

## Rank justifications
- **QA-1 (H,H)**: Head office requires the report on the morning of day 2, and processing 3 M rows using exact `BigDecimal` arithmetic and disk I/O requires careful memory and threading design.
- **QA-2 (M,M)**: Expanding to new branches is planned for future growth, though current 3-branch volume is fixed for release 1.
- **QA-3 (H,M)**: Branch internet links fail periodically according to the brief, so a single missing file must not block head office from viewing the remaining branches.

## Assumptions
- **A1**: The production report server has 8 CPU cores, 16 GB RAM, and local SSD storage.
- **A2**: Daily CSV files average 33,000 rows (~2.8 MB per file, ~250 MB per month across 3 branches).