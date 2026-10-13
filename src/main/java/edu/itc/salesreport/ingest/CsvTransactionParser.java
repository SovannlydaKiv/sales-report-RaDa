package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.PaymentMethod;
import edu.itc.salesreport.model.SaleTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.function.Function;

/** Parses the canonical Angkor Mart POS export (10 fields, no quoting). */
public final class CsvTransactionParser implements TransactionParser {

    public static final String HEADER = "branch,date,receipt_no,sku,"
            + "product_name,category,quantity,unit_price,discount,"
            + "payment_method";

    private static final Set<String> BRANCHES = Set.of("PNH", "REP", "BTB");
    private static final Set<String> CATEGORIES = Set.of(
            "Grocery", "Beverages", "Household",
            "Personal Care", "Electronics", "Stationery");

    @Override
    public SaleTransaction parse(String line) throws InvalidRowException {
        if (line == null || line.isBlank()) {
            throw new InvalidRowException("empty line");
        }
        String[] f = line.split(",", -1);
        if (f.length != 10) {
            throw new InvalidRowException(
                    "expected 10 fields but found " + f.length);
        }

        String branch = f[0].strip();
        if (!BRANCHES.contains(branch)) {
            throw new InvalidRowException("bad branch '" + f[0] + "'");
        }

        LocalDate date = field("date", f[1], LocalDate::parse);

        String receiptNo = f[2].strip();
        if (receiptNo.isEmpty()) {
            throw new InvalidRowException("bad receipt_no '" + f[2] + "'");
        }

        String sku = f[3].strip();
        if (sku.isEmpty()) {
            throw new InvalidRowException("bad sku '" + f[3] + "'");
        }

        String productName = f[4].strip();
        if (productName.isEmpty()) {
            throw new InvalidRowException("bad product_name '" + f[4] + "'");
        }

        String category = f[5].strip();
        if (!CATEGORIES.contains(category)) {
            throw new InvalidRowException("bad category '" + f[5] + "'");
        }

        int quantity = field("quantity", f[6], Integer::parseInt);
        BigDecimal unitPrice = field("unit_price", f[7], BigDecimal::new);
        BigDecimal discount = field("discount", f[8], BigDecimal::new);
        PaymentMethod paymentMethod = field("payment_method", f[9], PaymentMethod::valueOf);

        if (quantity <= 0) {
            throw new InvalidRowException("bad quantity '" + f[6] + "'");
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRowException("bad unit_price '" + f[7] + "'");
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRowException("bad discount '" + f[8] + "'");
        }
        if (discount.compareTo(unitPrice.multiply(BigDecimal.valueOf(quantity))) > 0) {
            throw new InvalidRowException("bad discount '" + f[8] + "'");
        }

        try {
            return new SaleTransaction(
                    branch, date, receiptNo, sku, productName,
                    category, quantity, unitPrice, discount, paymentMethod);
        } catch (IllegalArgumentException e) {
            throw new InvalidRowException("invalid transaction: " + e.getMessage(), e);
        }
    }

    /** Converts one field; any RuntimeException becomes a checked error. */
    private static <T> T field(String name, String raw,
            Function<String, T> convert) throws InvalidRowException {
        try {
            return convert.apply(raw.strip());
        } catch (RuntimeException e) {
            throw new InvalidRowException(
                    "bad " + name + " '" + raw + "'", e);
        }
    }
}