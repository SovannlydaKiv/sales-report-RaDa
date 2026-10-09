package edu.itc.salesreport.ingest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MonthLoader} – authored by Member B.
 *
 * <p>Uses {@link org.junit.jupiter.api.io.TempDir} to simulate a month
 * directory with synthetic CSV files, asserting:
 * <ul>
 *   <li>Correct event counts (FileLoaded per file, one LoadFinished).</li>
 *   <li>Total transaction counts match expected valid rows.</li>
 *   <li>An unsubscribed listener receives 0 events.</li>
 * </ul>
 */
@DisplayName("MonthLoader – observable loading and unsubscription")
class MonthLoaderTest {

    private static final String HEADER =
            "branch,date,receipt_no,sku,product_name,category," +
            "quantity,unit_price,discount,payment_method";

    private static final String VALID_ROW =
            "PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Grocery,2,6.50,0.00,CASH";

    private static final String BAD_ROW =
            "PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Grocery,0,6.50,0.00,CASH";

    @TempDir
    Path tempDir;

    // ── Event-count tests ────────────────────────────────────────────────────

    @Test
    @DisplayName("two CSV files → two FileLoaded events + one LoadFinished")
    void twoFiles_correctEventCount() throws Exception {
        writeFile("PNH-2026-09-01.csv", HEADER, VALID_ROW, VALID_ROW);
        writeFile("REP-2026-09-01.csv", HEADER, VALID_ROW);

        var events = new ArrayList<LoadEvent>();
        var loader = new MonthLoader(tempDir, new CsvTransactionParser());
        loader.addListener(events::add);

        loader.load();

        long fileLoadedCount = events.stream()
                .filter(e -> e instanceof LoadEvent.FileLoaded).count();
        long finishedCount = events.stream()
                .filter(e -> e instanceof LoadEvent.LoadFinished).count();

        assertEquals(2, fileLoadedCount, "expected one FileLoaded per CSV file");
        assertEquals(1, finishedCount,   "expected exactly one LoadFinished");
    }

    @Test
    @DisplayName("LoadFinished reports correct total transaction count")
    void loadFinished_correctTotalTransactions() throws Exception {
        // 3 valid rows across two files
        writeFile("PNH-2026-09-01.csv", HEADER, VALID_ROW, VALID_ROW);
        writeFile("REP-2026-09-01.csv", HEADER, VALID_ROW);

        var loader = new MonthLoader(tempDir, new CsvTransactionParser());
        var finished = new ArrayList<LoadEvent.LoadFinished>();
        loader.addListener(e -> {
            if (e instanceof LoadEvent.LoadFinished lf) finished.add(lf);
        });

        loader.load();

        assertEquals(1, finished.size());
        assertEquals(3L, finished.get(0).totalTransactions(),
                "total transactions must equal sum of valid rows");
    }

    @Test
    @DisplayName("invalid rows are counted in error totals, not transactions")
    void invalidRows_countedAsErrors() throws Exception {
        writeFile("BTB-2026-09-01.csv", HEADER, VALID_ROW, BAD_ROW, VALID_ROW);

        var loader = new MonthLoader(tempDir, new CsvTransactionParser());
        var finished = new ArrayList<LoadEvent.LoadFinished>();
        loader.addListener(e -> {
            if (e instanceof LoadEvent.LoadFinished lf) finished.add(lf);
        });

        loader.load();

        assertEquals(1, finished.size());
        assertEquals(2L, finished.get(0).totalTransactions(), "two valid rows");
        assertEquals(1L, finished.get(0).totalErrors(),       "one bad row");
    }

    // ── Unsubscription test ──────────────────────────────────────────────────

    @Test
    @DisplayName("unsubscribed listener receives 0 events")
    void unsubscribedListener_receivesNoEvents() throws Exception {
        writeFile("PNH-2026-09-01.csv", HEADER, VALID_ROW);

        var loader = new MonthLoader(tempDir, new CsvTransactionParser());
        var events = new ArrayList<LoadEvent>();

        // Subscribe then immediately unsubscribe
        Runnable unsubscribe = loader.addListener(events::add);
        unsubscribe.run();

        loader.load();

        assertEquals(0, events.size(),
                "unsubscribed listener must not receive any events");
    }

    @Test
    @DisplayName("active listener still receives events after another unsubscribes")
    void otherUnsubscribe_doesNotAffectActiveListener() throws Exception {
        writeFile("PNH-2026-09-01.csv", HEADER, VALID_ROW);

        var loader  = new MonthLoader(tempDir, new CsvTransactionParser());
        var active  = new ArrayList<LoadEvent>();
        var removed = new ArrayList<LoadEvent>();

        loader.addListener(active::add);
        Runnable unsubscribe = loader.addListener(removed::add);
        unsubscribe.run();

        loader.load();

        assertFalse(active.isEmpty(), "active listener must receive events");
        assertTrue(removed.isEmpty(), "removed listener must receive nothing");
    }

    // ── Empty directory ──────────────────────────────────────────────────────

    @Test
    @DisplayName("empty directory → zero FileLoaded, one LoadFinished with zeros")
    void emptyDirectory_oneLoadFinishedWithZeros() throws Exception {
        var loader   = new MonthLoader(tempDir, new CsvTransactionParser());
        var finished = new ArrayList<LoadEvent.LoadFinished>();
        loader.addListener(e -> {
            if (e instanceof LoadEvent.LoadFinished lf) finished.add(lf);
        });

        loader.load();

        assertEquals(1, finished.size());
        assertEquals(0L, finished.get(0).totalTransactions());
        assertEquals(0L, finished.get(0).totalErrors());
        assertEquals(0,  finished.get(0).skippedFiles());
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private void writeFile(String name, String... lines) throws Exception {
        Files.write(tempDir.resolve(name), List.of(lines));
    }
}
