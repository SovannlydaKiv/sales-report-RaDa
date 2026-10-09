package edu.itc.salesreport.model;

/**
 * Accepted payment tenders at every Angkor Mart POS terminal.
 *
 * <p>Backed by the {@code payment_method} column in each branch CSV.
 * Any other value must be rejected by the parser with an
 * {@link edu.itc.salesreport.ingest.InvalidRowException}.
 */
public enum PaymentMethod {
    /** Physical banknotes / coins. */
    CASH,
    /** Debit or credit card via the POS card reader. */
    CARD,
    /** Cambodia QR payment (Bakong / KHQR standard). */
    KHQR
}
