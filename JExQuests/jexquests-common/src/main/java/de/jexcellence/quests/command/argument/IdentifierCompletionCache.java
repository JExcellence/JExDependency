package de.jexcellence.quests.command.argument;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Shared TTL-cached completion backbone for the custom argument
 * types. Tab-completion runs synchronously on the main thread — this
 * cache swallows the inevitable DB fetch into a bounded async call
 * and stales-out to a short-lived in-memory list.
 *
 * <p>The supplier is allowed to block briefly; if it throws or
 * times out, the last known list stays visible until the next
 * refresh tick passes.
 */
public final class IdentifierCompletionCache {

    private static final long DEFAULT_TTL_MILLIS = 4_000L;

    private final long ttlMillis;
    private final Supplier<List<String>> supplier;
    private volatile List<String> cached = List.of();
    private volatile long cachedAt;

    public IdentifierCompletionCache(@NotNull Supplier<List<String>> supplier) {
        this(DEFAULT_TTL_MILLIS, supplier);
    }

    public IdentifierCompletionCache(long ttlMillis, @NotNull Supplier<List<String>> supplier) {
        this.ttlMillis = ttlMillis;
        this.supplier = supplier;
    }

    /** Returns identifiers that start with {@code partial}, case-insensitive, sorted. */
    public @NotNull List<String> matching(@NotNull String partial) {
        refreshIfStale();
        final String lower = partial.toLowerCase(Locale.ROOT);
        return this.cached.stream()
                .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted()
                .toList();
    }

    private synchronized void refreshIfStale() {
        final long now = System.currentTimeMillis();
        if (!this.cached.isEmpty() && now - this.cachedAt < this.ttlMillis) return;
        try {
            final List<String> next = this.supplier.get();
            if (next != null) {
                this.cached = next;
                this.cachedAt = now;
            }
        } catch (final RuntimeException ex) {
            // Keep the previous snapshot visible; worst case is a stale
            // completion list, never a broken tab-complete.
        }
    }
}
