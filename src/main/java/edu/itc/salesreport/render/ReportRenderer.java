package edu.itc.salesreport.render;

import edu.itc.salesreport.model.MonthlyReport;

/**
 * Sealed strategy interface for rendering a {@link MonthlyReport} to a string.
 *
 * <p>The closed permit set ({@link TextReportRenderer}, {@link HtmlReportRenderer},
 * {@link CsvReportRenderer}) enables exhaustive {@code switch} expressions in
 * the caller without a {@code default} clause — satisfying QA-4's compiler
 * exhaustiveness requirement.
 *
 * <p>To add a new output format:
 * <ol>
 *   <li>Add a new implementation class in this package.</li>
 *   <li>Add it to the {@code permits} clause.</li>
 *   <li>Update {@link #forExtension(String)} accordingly.</li>
 * </ol>
 */
public sealed interface ReportRenderer
        permits TextReportRenderer, HtmlReportRenderer, CsvReportRenderer {

    /**
     * Renders the given report to a string.
     *
     * @param report the monthly report to render (must not be {@code null})
     * @return a non-null rendered string in the format specific to this renderer
     */
    String render(MonthlyReport report);

    /**
     * Factory method that returns the appropriate renderer for a file extension.
     *
     * @param ext file extension without the leading dot (e.g. {@code "html"})
     * @return matched {@link ReportRenderer} instance
     * @throws IllegalArgumentException if the extension is not supported
     */
    static ReportRenderer forExtension(String ext) {
        return switch (ext == null ? "" : ext.toLowerCase()) {
            case "txt"  -> new TextReportRenderer();
            case "html" -> new HtmlReportRenderer();
            case "csv"  -> new CsvReportRenderer();
            default     -> throw new IllegalArgumentException(
                    "Unsupported report extension: '" + ext + "'");
        };
    }
}
