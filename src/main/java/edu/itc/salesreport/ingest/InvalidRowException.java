package edu.itc.salesreport.ingest;

/**
 * Checked exception thrown when a CSV row cannot be parsed into a
 * {@link edu.itc.salesreport.model.SaleTransaction}.
 *
 * <p>The message should identify the offending field and its value,
 * e.g. {@code "quantity: 'two' is not a valid integer"}.
 */
public class InvalidRowException extends Exception {

    /** Serialization UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the given detail message.
     *
     * @param message human-readable description of the validation failure
     */
    public InvalidRowException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with a message and the root cause.
     *
     * @param message human-readable description of the validation failure
     * @param cause   underlying exception (e.g. {@link NumberFormatException})
     */
    public InvalidRowException(String message, Throwable cause) {
        super(message, cause);
    }
}
