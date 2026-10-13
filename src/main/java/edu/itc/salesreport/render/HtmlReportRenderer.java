package edu.itc.salesreport.render;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.MonthlyReport;

import java.nio.charset.StandardCharsets;

public final class HtmlReportRenderer implements ReportRenderer {

    @Override
    public String extension() {
        return "html";
    }

    @Override
    public byte[] render(MonthlyReport report) {
        var sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html>\n<head><title>Angkor Mart ")
          .append(escape(report.month().toString()))
          .append("</title></head>\n<body>\n<table>\n")
          .append("<thead><tr><th>Branch</th><th>Revenue</th><th>Receipts</th><th>Avg basket</th></tr></thead>\n")
          .append("<tbody>\n");
        for (BranchSummary b : report.branches()) {
            row(sb, b);
        }
        row(sb, report.chain());
        sb.append("</tbody>\n</table>\n</body>\n</html>\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void row(StringBuilder sb, BranchSummary b) {
        sb.append("<tr><td>").append(escape(b.branch()))
          .append("</td><td>").append(escape(b.revenue().toString()))
          .append("</td><td>").append(b.receipts())
          .append("</td><td>").append(escape(b.averageBasket().toString()))
          .append("</td></tr>\n");
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}