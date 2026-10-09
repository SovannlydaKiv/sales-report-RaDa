package edu.itc.salesreport.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable chain-wide monthly report aggregating all branch summaries.
 *
 * @param month          the reporting month (e.g. {@code 2026-09})
 * @param branches       ordered list of {@link BranchSummary} records
 *                       (defensive copy)
 * @param chainRevenue   sum of all branch revenues (must be &ge; 0)
 * @param skippedFiles   map of {@code filename -> reason} for any files
 *                       that could not be loaded (defensive copy)
 */
public record MonthlyReport(
        YearMonth month,
        List<BranchSummary> branches,
        BigDecimal chainRevenue,
        Map<String, String> skippedFiles) {

    /** Compact constructor – validates and defensively copies collections. */
    public MonthlyReport {
        Objects.requireNonNull(month,        "month must not be null");
        Objects.requireNonNull(branches,     "branches must not be null");
        Objects.requireNonNull(chainRevenue, "chainRevenue must not be null");
        Objects.requireNonNull(skippedFiles, "skippedFiles must not be null");

        if (chainRevenue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "chainRevenue must be >= 0, got: " + chainRevenue);
        }

        branches    = List.copyOf(branches);
        skippedFiles = Map.copyOf(skippedFiles);
    }
}
