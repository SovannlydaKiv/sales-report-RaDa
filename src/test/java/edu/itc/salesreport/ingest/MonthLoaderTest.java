package edu.itc.salesreport.ingest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MonthLoaderTest {

    @Test
    void loadsFilesInOrderAndPublishesEvents(@TempDir Path tempDir) throws IOException {
        Path monthDir = Files.createDirectories(tempDir.resolve("2026-09"));
        Files.write(monthDir.resolve("BTB-2026-09-01.csv"), List.of(
                CsvTransactionParser.HEADER,
                "BTB,2026-09-01,BTB-000001,SKU-1001,Jasmine Rice 5kg,Grocery,2,6.50,0.00,CASH",
                "BTB,2026-09-01,BTB-000002,SKU-1002,Fish Sauce 700ml,Grocery,bad,1.80,0.00,CASH"
        ));
        Files.write(monthDir.resolve("PNH-2026-09-01.csv"), List.of(
                CsvTransactionParser.HEADER,
                "PNH,2026-09-01,PNH-000001,SKU-2001,Mineral Water 1.5L,Beverages,1,0.45,0.00,KHQR"
        ));

        var loader = new MonthLoader(new CsvTransactionParser());
        var events = new ArrayList<LoadEvent>();
        loader.subscribe(events::add);

        var transactions = loader.load(YearMonth.of(2026, 9), tempDir);

        assertEquals(2, transactions.size());
        assertEquals(1, loader.rejectedRows().size());
        assertTrue(loader.rejectedRows().getFirst().startsWith("BTB-2026-09-01.csv:3 bad quantity 'bad'"));

        assertEquals(3, events.size());
        assertInstanceOf(LoadEvent.FileLoaded.class, events.get(0));
        assertInstanceOf(LoadEvent.FileLoaded.class, events.get(1));
        assertInstanceOf(LoadEvent.LoadFinished.class, events.get(2));

        var first = (LoadEvent.FileLoaded) events.get(0);
        assertEquals(1, first.accepted());
        assertEquals(1, first.rejected());
        assertEquals(1, first.filesDone());
        assertEquals(2, first.filesTotal());

        var finished = (LoadEvent.LoadFinished) events.get(2);
        assertEquals(2, finished.accepted());
        assertEquals(1, finished.rejected());
    }

    @Test
    void unsubscribedListenerReceivesNoEvents(@TempDir Path tempDir) throws IOException {
        Path monthDir = Files.createDirectories(tempDir.resolve("2026-09"));
        Files.write(monthDir.resolve("PNH-2026-09-01.csv"), List.of(
                CsvTransactionParser.HEADER,
                "PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Grocery,1,6.50,0.00,CASH"
        ));

        var loader = new MonthLoader(new CsvTransactionParser());
        var events = new ArrayList<LoadEvent>();
        Runnable unsubscribe = loader.subscribe(events::add);
        unsubscribe.run();

        loader.load(YearMonth.of(2026, 9), tempDir);
        assertEquals(List.of(), events);
    }
}