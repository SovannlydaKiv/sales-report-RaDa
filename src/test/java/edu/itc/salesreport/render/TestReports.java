package edu.itc.salesreport.render;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.MonthlyReport;
import edu.itc.salesreport.model.ProductTotal;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Shared test-fixture factory for renderer tests.
 *
 * <p>Creates a synthetic {@link MonthlyReport} with three branches:
 * <ul>
 *   <li>PNH – revenue $1000.25</li>
 *   <li>REP – revenue $800.00</li>
 *   <li>BTB – revenue $500.25</li>
 * </ul>
 * Chain total: $2300.50
 */
public final class TestReports {

    private TestReports() { /* utility */ }

    /** Creates a deterministic three-branch report for unit tests. */
    public static MonthlyReport threebranchReport() {
        ProductTotal rice = new ProductTotal(
                "SKU-1001", "Jasmine Rice 5kg", 100L, new BigDecimal("650.00"));
        ProductTotal water = new ProductTotal(
                "SKU-2001", "Mineral Water 1.5L", 80L, new BigDecimal("36.00"));
        ProductTotal soap = new ProductTotal(
                "SKU-3001", "Dish Soap 500ml", 60L, new BigDecimal("75.00"));

        BranchSummary pnh = new BranchSummary(
                "PNH",
                new BigDecimal("1000.25"),
                300L,
                40L,
                List.of(rice),
                Map.of("Grocery", new BigDecimal("1000.25")));

        BranchSummary rep = new BranchSummary(
                "REP",
                new BigDecimal("800.00"),
                250L,
                35L,
                List.of(water),
                Map.of("Beverages", new BigDecimal("800.00")));

        BranchSummary btb = new BranchSummary(
                "BTB",
                new BigDecimal("500.25"),
                180L,
                28L,
                List.of(soap),
                Map.of("Household", new BigDecimal("500.25")));

        return new MonthlyReport(
                YearMonth.of(2026, 9),
                List.of(pnh, rep, btb),
                new BigDecimal("2300.50"),
                Map.of());
    }
}
