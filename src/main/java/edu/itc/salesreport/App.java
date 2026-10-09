package edu.itc.salesreport;

import edu.itc.salesreport.ingest.ConsoleProgress;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.ingest.MonthLoader;
import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.MonthlyReport;
import edu.itc.salesreport.model.ProductTotal;
import edu.itc.salesreport.model.SaleTransaction;
import edu.itc.salesreport.render.ReportRenderer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Composition root and application entry point.
 *
 * <p>Usage:
 * <pre>
 *   mvn compile exec:java          # reads data/2026-09, writes TXT to stdout
 *   mvn compile exec:java -Dmonth=2026-09 -Dext=html
 * </pre>
 */
public class App {

    public static void main(String[] args) throws Exception {
        // Read configuration from system properties / fallbacks
        String monthStr = System.getProperty("month", "2026-09");
        String ext      = System.getProperty("ext",   "txt");

        YearMonth month = YearMonth.parse(monthStr);
        Path directory  = Path.of("data", monthStr);

        // Wire up the loader with the console observer
        var parser   = new CsvTransactionParser();
        var loader   = new MonthLoader(directory, parser);
        var progress = new ConsoleProgress();
        loader.addListener(progress);

        // Load all transactions
        List<SaleTransaction> transactions = loader.load();

        // Aggregate by branch
        Map<String, List<SaleTransaction>> byBranch = transactions.stream()
                .collect(Collectors.groupingBy(SaleTransaction::branch));

        List<BranchSummary> summaries = byBranch.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> buildSummary(e.getKey(), e.getValue()))
                .toList();

        BigDecimal chainRevenue = summaries.stream()
                .map(BranchSummary::totalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        MonthlyReport report = new MonthlyReport(month, summaries, chainRevenue, Map.of());

        // Render and print
        ReportRenderer renderer = ReportRenderer.forExtension(ext);
        System.out.println(renderer.render(report));
    }

    /**
     * Builds a {@link BranchSummary} from a list of transactions for one branch.
     *
     * @param branch       branch code
     * @param transactions all transactions for this branch
     * @return aggregated summary
     */
    private static BranchSummary buildSummary(
            String branch, List<SaleTransaction> transactions) {

        BigDecimal totalRevenue = transactions.stream()
                .map(SaleTransaction::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        long receiptCount = transactions.stream()
                .map(SaleTransaction::receiptNo)
                .distinct()
                .count();

        // Revenue by category
        Map<String, BigDecimal> byCategory = new TreeMap<>();
        for (SaleTransaction t : transactions) {
            byCategory.merge(t.category(), t.revenue(), BigDecimal::add);
        }

        // Top products by revenue (descending), take top 5
        Map<String, BigDecimal> skuRevenue = new TreeMap<>();
        Map<String, Long>       skuUnits   = new TreeMap<>();
        Map<String, String>     skuName    = new TreeMap<>();

        for (SaleTransaction t : transactions) {
            skuRevenue.merge(t.sku(), t.revenue(), BigDecimal::add);
            skuUnits.merge(t.sku(), (long) t.quantity(), Long::sum);
            skuName.put(t.sku(), t.productName());
        }

        List<ProductTotal> topProducts = skuRevenue.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue(
                        Comparator.reverseOrder()))
                .limit(5)
                .map(e -> new ProductTotal(
                        e.getKey(),
                        skuName.get(e.getKey()),
                        skuUnits.get(e.getKey()),
                        e.getValue().setScale(2, RoundingMode.HALF_UP)))
                .toList();

        return new BranchSummary(branch, totalRevenue,
                transactions.size(), receiptCount, topProducts, byCategory);
    }
}
