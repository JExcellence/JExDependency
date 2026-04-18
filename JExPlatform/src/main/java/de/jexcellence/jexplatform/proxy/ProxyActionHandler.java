package de.jexcellence.jexplatform.proxy;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Async handler invoked for one proxy action envelope.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@FunctionalInterface
public interface ProxyActionHandler {

    /**
     * Handles an action envelope and returns an async action result.
     *
     * @param envelope the action envelope
     * @return an async result
     */
    @NotNull CompletableFuture<ProxyActionResult> handle(
            @NotNull ProxyActionEnvelope envelope);
}
