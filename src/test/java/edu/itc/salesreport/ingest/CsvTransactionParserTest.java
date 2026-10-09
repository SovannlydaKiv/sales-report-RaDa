package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.PaymentMethod;
import edu.itc.salesreport.model.SaleTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CsvTransactionParser} – authored by Member B.
 *
 * <p>Covers:
 * <ul>
 *   <li>Valid line: revenue calculation matches expected value.</li>
 *   <li>Valid line with discount applied.</li>
 *   <li>Parameterized invalid lines (&ge; 8 cases).</li>
 * </ul>
 */
@DisplayName("CsvTransactionParser – validation and parsing")
class CsvTransactionParserTest {

    private CsvTransactionParser parser;

    @BeforeEach
    void setUp() {
        parser = new CsvTransactionParser();
    }

    // ── Valid lines ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("valid line: 5 × $3.00 − $2.00 discount = $13.00 revenue")
    void validLine_revenueEquality() throws InvalidRowException {
        // 5 units at $3.00, discount $2.00 → revenue = 15.00 - 2.00 = 13.00
        String line = "PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg," +
                      "Grocery,5,3.00,2.00,CASH";

        SaleTransaction tx = parser.parse(line);

        assertEquals(new BigDecimal("13.00"), tx.revenue(),
                "revenue() must equal quantity×unitPrice − discount, rounded to 2dp");
    }

    @Test
    @DisplayName("valid line: zero discount preserved correctly")
    void validLine_zeroDiscount() throws InvalidRowException {
        String line = "REP,2026-09-02,REP-000042,SKU-2002,Iced Coffee Can," +
                      "Beverages,3,0.90,0.00,KHQR";

        SaleTransaction tx = parser.parse(line);

        assertEquals("REP",                   tx.branch());
        assertEquals(PaymentMethod.KHQR,       tx.paymentMethod());
        assertEquals(new BigDecimal("2.70"),   tx.revenue());
    }

    @Test
    @DisplayName("valid line: CARD payment parsed correctly")
    void validLine_cardPayment() throws InvalidRowException {
        String line = "BTB,2026-09-03,BTB-000007,SKU-5002,Power Bank 10000mAh," +
                      "Electronics,2,18.00,0.10,CARD";

        SaleTransaction tx = parser.parse(line);

        assertEquals(PaymentMethod.CARD, tx.paymentMethod());
        assertEquals(new BigDecimal("35.90"), tx.revenue());
    }

    // ── Invalid lines – parameterized ───────────────────────────────────────

    /**
     * Each row: (label, csvLine).
     * All must throw {@link InvalidRowException}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("invalid lines must throw InvalidRowException")
    @CsvSource(delimiter = '|', value = {
        // 1 – too few fields (missing payment_method)
        "missing fields          |PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Grocery,1,6.50,0.00",
        // 2 – blank line
        "blank line              | ",
        // 3 – bad branch
        "bad branch              |SRP,2026-09-01,SRP-000001,SKU-1001,Jasmine Rice 5kg,Grocery,1,6.50,0.00,CASH",
        // 4 – bad category
        "bad category            |PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Software,1,6.50,0.00,CASH",
        // 5 – invalid date (Sept has 30 days)
        "date 2026-09-31         |PNH,2026-09-31,PNH-000001,SKU-1001,Jasmine Rice 5kg,Grocery,1,6.50,0.00,CASH",
        // 6 – quantity zero
        "quantity 0              |PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Grocery,0,6.50,0.00,CASH",
        // 7 – negative discount
        "negative discount       |PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Grocery,1,6.50,-0.10,CASH",
        // 8 – discount exceeds quantity×unitPrice
        "excess discount         |PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Grocery,1,6.50,7.00,CASH",
        // 9 – non-numeric unit price
        "non-numeric price       |PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Grocery,1,free,0.00,CASH",
        // 10 – non-numeric quantity
        "non-numeric quantity    |REP,2026-09-02,REP-000010,SKU-1001,Jasmine Rice 5kg,Grocery,two,6.50,0.00,CASH",
        // 11 – unsupported payment method
        "bad payment method      |BTB,2026-09-04,BTB-000020,SKU-3001,Dish Soap 500ml,Household,1,1.25,0.00,PAYPAL"
    })
    void invalidLine_throwsInvalidRowException(String label, String csvLine) {
        // Trim leading whitespace introduced by the @CsvSource delimiter
        String line = csvLine == null ? "" : csvLine.strip();
        assertThrows(InvalidRowException.class,
                () -> parser.parse(line),
                "Expected InvalidRowException for case: " + label.strip());
    }
}
