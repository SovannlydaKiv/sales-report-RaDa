package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.SaleTransaction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

public final class MonthLoader {

    private final TransactionParser parser;
    private final List<LoadListener> listeners = new CopyOnWriteArrayList<>();
    private final List<String> rejectedRows = new ArrayList<>();

    public MonthLoader(TransactionParser parser) {
        this.parser = parser;
    }

    /** Registers a listener; the returned handle unsubscribes it. */
    public Runnable subscribe(LoadListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public List<String> rejectedRows() {
        return List.copyOf(rejectedRows);
    }

    public List<SaleTransaction> load(YearMonth month, Path dataDir) {
        Instant start = Instant.now();
        rejectedRows.clear();
        var transactions = new ArrayList<SaleTransaction>();
        Path monthDir = dataDir.resolve(month.toString());

        List<Path> files;
        try (Stream<Path> stream = Files.list(monthDir)) {
            files = stream
                    .filter(p -> p.getFileName().toString().endsWith(".csv"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        int totalAccepted = 0;
        int totalRejected = 0;
        int filesDone = 0;
        int filesTotal = files.size();

        for (Path file : files) {
            int fileAccepted = 0;
            int fileRejected = 0;
            List<String> lines;
            try {
                lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            for (int i = 1; i < lines.size(); i++) {
                try {
                    transactions.add(parser.parse(lines.get(i)));
                    fileAccepted++;
                } catch (InvalidRowException e) {
                    fileRejected++;
                    rejectedRows.add(file.getFileName() + ":" + (i + 1) + " " + e.getMessage());
                }
            }

            totalAccepted += fileAccepted;
            totalRejected += fileRejected;
            filesDone++;
            publish(new LoadEvent.FileLoaded(file, fileAccepted, fileRejected, filesDone, filesTotal));
        }

        publish(new LoadEvent.LoadFinished(totalAccepted, totalRejected, Duration.between(start, Instant.now())));
        return transactions;
    }

    private void publish(LoadEvent event) {
        listeners.forEach(l -> l.onEvent(event));
    }
}