package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.SaleTransaction;

/**
 * Strategy interface for parsing a single raw CSV line into a
 * {@link SaleTransaction}.
 *
 * <p>Implementations must be stateless so that the same instance can
 * be reused concurrently by {@link MonthLoader}.
 */
@FunctionalInterface
public interface TransactionParser {

    /**
     * Parses one CSV data line (header lines must be filtered by the caller).
     *
     * @param line raw CSV line (must not be {@code null})
     * @return the parsed {@link SaleTransaction}
     * @throws InvalidRowException if the line fails any validation rule
     */
    SaleTransaction parse(String line) throws InvalidRowException;
}
