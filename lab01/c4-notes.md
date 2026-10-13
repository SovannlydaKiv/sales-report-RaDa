# C4 Architecture Diagram Notes

## Level 1: System Context Notes
1. **What this diagram deliberately omits**: It hides internal containers, data stores, scheduling mechanisms, and network protocols to focus strictly on user roles and external system boundaries.
2. **Quality-attribute linkage**: The boundary between the three Branch POS Systems and the Monthly Sales Report System directly frames **QA-3 (Availability)** (handling late branch uploads) and **QA-5 (Security)** (separating Head-Office Analyst and Branch Manager access).
3. **Open question for the client**: Does the Finance Director want the PDF/Excel report attached directly to the readiness e-mail, or only a hyperlink to download it from the system?