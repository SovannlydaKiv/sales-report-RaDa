package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.PaymentMethod;
import edu.itc.salesreport.model.SaleTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

/**
 * Parses a single CSV data line from a branch POS export into a
 * {@link SaleTransaction}.
 *
 * <p>Expected column order (10 fields, 0-indexed):
 * <pre>
 * 0:branch, 1:date, 2:receipt_no, 3:sku, 4:product_name,
 * 5:category, 6:quantity, 7:unit_price, 8:discount, 9:payment_method
 * </pre>
 *
 * <p>Validation rules:
 * <ul>
 *   <li>Exactly 10 non-blank fields after splitting on {@code ','} with
 *       {@code limit = -1}.</li>
 *   <li>Branch must be one of {@code PNH}, {@code REP}, {@code BTB}.</li>
 *   <li>Category must be one of the six allowed values.</li>
 *   <li>Date must be a valid {@link LocalDate} in ISO-8601 format.</li>
 *   <li>Quantity must be a positive integer.</li>
 *   <li>Unit price must be non-negative.</li>
 *   <li>Discount must be &ge; 0 and &le; quantity × unitPrice.</li>
 *   <li>Payment method must match a {@link PaymentMethod} constant.</li>
 * </ul>
 *
 * <p>All validation failures are wrapped in {@link InvalidRowException}
 * with the offending field name in the message.
 */
public class CsvTransactionParser implements TransactionParser {

    /** Valid branch codes accepted by the head-office system. */
    private static final Set<String> VALID_BRANCHES =
            Set.of("PNH", "REP", "BTB");

    /** Valid product categories present in the SampleData generator. */
    private static final Set<String> VALID_CATEGORIES =
            Set.of("Grocery", "Beverages", "Household",
                    "Personal Care", "Electronics", "Stationery");

    /** Number of fields expected in each data row. */
    private static final int EXPECTED_FIELDS = 10;

    /**
     * Parses one CSV data line into a {@link SaleTransaction}.
     *
     * @param line raw CSV line (not the header)
     * @return validated {@link SaleTransaction}
     * @throws InvalidRowException if any validation rule is violated
     */
    @Override
    public SaleTransaction parse(String line) throws InvalidRowException {
        if (line == null || line.isBlank()) {
            throw new InvalidRowException("line: blank or null input");
        }

        String[] f = line.split(",", -1);
        if (f.length != EXPECTED_FIELDS) {
            throw new InvalidRowException(
                    "line: expected %d fields but got %d in '%s'"
                            .formatted(EXPECTED_FIELDS, f.length, line));
        }

        // 0 – branch
        String branch = f[0].strip();
        if (!VALID_BRANCHES.contains(branch)) {
            throw new InvalidRowException(
                    "branch: '%s' is not one of %s".formatted(branch, VALID_BRANCHES));
        }

        // 1 – date
        LocalDate date;
        try {
            date = LocalDate.parse(f[1].strip());
        } catch (DateTimeParseException e) {
            throw new InvalidRowException(
                    "date: '%s' is not a valid ISO-8601 date".formatted(f[1].strip()), e);
        }

        // 2 – receipt_no (non-blank string)
        String receiptNo = f[2].strip();
        if (receiptNo.isEmpty()) {
            throw new InvalidRowException("receipt_no: must not be blank");
        }

        // 3 – sku (non-blank string)
        String sku = f[3].strip();
        if (sku.isEmpty()) {
            throw new InvalidRowException("sku: must not be blank");
        }

        // 4 – product_name (non-blank string)
        String productName = f[4].strip();
        if (productName.isEmpty()) {
            throw new InvalidRowException("product_name: must not be blank");
        }

        // 5 – category
        String category = f[5].strip();
        if (!VALID_CATEGORIES.contains(category)) {
            throw new InvalidRowException(
                    "category: '%s' is not one of %s".formatted(category, VALID_CATEGORIES));
        }

        // 6 – quantity
        int quantity;
        try {
            quantity = Integer.parseInt(f[6].strip());
        } catch (NumberFormatException e) {
            throw new InvalidRowException(
                    "quantity: '%s' is not a valid integer".formatted(f[6].strip()), e);
        }
        if (quantity <= 0) {
            throw new InvalidRowException(
                    "quantity: must be > 0, got " + quantity);
        }

        // 7 – unit_price
        BigDecimal unitPrice;
        try {
            unitPrice = new BigDecimal(f[7].strip());
        } catch (NumberFormatException e) {
            throw new InvalidRowException(
                    "unit_price: '%s' is not a valid decimal".formatted(f[7].strip()), e);
        }
        if (unitPrice.signum() < 0) {
            throw new InvalidRowException(
                    "unit_price: must be >= 0, got " + unitPrice);
        }

        // 8 – discount
        BigDecimal discount;
        try {
            discount = new BigDecimal(f[8].strip());
        } catch (NumberFormatException e) {
            throw new InvalidRowException(
                    "discount: '%s' is not a valid decimal".formatted(f[8].strip()), e);
        }

        // 9 – payment_method
        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(f[9].strip());
        } catch (IllegalArgumentException e) {
            throw new InvalidRowException(
                    "payment_method: '%s' is not a valid PaymentMethod".formatted(f[9].strip()), e);
        }

        // Delegate remaining invariants (discount bounds, etc.) to the record.
        try {
            return new SaleTransaction(branch, date, receiptNo, sku,
                    productName, category, quantity, unitPrice,
                    discount, paymentMethod);
        } catch (IllegalArgumentException e) {
            throw new InvalidRowException(e.getMessage(), e);
        }
    }
}
