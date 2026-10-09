package edu.itc.salesreport.render;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.MonthlyReport;
import edu.itc.salesreport.model.ProductTotal;

/**
 * Renders a {@link MonthlyReport} as a machine-readable CSV.
 *
 * <p>Output columns:
 * <pre>
 * month,branch,total_revenue,receipt_count,avg_basket,top_sku,top_product,top_units,top_revenue
 * </pre>
 * One row per branch–product pair. If a branch has no top products, a
 * single summary row is written with empty product fields.
 */
public final class CsvReportRenderer implements ReportRenderer {

    private static final String HEADER =
            "month,branch,total_revenue,receipt_count,avg_basket," +
            "top_sku,top_product,top_units,top_revenue";

    @Override
    public String render(MonthlyReport report) {
        var sb = new StringBuilder();
        sb.append(HEADER).append('\n');

        for (BranchSummary b : report.branches()) {
            String prefix = "%s,%s,%s,%d,%s".formatted(
                    report.month(),
                    csvEscape(b.branch()),
                    b.totalRevenue().toPlainString(),
                    b.receiptCount(),
                    b.averageBasket().toPlainString());

            if (b.topProducts().isEmpty()) {
                sb.append(prefix).append(",,,, \n");
            } else {
                for (ProductTotal p : b.topProducts()) {
                    sb.append(prefix).append(',')
                      .append(csvEscape(p.sku())).append(',')
                      .append(csvEscape(p.productName())).append(',')
                      .append(p.totalUnits()).append(',')
                      .append(p.totalRevenue().toPlainString()).append('\n');
                }
            }
        }

        return sb.toString();
    }

    /**
     * Wraps a field in double-quotes if it contains a comma or quote,
     * escaping any internal double-quotes by doubling them (RFC 4180).
     *
     * @param value field value (may be null → empty string)
     * @return RFC-4180-compliant CSV field
     */
    static String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
