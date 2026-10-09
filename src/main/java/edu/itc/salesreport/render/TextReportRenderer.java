package edu.itc.salesreport.render;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.MonthlyReport;
import edu.itc.salesreport.model.ProductTotal;

import java.util.stream.Collectors;

/**
 * Renders a {@link MonthlyReport} as plain-text (human-readable).
 *
 * <p>Output format:
 * <pre>
 * =============================================
 * Angkor Mart – Monthly Sales Report [2026-09]
 * =============================================
 * Branch: PNH  Revenue: $1234.56  Baskets: 42  Avg: $29.40
 *   Top Products:
 *     1. Jasmine Rice 5kg  (SKU-1001)  units: 42  revenue: $273.00
 * ...
 * ---------------------------------------------
 * CHAIN TOTAL: $3456.78
 * </pre>
 */
public final class TextReportRenderer implements ReportRenderer {

    private static final String SEP_HEAVY = "=".repeat(52);
    private static final String SEP_LIGHT = "-".repeat(52);

    @Override
    public String render(MonthlyReport report) {
        var sb = new StringBuilder();
        sb.append(SEP_HEAVY).append('\n');
        sb.append("Angkor Mart – Monthly Sales Report [")
          .append(report.month()).append("]\n");
        sb.append(SEP_HEAVY).append('\n');

        for (BranchSummary b : report.branches()) {
            sb.append("Branch: ").append(b.branch())
              .append("  Revenue: $").append(b.totalRevenue())
              .append("  Baskets: ").append(b.receiptCount())
              .append("  Avg: $").append(b.averageBasket())
              .append('\n');

            if (!b.topProducts().isEmpty()) {
                sb.append("  Top Products:\n");
                int rank = 1;
                for (ProductTotal p : b.topProducts()) {
                    sb.append("    %d. %s (%s)  units: %d  revenue: $%s%n"
                            .formatted(rank++, p.productName(), p.sku(),
                                    p.totalUnits(), p.totalRevenue()));
                }
            }

            if (!b.revenueByCategory().isEmpty()) {
                sb.append("  By Category:\n");
                b.revenueByCategory().entrySet().stream()
                 .sorted(java.util.Map.Entry.comparingByKey())
                 .forEach(e -> sb.append("    %s: $%s%n"
                         .formatted(e.getKey(), e.getValue())));
            }
            sb.append(SEP_LIGHT).append('\n');
        }

        sb.append("CHAIN TOTAL: $").append(report.chainRevenue()).append('\n');

        if (!report.skippedFiles().isEmpty()) {
            sb.append("SKIPPED FILES:\n");
            report.skippedFiles().forEach((f, reason) ->
                    sb.append("  ").append(f).append(" – ").append(reason).append('\n'));
        }

        return sb.toString();
    }
}
