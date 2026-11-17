package de.jexcellence.dependency.model;

import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Times the phases of one startup and renders them as one log line, so server owners can see what loading
 * libraries costs and whether the cache works. Not thread-safe; one report belongs to one startup.
 */
public final class StartupReport {

    /** Startup phases in the order they run. */
    public enum Phase {
        /** Reading POM files for transitive dependencies. */
        RESOLVE,
        /** Downloading or reusing cached jars, including checksum checks. */
        DOWNLOAD,
        /** Relocating packages. */
        REMAP,
        /** Putting jars on the classpath. */
        INJECT
    }

    private final long started = System.nanoTime();
    private final Map<Phase, Long> nanos = new EnumMap<>(Phase.class);
    private int declared;
    private int transitive;
    private int cached;
    private int downloaded;
    private int failed;

    /**
     * Runs a phase and adds its duration to the report.
     *
     * @param phase  phase to time
     * @param action work of the phase
     * @param <T>    result type
     * @return the action's result
     */
    public <T> T time(@NotNull final Phase phase, @NotNull final Supplier<T> action) {
        final long start = System.nanoTime();
        try {
            return action.get();
        } finally {
            nanos.merge(phase, System.nanoTime() - start, Long::sum);
        }
    }

    /**
     * Records how many libraries were declared and how many transitive ones resolution added.
     *
     * @param declared   libraries listed by the plugin
     * @param transitive libraries added by transitive resolution
     * @return this report
     */
    public @NotNull StartupReport libraries(final int declared, final int transitive) {
        this.declared = declared;
        this.transitive = transitive;
        return this;
    }

    /**
     * Records the download outcome.
     *
     * @param cached     jars reused from the shared cache
     * @param downloaded jars fetched from a repository
     * @param failed     jars that could not be loaded
     * @return this report
     */
    public @NotNull StartupReport downloads(final int cached, final int downloaded, final int failed) {
        this.cached = cached;
        this.downloaded = downloaded;
        this.failed = failed;
        return this;
    }

    /**
     * Returns the time spent in a phase.
     *
     * @param phase phase to read
     * @return milliseconds, {@code 0} when the phase did not run
     */
    public long millis(@NotNull final Phase phase) {
        return TimeUnit.NANOSECONDS.toMillis(nanos.getOrDefault(phase, 0L));
    }

    /**
     * Renders the report, for example {@code Loaded 12 libraries (3 transitive) in 840 ms: resolve 120 ms,
     * download 600 ms (9 cached, 3 downloaded), inject 40 ms}.
     *
     * @return one line for the server log
     */
    public @NotNull String summary() {
        final long total = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        final StringBuilder line = new StringBuilder("Loaded ")
                .append(declared + transitive - failed).append(" libraries");
        if (transitive > 0) {
            line.append(" (").append(transitive).append(" transitive)");
        }
        line.append(" in ").append(total).append(" ms");
        String separator = ": ";
        for (final Phase phase : Phase.values()) {
            if (!nanos.containsKey(phase)) {
                continue;
            }
            line.append(separator).append(phase.name().toLowerCase(java.util.Locale.ROOT)).append(' ')
                    .append(millis(phase)).append(" ms");
            if (phase == Phase.DOWNLOAD) {
                line.append(" (").append(cached).append(" cached, ").append(downloaded).append(" downloaded)");
            }
            separator = ", ";
        }
        if (failed > 0) {
            line.append("; ").append(failed).append(" failed");
        }
        return line.toString();
    }
}
