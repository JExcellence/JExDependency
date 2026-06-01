package de.jexcellence.dependency.model;

import org.jetbrains.annotations.NotNull;

/**
 * Callback interface invoked when {@link FailurePolicy#CALLBACK} is active and one or more dependencies
 * failed to download or inject. The callback receives the complete {@link ProcessingResult} and can
 * inspect failures, disable plugin features, or throw to abort startup.
 *
 * <p>Implementations should avoid blocking for extended periods since the callback runs on the
 * initialization thread.</p>
 */
@FunctionalInterface
public interface FailureCallback {

    /**
     * Called after dependency processing completes with at least one failure.
     *
     * @param result processing result containing successful and failed coordinates
     */
    void onFailure(@NotNull ProcessingResult result);
}
