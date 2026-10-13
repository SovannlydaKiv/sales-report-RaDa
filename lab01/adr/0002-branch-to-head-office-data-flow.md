# ADR-0002: Push daily branch CSV files via SFTP with idempotent partial-report fallback

Status: Accepted
Date: 2026-10-13 · Deciders: Inthera, Lyda

## Context
Each of the three Angkor Mart branches (`PNH`, `REP`, `BTB`) generates roughly 33,000 POS rows per day (~3,000,000 rows per month) and exports a daily CSV file (`BRANCH-yyyy-MM-dd.csv`) by 23:00. Branch internet links fail periodically (**QA-3**), yet head office requires the monthly report at 06:00 on day 2 of month M+1 within 60 seconds (**QA-1**) while ensuring branch integrity (**QA-5**). We need a reliable transport mechanism and a deterministic policy for late or re-uploaded files.

## Decision
We will have each branch POS push its daily CSV file over **SFTP** to the head-office landing zone (`data/<yyyy-MM>/<BRANCH>-<yyyy-MM-dd>.csv`) alongside a `.sha256` checksum. At 06:00 on day 2 of M+1, the batch job waits up to 45 minutes for any missing daily files; if a file is still missing at 06:45, the engine publishes a **partial report** clearly listing the missing branch/day in `anomalies` so the report is out before 07:00 (**QA-3**). When the late file arrives, the ingest job verifies its `(file_name, sha256)` key, replaces any prior file of the same name atomically, and triggers an idempotent full re-run.
The e-mail notification specifies whether the published report is partial or complete so the finance director is never misled.

## Alternatives considered
- **Head office pulls files from branch POS servers via HTTPS**: Attractive because head office controls the schedule, but rejected because branches lie behind NAT/firewalls over unstable ISP links and exposing inbound ports on POS terminals increases attack surface (**QA-5**).
- **POS publishes each receipt in real time to a cloud message broker (Kafka/RabbitMQ)**: Attractive for near-real-time visibility, but rejected because operating distributed broker clients on branch POS machines adds high operational complexity and per-message network overhead (**QA-1**).

## Consequences
+ **+ (QA-3)** A branch internet outage at 06:00 does not block PNH or BTB reports; partial reports ship by 07:00 and self-heal automatically upon upload.
+ **+ (QA-1, QA-6)** Local CSV files in the landing zone allow sequential disk reads at maximum throughput and deterministic file-based testing.
- **− (QA-3)** Stakeholders viewing the report between 07:00 and late-file recovery see provisional chain totals until the re-run completes.
- **− (QA-1)** Full idempotent re-runs recompute the entire month rather than applying incremental deltas, doubling CPU work on days with late uploads.

Revisit when: Branch count exceeds 10 or head office requests intraday hourly sales dashboards instead of a monthly batch.