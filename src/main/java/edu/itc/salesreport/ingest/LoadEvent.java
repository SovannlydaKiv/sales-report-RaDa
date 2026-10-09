package edu.itc.salesreport.ingest;

import java.nio.file.Path;

/**
 * Sealed event hierarchy published by {@link MonthLoader} to registered
 * {@link LoadListener}s.
 *
 * <p>Permitted subtypes:
 * <ul>
 *   <li>{@link FileLoaded}  – one CSV file has been fully ingested.</li>
 *   <li>{@link LoadFinished} – the entire month's load is complete.</li>
 * </ul>
 */
public sealed interface LoadEvent
        permits LoadEvent.FileLoaded, LoadEvent.LoadFinished {

    /**
     * Emitted after each CSV file has been successfully parsed.
     *
     * @param file           the path of the file just loaded
     * @param transactionCount number of valid rows parsed from this file
     * @param errorCount     number of rows that failed validation
     */
    record FileLoaded(
            Path file,
            long transactionCount,
            long errorCount) implements LoadEvent {}

    /**
     * Emitted once after all files in the month directory have been processed.
     *
     * @param totalTransactions total valid rows across all files
     * @param totalErrors       total invalid rows across all files
     * @param skippedFiles      number of files that could not be opened
     */
    record LoadFinished(
            long totalTransactions,
            long totalErrors,
            int skippedFiles) implements LoadEvent {}
}
