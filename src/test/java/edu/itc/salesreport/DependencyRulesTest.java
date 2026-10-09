package edu.itc.salesreport;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.MonthlyReport;
import edu.itc.salesreport.model.ProductTotal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Architectural dependency rules – authored by Member B.
 *
 * <p>Enforces that:
 * <ul>
 *   <li>The {@code model} package has zero outbound imports to {@code ingest}
 *       or {@code render}.</li>
 *   <li>The {@code ingest} package does not import anything from {@code render}.</li>
 * </ul>
 *
 * <p>The checks are implemented by scanning the compiled {@code .class} file
 * source text (since we don't have ArchUnit on the classpath) – we read the
 * production {@code .java} source files and inspect their {@code import}
 * statements. This is intentionally simple and sufficient for the lab.
 */
@DisplayName("Dependency Rules – acyclic layered imports")
class DependencyRulesTest {

    private static final Path SRC_ROOT =
            Path.of("src", "main", "java", "edu", "itc", "salesreport");

    // ── model must not import ingest or render ───────────────────────────────

    @Test
    @DisplayName("model package: no imports from ingest or render")
    void model_hasNoCrossImports() throws IOException {
        checkNoImports(SRC_ROOT.resolve("model"),
                Set.of("edu.itc.salesreport.ingest",
                       "edu.itc.salesreport.render"));
    }

    // ── ingest must not import render ────────────────────────────────────────

    @Test
    @DisplayName("ingest package: no imports from render")
    void ingest_doesNotImportRender() throws IOException {
        checkNoImports(SRC_ROOT.resolve("ingest"),
                Set.of("edu.itc.salesreport.render"));
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    /**
     * Scans all {@code .java} files in {@code directory} and fails if any
     * {@code import} statement starts with one of the forbidden prefixes.
     *
     * @param directory       package source directory
     * @param forbiddenPkgs   set of forbidden import prefixes
     * @throws IOException    if the directory cannot be walked
     */
    private static void checkNoImports(Path directory,
                                       Set<String> forbiddenPkgs)
            throws IOException {

        if (!Files.exists(directory)) {
            // Directory not yet created – treat as pass (nothing to check).
            return;
        }

        try (var walk = Files.walk(directory)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                .forEach(file -> {
                    try {
                        List<String> lines = Files.readAllLines(file);
                        for (int i = 0; i < lines.size(); i++) {
                            String line = lines.get(i).strip();
                            if (!line.startsWith("import ")) continue;
                            for (String forbidden : forbiddenPkgs) {
                                if (line.startsWith("import " + forbidden)) {
                                    fail("Illegal import in %s:%d  →  %s"
                                            .formatted(file, i + 1, line));
                                }
                            }
                        }
                    } catch (IOException e) {
                        fail("Could not read " + file + ": " + e.getMessage());
                    }
                });
        }
    }
}
