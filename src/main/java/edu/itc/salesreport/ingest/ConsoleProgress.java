package edu.itc.salesreport.ingest;

/**
 * {@link LoadListener} that prints a formatted progress line to
 * {@link System#out} for each {@link LoadEvent}.
 *
 * <p>Output format examples:
 * <pre>
 * [LOADED]  PNH-2026-09-01.csv  tx=200  err=0
 * [LOADED]  REP-2026-09-02.csv  tx=199  err=1
 * [DONE]    files=15  total_tx=2985  total_err=2  skipped=0
 * </pre>
 */
public class ConsoleProgress implements LoadListener {

    @Override
    public void onEvent(LoadEvent event) {
        switch (event) {
            case LoadEvent.FileLoaded fl ->
                System.out.printf("[LOADED]  %-30s  tx=%-6d  err=%d%n",
                        fl.file().getFileName(),
                        fl.transactionCount(),
                        fl.errorCount());
            case LoadEvent.LoadFinished lf ->
                System.out.printf("[DONE]    total_tx=%-8d  total_err=%-4d  skipped=%d%n",
                        lf.totalTransactions(),
                        lf.totalErrors(),
                        lf.skippedFiles());
        }
    }
}
