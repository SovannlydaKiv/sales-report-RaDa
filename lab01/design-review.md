# Lab-01: Design Review

*Authors: Chiv Inthera (Member A) – coupling metrics | Kiv Sovannlyda (Member B) – design trade-offs*

---

## 1. Package Coupling Metrics  *(Member A)*

### Methodology

We count **afferent coupling** (Ca = number of packages that import *into* the package)
and **efferent coupling** (Ce = number of packages the package imports *from*),
then compute Martin's **Instability**:

$$I = \frac{C_e}{C_a + C_e}$$

`I = 0` → maximally stable (responsible); `I = 1` → maximally instable (irresponsible).

### Import inspection

| From ↓ / To → | `model` | `ingest` | `render` | `App` (root) |
|---------------|---------|----------|----------|-------------|
| `model`       | –       | **no**   | **no**   | –           |
| `ingest`      | **yes** | –        | **no**   | –           |
| `render`      | **yes** | **no**   | –        | –           |
| `App` (root)  | **yes** | **yes**  | **yes**  | –           |

### Coupling table

| Package | Ca (afferent) | Ce (efferent) | I = Ce/(Ca+Ce) | Interpretation |
|---------|--------------|--------------|---------------|----------------|
| `model` | 3 *(ingest, render, App)* | 0 | **0.00** | Maximally stable – pure domain, no outbound deps |
| `ingest` | 1 *(App)* | 1 *(model)* | **0.50** | Balanced – depends on model, depended on by App |
| `render` | 1 *(App)* | 1 *(model)* | **0.50** | Balanced – depends on model, depended on by App |
| `App` (root) | 0 | 3 *(model, ingest, render)* | **1.00** | Maximally instable – composition root; expected and correct |

### Cohesion observation

- **`model`** – high cohesion: all types are pure data carriers (records + enum). No behaviour that belongs elsewhere.
- **`ingest`** – high cohesion: all types collaborate on the single responsibility of CSV ingestion and event notification.
- **`render`** – high cohesion: all types are output strategies for the same `MonthlyReport` input.
- **`App`** – intentionally low cohesion: it is the composition root and is the only permitted place to wire the three modules together.

---

## 2. Design Trade-Off: Sealed Renderer Hierarchy vs. Open Plugin Interface  *(Member B)*

### 2.1 The Current Design – Sealed Interface (Closed Set)

```java
public sealed interface ReportRenderer
        permits TextReportRenderer, HtmlReportRenderer, CsvReportRenderer {
    String render(MonthlyReport report);
}
```

**Compiler-exhaustiveness guarantee**

Because the `permits` clause lists exactly the allowed subtypes, the compiler
rejects any `switch` over a `ReportRenderer` that omits one arm:

```java
// This switch is exhaustive – no default needed
String rendered = switch (renderer) {
    case TextReportRenderer  r -> r.render(report);
    case HtmlReportRenderer  r -> r.render(report);
    case CsvReportRenderer   r -> r.render(report);
};
```

If a future developer adds `PdfReportRenderer` to the `permits` clause but
forgets to update this switch, the **compiler refuses to build** — a compile-time
safety net directly addressing **QA-4 (Modifiability)**.

**Pros of sealed interface**

| Pro | Impact |
|-----|--------|
| Exhaustive switch without `default` | Prevents silent runtime failures when a format is added |
| Refactoring confidence | IDEs and compilers flag every switch that needs updating |
| Simple factory (`forExtension`) | One switch expression; no ServiceLoader boilerplate |
| Zero framework dependency | No annotation processors, no module-path configuration |

**Cons of sealed interface**

| Con | Impact |
|-----|--------|
| Closed to external extension | Third-party plugins (e.g. customer-specific formats) cannot be added without modifying library source |
| Requires recompilation | Adding a format requires a new JAR deployment to all report servers |
| Module-path constraints | In a JPMS multi-module build, `permits` types must reside in the same module |

---

### 2.2 Alternative – Open Plugin Interface via `ServiceLoader`

```java
// Non-sealed; extensible by third parties
public interface ReportRenderer {
    String format();           // e.g. "html"
    String render(MonthlyReport report);
}
```

Third-party renderers register via `META-INF/services/edu.itc.salesreport.render.ReportRenderer`.
The factory becomes:

```java
public static ReportRenderer forExtension(String ext) {
    return ServiceLoader.load(ReportRenderer.class)
            .stream()
            .map(ServiceLoader.Provider::get)
            .filter(r -> r.format().equalsIgnoreCase(ext))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown format: " + ext));
}
```

**Pros of open plugin interface**

| Pro | Impact |
|-----|--------|
| Dynamic discovery at runtime | New formats deployed as JARs on the classpath without recompiling the core |
| Open extension point | Finance teams or BI vendors can contribute renderers independently |
| Aligns with OSGi / module system | Standard Java extension mechanism |

**Cons of open plugin interface**

| Con | Impact |
|-----|--------|
| No compiler exhaustiveness | A `switch` over an open interface always requires a `default` arm → silent fallback risk |
| Runtime class-loading | Broken plugins cause `ServiceConfigurationError` at runtime, not compile time |
| Testing burden | Every renderer variant must be independently discovered and tested; no static list |
| Operational complexity | Classpath/module-path management required per deployment environment |

---

### 2.3 Recommendation

For **Release 1**, the **sealed interface** is the correct choice:

- The format list is small and known in advance (TXT, HTML, CSV).
- QA-4 mandates a ≤ 2-day change cycle, which is satisfied by the simple
  three-step process (add class → add `permits` → update `forExtension`).
- The compile-time exhaustiveness check is a direct risk mitigation for the
  most likely modification scenario (adding a fourth format).

Migration to a `ServiceLoader`-based open interface is recommended only if
**external contributors** need to supply formats without access to the core
repository — a requirement not present until at least Release 3.

---

## 3. Architectural Analysis: Sealed vs. Open Interfaces for QA-4  *(Member B)*

The table below maps QA-4 (Modifiability) sub-scenarios to each design option:

| Sub-scenario | Sealed Interface | Open Interface |
|---|---|---|
| Internal team adds a new format | ✅ Compile-time safe, ≤ 1 day | ✅ Runtime dynamic, ≤ 1 day |
| External vendor adds a format | ❌ Requires source access | ✅ JAR drop-in |
| Forgot to update a switch after adding format | ❌ Compile error (caught early) | ⚠️ Silent `default` path (caught late) |
| Performance overhead | ✅ Zero | ⚠️ `ServiceLoader` scan on startup |
| JPMS compatibility | ⚠️ Same module required | ✅ Natural module boundary |

**Conclusion**: the sealed interface offers a **stronger correctness guarantee** at
the cost of extensibility. For Angkor Mart's closed-team Release 1, the trade-off
favours the sealed design.
