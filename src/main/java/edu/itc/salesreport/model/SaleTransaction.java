package edu.itc.salesreport.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Immutable value object representing one line-item in a branch POS CSV.
 *
 * <p>Invariants enforced in the compact constructor:
 * <ul>
 *   <li>No field is {@code null}.</li>
 *   <li>{@code quantity > 0}</li>
 *   <li>{@code unitPrice >= 0}</li>
 *   <li>{@code discount >= 0}</li>
 *   <li>{@code discount <= quantity × unitPrice}</li>
 * </ul>
 *
 * <p>No {@code float} or {@code double} is ever used; all monetary
 * arithmetic is done with {@link BigDecimal}.
 *
 * @param branch        three-letter branch code (PNH, REP, BTB)
 * @param date          transaction date
 * @param receiptNo     composite receipt identifier
 * @param sku           stock-keeping unit code
 * @param productName   human-readable product name
 * @param category      product category
 * @param quantity      number of units sold (must be &gt; 0)
 * @param unitPrice     price per unit (must be &ge; 0)
 * @param discount      total discount applied to the line (must be &ge; 0
 *                      and &le; {@code quantity × unitPrice})
 * @param paymentMethod tender type
 */
public record SaleTransaction(
        String branch,
        LocalDate date,
        String receiptNo,
        String sku,
        String productName,
        String category,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal discount,
        PaymentMethod paymentMethod) {

    /** Compact canonical constructor – validates all invariants. */
    public SaleTransaction {
        Objects.requireNonNull(branch,        "branch must not be null");
        Objects.requireNonNull(date,          "date must not be null");
        Objects.requireNonNull(receiptNo,     "receiptNo must not be null");
        Objects.requireNonNull(sku,           "sku must not be null");
        Objects.requireNonNull(productName,   "productName must not be null");
        Objects.requireNonNull(category,      "category must not be null");
        Objects.requireNonNull(unitPrice,     "unitPrice must not be null");
        Objects.requireNonNull(discount,      "discount must not be null");
        Objects.requireNonNull(paymentMethod, "paymentMethod must not be null");

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "quantity must be > 0, got: " + quantity);
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "unitPrice must be >= 0, got: " + unitPrice);
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "discount must be >= 0, got: " + discount);
        }

        // discount <= quantity × unitPrice
        BigDecimal maxDiscount =
                BigDecimal.valueOf(quantity).multiply(unitPrice);
        if (discount.compareTo(maxDiscount) > 0) {
            throw new IllegalArgumentException(
                    "discount %s exceeds quantity×unitPrice %s"
                            .formatted(discount, maxDiscount));
        }
    }

    /**
     * Net revenue for this line item, rounded to 2 decimal places
     * using {@link RoundingMode#HALF_UP}.
     *
     * <pre>revenue = quantity × unitPrice − discount</pre>
     *
     * @return non-negative revenue value rounded to 2 d.p.
     */
    public BigDecimal revenue() {
        return BigDecimal.valueOf(quantity)
                .multiply(unitPrice)
                .subtract(discount)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
