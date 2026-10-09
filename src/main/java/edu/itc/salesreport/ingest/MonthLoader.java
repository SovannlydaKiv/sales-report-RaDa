package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.SaleTransaction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * Observable batch loader that reads all CSV files from a month directory.
 *
 * <h2>Observer pattern</h2>
 * <p>Listeners are registered with {@link #addListener(LoadListener)}.
 * The returned {@link Runnable} unsubscribes the listener when invoked.
 * Listeners are stored in a {@link CopyOnWriteArrayList} so that
 * subscriptions / unsubscriptions are safe while the loader is running.
 *
 * <h2>Load sequence</h2>
 * <ol>
 *   <li>Collect all {@code *.csv} files in the directory, sorted by name.</li>
 *   <li>For each file: skip header, parse each data row, accumulate errors.</li>
 *   <li>Dispatch {@link LoadEvent.FileLoaded} after each file.</li>
 *   <li>Dispatch {@link LoadEvent.LoadFinished} after all files.</li>
 * </ol>
 */
public class MonthLoader {

    private final Path directory;
    private final TransactionParser parser;

    /** Thread-safe listener registry. */
    private final CopyOnWriteArrayList<LoadListener> listeners =
            new CopyOnWriteArrayList<>();

    /**
     * Creates a loader for the given month directory.
     *
     * @param directory directory containing the branch CSV files
     * @param parser    stateless row parser
     */
    public MonthLoader(Path directory, TransactionParser parser) {
        this.directory = directory;
        this.parser    = parser;
    }

    /**
     * Subscribes a listener to load events.
     *
     * @param listener the observer to add (must not be {@code null})
     * @return a {@link Runnable} that, when invoked, removes the listener
     */
    public Runnable addListener(LoadListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /**
     * Loads all CSV files from the directory, parses every data row,
     * and dispatches {@link LoadEvent}s to registered listeners.
     *
     * @return all successfully parsed {@link SaleTransaction}s
     * @throws IOException if the directory cannot be listed
     */
    public List<SaleTransaction> load() throws IOException {
        List<SaleTransaction> transactions = new ArrayList<>();
        long totalTransactions = 0;
        long totalErrors       = 0;
        int  skippedFiles      = 0;

        List<Path> csvFiles;
        try (Stream<Path> stream = Files.list(directory)) {
            csvFiles = stream
                    .filter(p -> p.getFileName().toString().endsWith(".csv"))
                    .sorted()
                    .toList();
        }

        for (Path file : csvFiles) {
            long fileTx     = 0;
            long fileErrors = 0;
            List<String> lines;

            try {
                lines = Files.readAllLines(file);
            } catch (IOException e) {
                skippedFiles++;
                continue;
            }

            // Skip header line (index 0)
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.isBlank()) continue;
                try {
                    transactions.add(parser.parse(line));
                    fileTx++;
                } catch (InvalidRowException e) {
                    fileErrors++;
                    // Log format: "FILE:LINE message"
                    System.err.printf("%s:%d %s%n",
                            file.getFileName(), i + 1, e.getMessage());
                }
            }

            totalTransactions += fileTx;
            totalErrors       += fileErrors;
            dispatch(new LoadEvent.FileLoaded(file, fileTx, fileErrors));
        }

        dispatch(new LoadEvent.LoadFinished(totalTransactions, totalErrors, skippedFiles));
        return List.copyOf(transactions);
    }

    /** Publishes an event to all currently registered listeners. */
    private void dispatch(LoadEvent event) {
        for (LoadListener l : listeners) {
            l.onEvent(event);
        }
    }
}
