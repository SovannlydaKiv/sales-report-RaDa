package edu.itc.salesreport.ingest;

/**
 * Observer that receives {@link LoadEvent}s from a {@link MonthLoader}.
 *
 * <p>Implementations are called synchronously on the loader's thread,
 * so they must return quickly. Marked {@code @FunctionalInterface} so
 * that callers can supply lambda expressions.
 */
@FunctionalInterface
public interface LoadListener {

    /**
     * Called for each event emitted by the loader.
     *
     * @param event the event (never {@code null})
     */
    void onEvent(LoadEvent event);
}
