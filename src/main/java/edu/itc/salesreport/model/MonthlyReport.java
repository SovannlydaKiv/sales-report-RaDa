package edu.itc.salesreport.model;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

public record MonthlyReport(
        YearMonth month,
        List<BranchSummary> branches,
        BranchSummary chain,
        List<String> anomalies) {

    public MonthlyReport {
        Objects.requireNonNull(month, "month");
        branches = List.copyOf(Objects.requireNonNull(branches, "branches"));
        Objects.requireNonNull(chain, "chain");
        anomalies = List.copyOf(Objects.requireNonNull(anomalies, "anomalies"));
    }
}