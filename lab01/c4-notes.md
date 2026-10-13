# C4 Architecture Diagram Notes

## Level 1: System Context Notes
1. **What this diagram deliberately omits**: It hides internal containers, data stores, scheduling mechanisms, and network protocols to focus strictly on user roles and external system boundaries.
2. **Quality-attribute linkage**: The boundary between the three Branch POS Systems and the Monthly Sales Report System directly frames **QA-3 (Availability)** (handling late branch uploads) and **QA-5 (Security)** (separating Head-Office Analyst and Branch Manager access).
3. **Open question for the client**: Does the Finance Director want the PDF/Excel report attached directly to the readiness e-mail, or only a hyperlink to download it from the system?

## Level 2: Container Diagram Notes
1. **What this diagram deliberately omits**: It omits Java package structures (`model`, `ingest`, `render`), internal classes such as `CsvTransactionParser`, and method-level design patterns (Strategy and Observer), which belong to C4 Level 3/4.
2. **Quality-attribute linkage**: Decoupling the **CSV Landing Zone** from the **Report Batch Engine** supports **QA-3 (Availability)** by allowing partial runs when a branch link fails, while the dedicated **Branch Web Portal** container enforces **QA-5 (Security)** by isolating branch manager HTTPS requests from full report files.
3. **Open question for the client**: Will Head-Office Analysts run the JavaFX Desktop Viewer on the same machine as the Report Store, or over a shared corporate network drive?