# Package Design Review: Cohesion, Coupling, and Instability

## 1. Package Responsibilities (Cohesion)
- **`edu.itc.salesreport.model`**: Defines the immutable canonical domain records (`SaleTransaction`, `ProductTotal`, `BranchSummary`, `MonthlyReport`) and `PaymentMethod` enum, enforcing financial invariants and `BigDecimal` money rules.
- **`edu.itc.salesreport.ingest`**: Parses and validates raw POS CSV lines into `SaleTransaction` records and streams load progress events (`LoadEvent`) to subscribed listeners (`LoadListener`).
- **`edu.itc.salesreport.render`**: Converts a `MonthlyReport` into byte arrays for specific output formats (`txt`, `html`, `csv`) using pluggable rendering strategies.
- **`edu.itc.salesreport` (root)**: Houses the composition root (`App`) that wires `CsvTransactionParser`, `MonthLoader`, and `ConsoleProgress` together at runtime.

## 2. Package Coupling and Instability ($I = C_e / (C_a + C_e)$)

Counting package-level dependencies across project packages (`model`, `ingest`, `render`, and root `edu.itc.salesreport`) from actual `import edu.itc.salesreport.*` declarations:

| Package | Efferent Coupling ($C_e$) | Afferent Coupling ($C_a$) | Instability $I = \frac{C_e}{C_a + C_e}$ |
|---------|---------------------------|---------------------------|-----------------------------------------|
| `model` | $0$ (imports no project packages) | $2$ (imported by `ingest` and `render`) | $I = \frac{0}{2 + 0} = 0.00$ (Maximally stable) |
| `ingest` | $1$ (imports `model`) | $1$ (imported by root `App`) | $I = \frac{1}{1 + 1} = 0.50$ (Balanced; $1.00$ if excluding root `App`) |
| `render` | $1$ (imports `model`) | $0$ (not yet imported by `App`) | $I = \frac{1}{0 + 1} = 1.00$ (Maximally unstable / leaf package) |
| `root` (`App`) | $1$ (imports `ingest`) | $0$ (entry point) | $I = \frac{1}{0 + 1} = 1.00$ (Composition root) |

*(Note: If measuring distinct class imports rather than package edges, `ingest` has $C_e = 2$ (`PaymentMethod`, `SaleTransaction`) and `render` has $C_e = 2$ (`BranchSummary`, `MonthlyReport`), yielding the same $I = 0.00$ for `model` and $I = 1.00$ for `render`.)*

## 3. Accepted Coupling Justification
We explicitly accept that both `ingest` and `render` depend on `model` ($C_e = 1$ each). Because `model` has $I = 0.00$ and encapsulates Angkor Mart's core domain invariants with zero external dependencies, routing data between `ingest` and `render` via `model` adheres to the Stable Dependencies Principle (depend in the direction of stability) while keeping `ingest` and `render` completely decoupled from one another.

## 4. Sealed vs. Open `ReportRenderer` Hierarchy for QA-4 (Modifiability)
For **QA-4** (adding a new export format in $\le 2$ person-days), an **open interface** minimizes lines modified in existing files (0 lines touched in `ReportRenderer` when adding a new class), whereas a **sealed interface** requires adding the new class name to the `permits` clause and updating the `forExtension` factory and any exhaustive `switch` expressions. However, within a single-team modular monolith, the **sealed hierarchy** is superior because the Java 25 compiler statically guarantees that every `switch` over `ReportRenderer` handles the new format, preventing silent runtime omissions when PDF and XLSX renderers are added in Lab-08.