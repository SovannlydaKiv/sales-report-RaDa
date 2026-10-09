package edu.itc.salesreport.render;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.MonthlyReport;
import edu.itc.salesreport.model.ProductTotal;

/**
 * Renders a {@link MonthlyReport} as an HTML table.
 *
 * <p>All user-supplied strings are HTML-escaped to prevent XSS when the
 * report is served over HTTPS (QA-5 Security).
 */
public final class HtmlReportRenderer implements ReportRenderer {

    @Override
    public String render(MonthlyReport report) {
        var sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
          .append("  <meta charset=\"UTF-8\">\n")
          .append("  <title>Angkor Mart – Sales Report ")
          .append(escape(report.month().toString())).append("</title>\n")
          .append("</head>\n<body>\n")
          .append("<h1>Angkor Mart – Monthly Sales Report ")
          .append(escape(report.month().toString())).append("</h1>\n");

        // Branch table
        sb.append("<table border=\"1\" cellpadding=\"4\">\n")
          .append("  <thead><tr>")
          .append("<th>Branch</th><th>Revenue ($)</th>")
          .append("<th>Receipts</th><th>Avg Basket ($)</th>")
          .append("</tr></thead>\n  <tbody>\n");

        for (BranchSummary b : report.branches()) {
            sb.append("    <tr><td>").append(escape(b.branch()))
              .append("</td><td>").append(escape(b.totalRevenue().toPlainString()))
              .append("</td><td>").append(b.receiptCount())
              .append("</td><td>").append(escape(b.averageBasket().toPlainString()))
              .append("</td></tr>\n");
        }
        sb.append("  </tbody>\n</table>\n");

        // Top products per branch
        for (BranchSummary b : report.branches()) {
            if (b.topProducts().isEmpty()) continue;
            sb.append("<h2>").append(escape(b.branch()))
              .append(" – Top Products</h2>\n")
              .append("<table border=\"1\" cellpadding=\"4\">\n")
              .append("  <thead><tr><th>Rank</th><th>SKU</th>")
              .append("<th>Product</th><th>Units</th><th>Revenue ($)</th>")
              .append("</tr></thead>\n  <tbody>\n");
            int rank = 1;
            for (ProductTotal p : b.topProducts()) {
                sb.append("    <tr><td>").append(rank++)
                  .append("</td><td>").append(escape(p.sku()))
                  .append("</td><td>").append(escape(p.productName()))
                  .append("</td><td>").append(p.totalUnits())
                  .append("</td><td>").append(escape(p.totalRevenue().toPlainString()))
                  .append("</td></tr>\n");
            }
            sb.append("  </tbody>\n</table>\n");
        }

        // Chain total
        sb.append("<p><strong>Chain Total: $")
          .append(escape(report.chainRevenue().toPlainString()))
          .append("</strong></p>\n");

        // Skipped files
        if (!report.skippedFiles().isEmpty()) {
            sb.append("<h2>Skipped Files</h2>\n<ul>\n");
            report.skippedFiles().forEach((f, reason) ->
                    sb.append("  <li>").append(escape(f))
                      .append(" – ").append(escape(reason))
                      .append("</li>\n"));
            sb.append("</ul>\n");
        }

        sb.append("</body>\n</html>\n");
        return sb.toString();
    }

    /**
     * Escapes the five XML/HTML special characters to prevent injection.
     *
     * @param text input string (may be null → returns empty string)
     * @return HTML-safe string
     */
    static String escape(String text) {
        if (text == null) return "";
        return text.replace("&",  "&amp;")
                   .replace("<",  "&lt;")
                   .replace(">",  "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'",  "&#39;");
    }
}
