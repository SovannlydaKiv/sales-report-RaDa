package edu.itc.salesreport.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Aggregated sales figure for one SKU within a reporting period.
 *
 * @param sku         stock-keeping unit code
 * @param productName human-readable product name
 * @param totalUnits  total units sold (must be &gt; 0)
 * @param totalRevenue cumulative revenue (must be &ge; 0)
 */
public record ProductTotal(
        String sku,
        String productName,
        long totalUnits,
        BigDecimal totalRevenue) {

    /** Compact constructor – validates invariants. */
    public ProductTotal {
        Objects.requireNonNull(sku,          "sku must not be null");
        Objects.requireNonNull(productName,  "productName must not be null");
        Objects.requireNonNull(totalRevenue, "totalRevenue must not be null");
        if (totalUnits <= 0) {
            throw new IllegalArgumentException(
                    "totalUnits must be > 0, got: " + totalUnits);
        }
        if (totalRevenue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "totalRevenue must be >= 0, got: " + totalRevenue);
        }
    }
}
