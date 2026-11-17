package de.jexcellence.dependency.downloader;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates the executor downloads run on: virtual threads on Java 21 and newer, a pool of daemon threads on older
 * runtimes. Looked up at runtime so the library also runs on Java 16 and 17 servers.
 */
public final class DownloadThreads {

    private DownloadThreads() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Creates a new executor.
     *
     * @param name prefix for thread names on runtimes without virtual threads
     * @return an executor; the caller shuts it down
     */
    public static @NotNull ExecutorService newExecutor(@NotNull final String name) {
        try {
            return (ExecutorService) MethodHandles.publicLookup()
                    .findStatic(Executors.class, "newVirtualThreadPerTaskExecutor", MethodType.methodType(ExecutorService.class))
                    .invoke();
        } catch (final Throwable unavailable) {
            final AtomicInteger counter = new AtomicInteger();
            final ThreadFactory factory = runnable -> {
                final Thread thread = new Thread(runnable, name + "-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            };
            return Executors.newCachedThreadPool(factory);
        }
    }
}
