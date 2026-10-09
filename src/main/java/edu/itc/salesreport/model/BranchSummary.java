package edu.itc.salesreport.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable summary of one branch's activity for a reporting period.
 *
 * <p>All collections are defensively copied on construction so that
 * callers cannot mutate the internal state.
 *
 * @param branch        three-letter branch code (PNH, REP, BTB)
 * @param totalRevenue  gross revenue for the period (must be &ge; 0)
 * @param transactionCount number of individual line-item transactions
 *                      processed (must be &ge; 0)
 * @param receiptCount  number of distinct receipts (must be &ge; 0)
 * @param topProducts   ordered list of top-selling {@link ProductTotal}
 *                      records (defensive copy)
 * @param revenueByCategory revenue broken down by category name
 *                          (defensive copy)
 */
public record BranchSummary(
        String branch,
        BigDecimal totalRevenue,
        long transactionCount,
        long receiptCount,
        List<ProductTotal> topProducts,
        Map<String, BigDecimal> revenueByCategory) {

    /** Compact constructor – validates and defensively copies collections. */
    public BranchSummary {
        Objects.requireNonNull(branch,             "branch must not be null");
        Objects.requireNonNull(totalRevenue,        "totalRevenue must not be null");
        Objects.requireNonNull(topProducts,         "topProducts must not be null");
        Objects.requireNonNull(revenueByCategory,   "revenueByCategory must not be null");

        if (totalRevenue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "totalRevenue must be >= 0, got: " + totalRevenue);
        }
        if (transactionCount < 0) {
            throw new IllegalArgumentException(
                    "transactionCount must be >= 0, got: " + transactionCount);
        }
        if (receiptCount < 0) {
            throw new IllegalArgumentException(
                    "receiptCount must be >= 0, got: " + receiptCount);
        }

        // Defensive copies – callers cannot mutate the internal state.
        topProducts        = List.copyOf(topProducts);
        revenueByCategory  = Map.copyOf(revenueByCategory);
    }

    /**
     * Average basket value: {@code totalRevenue / receiptCount}.
     *
     * <p>Returns {@link BigDecimal#ZERO} when there are no receipts.
     *
     * @return average basket rounded to 2 d.p. using HALF_UP
     */
    public BigDecimal averageBasket() {
        if (receiptCount == 0) {
            return BigDecimal.ZERO;
        }
        return totalRevenue
                .divide(BigDecimal.valueOf(receiptCount), 2, RoundingMode.HALF_UP);
    }
}
