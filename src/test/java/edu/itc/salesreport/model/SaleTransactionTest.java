package edu.itc.salesreport.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SaleTransactionTest {

    @Test
    void revenueRoundsHalfUpToScaleTwo() {
        // 3 x 0.335 = 1.005 -> rounds HALF_UP to 1.01
        var tx = new SaleTransaction(
                "PNH",
                LocalDate.of(2026, 9, 1),
                "PNH-000001",
                "SKU-1001",
                "Jasmine Rice 5kg",
                "Grocery",
                3,
                new BigDecimal("0.335"),
                new BigDecimal("0.00"),
                PaymentMethod.CASH);

        assertEquals(new BigDecimal("1.01"), tx.revenue());
    }

    @Test
    void rejectsInvalidInvariantsInCompactConstructor() {
        LocalDate date = LocalDate.of(2026, 9, 1);
        assertThrows(IllegalArgumentException.class, () -> new SaleTransaction(
                "PNH", date, "R-1", "SKU-1", "Item", "Grocery",
                0, new BigDecimal("5.00"), BigDecimal.ZERO, PaymentMethod.CASH));

        assertThrows(IllegalArgumentException.class, () -> new SaleTransaction(
                "PNH", date, "R-1", "SKU-1", "Item", "Grocery",
                1, new BigDecimal("5.00"), new BigDecimal("-0.01"), PaymentMethod.CASH));

        assertThrows(IllegalArgumentException.class, () -> new SaleTransaction(
                "PNH", date, "R-1", "SKU-1", "Item", "Grocery",
                2, new BigDecimal("5.00"), new BigDecimal("10.01"), PaymentMethod.CASH));
    }

    @Test
    void branchSummaryCollectionsAndAverageBasketAreProtected() {
        var mutableProducts = new ArrayList<ProductTotal>();
        mutableProducts.add(new ProductTotal("SKU-1", "Rice", new BigDecimal("10.00"), 2));

        var summary = new BranchSummary(
                "PNH",
                new BigDecimal("100.00"),
                new BigDecimal("0.00"),
                3,
                Map.of("Grocery", new BigDecimal("100.00")),
                mutableProducts,
                Map.of(PaymentMethod.KHQR, new BigDecimal("100.00")));

        mutableProducts.clear();
        assertEquals(1, summary.topProducts().size());
        assertThrows(UnsupportedOperationException.class,
                () -> summary.topProducts().add(new ProductTotal("SKU-2", "Water", BigDecimal.ONE, 1)));
        assertEquals(new BigDecimal("33.33"), summary.averageBasket());

        var emptySummary = new BranchSummary(
                "BTB", BigDecimal.ZERO, BigDecimal.ZERO, 0, Map.of(), List.of(), Map.of());
        assertEquals(new BigDecimal("0.00"), emptySummary.averageBasket());
    }
}
