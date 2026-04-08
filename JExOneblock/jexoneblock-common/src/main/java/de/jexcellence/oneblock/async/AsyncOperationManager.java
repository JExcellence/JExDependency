package de.jexcellence.oneblock.async;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Async Operation Manager
 * 
 * Manages asynchronous operations with intelligent thread pool management,
 * operation prioritization, and performance monitoring integration.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class AsyncOperationManager {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    // Thread pools for different operation types
    private final ExecutorService databaseExecutor;
    private final ExecutorService computationExecutor;
    private final ExecutorService ioExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    
    // Simple operation counters (replacing PerformanceMonitor)
    private final ConcurrentHashMap<String, AtomicLong> operationCounters = new ConcurrentHashMap<>();
    
    // Operation tracking
    private final AtomicInteger activeOperations = new AtomicInteger(0);
    private final ConcurrentHashMap<String, CompletableFuture<?>> namedOperations = new ConcurrentHashMap<>();
    
    private static AsyncOperationManager instance;
    
    private AsyncOperationManager() {
        
        // Database operations pool - optimized for I/O bound operations
        this.databaseExecutor = new ThreadPoolExecutor(
            2, 8, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new NamedThreadFactory("OneBlock-DB"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // Computation pool - optimized for CPU bound operations
        this.computationExecutor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() * 2,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(50),
            new NamedThreadFactory("OneBlock-Compute"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // I/O operations pool - for file operations and external calls
        this.ioExecutor = new ThreadPoolExecutor(
            1, 4, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(25),
            new NamedThreadFactory("OneBlock-IO"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // Scheduled operations
        this.scheduledExecutor = Executors.newScheduledThreadPool(2, 
            new NamedThreadFactory("OneBlock-Scheduled"));
        
        // Start monitoring
        startMonitoring();
        
        LOGGER.info("AsyncOperationManager initialized with optimized thread pools");
    }
    
    /**
     * Gets the singleton instance
     */
    @NotNull
    public static synchronized AsyncOperationManager getInstance() {
        if (instance == null) {
            instance = new AsyncOperationManager();
        }
        return instance;
    }
    
    /**
     * Executes a database operation asynchronously
     * 
     * @param operation the database operation
     * @param <T> the result type
     * @return CompletableFuture with the result
     */
    @NotNull
    public <T> CompletableFuture<T> executeDatabase(@NotNull Supplier<T> operation) {
        return executeWithMonitoring("database_operation", operation, databaseExecutor);
    }
    
    /**
     * Executes a database operation asynchronously with callback
     * 
     * @param operation the database operation
     * @param onSuccess callback for successful completion
     * @param onError callback for error handling
     * @param <T> the result type
     */
    public <T> void executeDatabase(@NotNull Supplier<T> operation, 
                                  @Nullable Consumer<T> onSuccess,
                                  @Nullable Consumer<Throwable> onError) {
        executeDatabase(operation)
            .whenComplete((result, throwable) -> {
                if (throwable != null && onError != null) {
                    onError.accept(throwable);
                } else if (result != null && onSuccess != null) {
                    onSuccess.accept(result);
                }
            });
    }
    
    /**
     * Executes a computation operation asynchronously
     * 
     * @param operation the computation operation
     * @param <T> the result type
     * @return CompletableFuture with the result
     */
    @NotNull
    public <T> CompletableFuture<T> executeComputation(@NotNull Supplier<T> operation) {
        return executeWithMonitoring("computation_operation", operation, computationExecutor);
    }
    
    /**
     * Executes an I/O operation asynchronously
     * 
     * @param operation the I/O operation
     * @param <T> the result type
     * @return CompletableFuture with the result
     */
    @NotNull
    public <T> CompletableFuture<T> executeIO(@NotNull Supplier<T> operation) {
        return executeWithMonitoring("io_operation", operation, ioExecutor);
    }
    
    /**
     * Executes a named operation that can be tracked and cancelled
     * 
     * @param operationName unique name for the operation
     * @param operation the operation to execute
     * @param <T> the result type
     * @return CompletableFuture with the result
     */
    @NotNull
    public <T> CompletableFuture<T> executeNamed(@NotNull String operationName, 
                                               @NotNull Supplier<T> operation) {
        // Cancel existing operation with same name if it exists
        cancelNamedOperation(operationName);
        
        CompletableFuture<T> future = executeComputation(operation);
        namedOperations.put(operationName, future);
        
        // Remove from tracking when completed
        future.whenComplete((result, throwable) -> namedOperations.remove(operationName));
        
        return future;
    }
    
    /**
     * Schedules a recurring operation
     * 
     * @param operation the operation to execute
     * @param initialDelay initial delay before first execution
     * @param period period between executions
     * @param unit time unit
     * @return ScheduledFuture for the recurring operation
     */
    @NotNull
    public ScheduledFuture<?> scheduleRecurring(@NotNull Runnable operation,
                                              long initialDelay, long period, 
                                              @NotNull TimeUnit unit) {
        Runnable monitoredOperation = () -> {
            try {
                operation.run();
                incrementCounter("scheduled_operations_completed");
            } catch (Exception e) {
                incrementCounter("scheduled_operations_failed");
                LOGGER.warning("Scheduled operation failed: " + e.getMessage());
            }
        };
        
        return scheduledExecutor.scheduleAtFixedRate(monitoredOperation, initialDelay, period, unit);
    }
    
    /**
     * Schedules a one-time delayed operation
     * 
     * @param operation the operation to execute
     * @param delay delay before execution
     * @param unit time unit
     * @return ScheduledFuture for the operation
     */
    @NotNull
    public ScheduledFuture<?> scheduleDelayed(@NotNull Runnable operation, 
                                            long delay, @NotNull TimeUnit unit) {
        Runnable monitoredOperation = () -> {
            try {
                operation.run();
                incrementCounter("delayed_operations_completed");
            } catch (Exception e) {
                incrementCounter("delayed_operations_failed");
                LOGGER.warning("Delayed operation failed: " + e.getMessage());
            }
        };
        
        return scheduledExecutor.schedule(monitoredOperation, delay, unit);
    }
    
    /**
     * Executes multiple operations in parallel and combines results
     * 
     * @param operations list of operations to execute
     * @param <T> the result type
     * @return CompletableFuture with list of results
     */
    @NotNull
    public <T> CompletableFuture<java.util.List<T>> executeParallel(@NotNull java.util.List<Supplier<T>> operations) {
        java.util.List<CompletableFuture<T>> futures = operations.stream()
            .map(this::executeComputation)
            .collect(java.util.stream.Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(java.util.stream.Collectors.toList()));
    }
    
    /**
     * Executes operations in sequence with dependency chaining
     * 
     * @param operations list of operations where each depends on the previous result
     * @param initialValue initial value for the chain
     * @param <T> the value type
     * @return CompletableFuture with final result
     */
    @NotNull
    public <T> CompletableFuture<T> executeChain(@NotNull java.util.List<Function<T, T>> operations, 
                                               @NotNull T initialValue) {
        CompletableFuture<T> result = CompletableFuture.completedFuture(initialValue);
        
        for (Function<T, T> operation : operations) {
            result = result.thenApplyAsync(operation, computationExecutor);
        }
        
        return result;
    }
    
    /**
     * Cancels a named operation
     * 
     * @param operationName the operation name
     * @return true if operation was cancelled, false if not found
     */
    public boolean cancelNamedOperation(@NotNull String operationName) {
        CompletableFuture<?> future = namedOperations.remove(operationName);
        if (future != null && !future.isDone()) {
            return future.cancel(true);
        }
        return false;
    }
    
    /**
     * Gets the number of active operations
     * 
     * @return number of active operations
     */
    public int getActiveOperationCount() {
        return activeOperations.get();
    }
    
    /**
     * Gets thread pool statistics
     * 
     * @return thread pool statistics
     */
    @NotNull
    public ThreadPoolStats getThreadPoolStats() {
        return new ThreadPoolStats(
            getPoolStats("Database", databaseExecutor),
            getPoolStats("Computation", computationExecutor),
            getPoolStats("IO", ioExecutor),
            getScheduledPoolStats("Scheduled", scheduledExecutor)
        );
    }
    
    /**
     * Executes operation with tracking
     */
    @NotNull
    private <T> CompletableFuture<T> executeWithMonitoring(@NotNull String operationName,
                                                         @NotNull Supplier<T> operation,
                                                         @NotNull ExecutorService executor) {
        activeOperations.incrementAndGet();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                T result = operation.get();
                incrementCounter(operationName + "_completed");
                return result;
            } catch (Exception e) {
                incrementCounter(operationName + "_failed");
                throw new RuntimeException(e);
            }
        }, executor).whenComplete((result, throwable) -> {
            activeOperations.decrementAndGet();
            if (throwable != null) {
                LOGGER.warning("Async operation failed: " + throwable.getMessage());
            }
        });
    }
    
    /**
     * Increments a counter
     */
    private void incrementCounter(@NotNull String name) {
        operationCounters.computeIfAbsent(name, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * Adds to a counter
     */
    private void addToCounter(@NotNull String name, long value) {
        operationCounters.computeIfAbsent(name, k -> new AtomicLong(0)).addAndGet(value);
    }
    
    /**
     * Gets statistics for a thread pool executor
     */
    @NotNull
    private PoolStats getPoolStats(@NotNull String name, @NotNull ExecutorService executor) {
        if (executor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;
            return new PoolStats(
                name,
                tpe.getCorePoolSize(),
                tpe.getMaximumPoolSize(),
                tpe.getActiveCount(),
                tpe.getPoolSize(),
                tpe.getQueue().size(),
                tpe.getCompletedTaskCount()
            );
        }
        return new PoolStats(name, 0, 0, 0, 0, 0, 0);
    }
    
    /**
     * Gets statistics for a scheduled thread pool executor
     */
    @NotNull
    private PoolStats getScheduledPoolStats(@NotNull String name, @NotNull ScheduledExecutorService executor) {
        if (executor instanceof ScheduledThreadPoolExecutor) {
            ScheduledThreadPoolExecutor stpe = (ScheduledThreadPoolExecutor) executor;
            return new PoolStats(
                name,
                stpe.getCorePoolSize(),
                stpe.getMaximumPoolSize(),
                stpe.getActiveCount(),
                stpe.getPoolSize(),
                stpe.getQueue().size(),
                stpe.getCompletedTaskCount()
            );
        }
        return new PoolStats(name, 0, 0, 0, 0, 0, 0);
    }
    
    /**
     * Starts monitoring thread pools
     */
    private void startMonitoring() {
        scheduleRecurring(() -> {
            ThreadPoolStats stats = getThreadPoolStats();
            
            // Log warnings for high queue sizes
            if (stats.getDatabaseStats().getQueueSize() > 50) {
                LOGGER.warning("Database thread pool queue is getting full: " + 
                    stats.getDatabaseStats().getQueueSize());
            }
            
            if (stats.getComputationStats().getQueueSize() > 25) {
                LOGGER.warning("Computation thread pool queue is getting full: " + 
                    stats.getComputationStats().getQueueSize());
            }
            
            // Update counters
            addToCounter("async_active_operations", activeOperations.get());
            addToCounter("async_named_operations", namedOperations.size());
            
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    /**
     * Shuts down all thread pools
     */
    public void shutdown() {
        LOGGER.info("Shutting down AsyncOperationManager...");
        
        // Cancel all named operations
        namedOperations.values().forEach(future -> future.cancel(true));
        namedOperations.clear();
        
        // Shutdown thread pools
        shutdownExecutor("Database", databaseExecutor);
        shutdownExecutor("Computation", computationExecutor);
        shutdownExecutor("IO", ioExecutor);
        shutdownExecutor("Scheduled", scheduledExecutor);
        
        LOGGER.info("AsyncOperationManager shut down completed");
    }
    
    /**
     * Shuts down an executor service gracefully
     */
    private void shutdownExecutor(@NotNull String name, @NotNull ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                LOGGER.warning(name + " executor did not terminate gracefully, forcing shutdown");
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.severe(name + " executor did not terminate after forced shutdown");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Named thread factory for better debugging
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        
        public NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix + "-";
        }
        
        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
    
    /**
     * Thread pool statistics
     */
    public static class ThreadPoolStats {
        private final PoolStats databaseStats;
        private final PoolStats computationStats;
        private final PoolStats ioStats;
        private final PoolStats scheduledStats;
        
        public ThreadPoolStats(PoolStats databaseStats, PoolStats computationStats, 
                             PoolStats ioStats, PoolStats scheduledStats) {
            this.databaseStats = databaseStats;
            this.computationStats = computationStats;
            this.ioStats = ioStats;
            this.scheduledStats = scheduledStats;
        }
        
        public PoolStats getDatabaseStats() { return databaseStats; }
        public PoolStats getComputationStats() { return computationStats; }
        public PoolStats getIoStats() { return ioStats; }
        public PoolStats getScheduledStats() { return scheduledStats; }
        
        @Override
        public String toString() {
            return String.format("ThreadPoolStats{db=%s, compute=%s, io=%s, scheduled=%s}",
                databaseStats, computationStats, ioStats, scheduledStats);
        }
    }
    
    /**
     * Individual pool statistics
     */
    public static class PoolStats {
        private final String name;
        private final int corePoolSize;
        private final int maximumPoolSize;
        private final int activeCount;
        private final int poolSize;
        private final int queueSize;
        private final long completedTaskCount;
        
        public PoolStats(String name, int corePoolSize, int maximumPoolSize, 
                        int activeCount, int poolSize, int queueSize, long completedTaskCount) {
            this.name = name;
            this.corePoolSize = corePoolSize;
            this.maximumPoolSize = maximumPoolSize;
            this.activeCount = activeCount;
            this.poolSize = poolSize;
            this.queueSize = queueSize;
            this.completedTaskCount = completedTaskCount;
        }
        
        public String getName() { return name; }
        public int getCorePoolSize() { return corePoolSize; }
        public int getMaximumPoolSize() { return maximumPoolSize; }
        public int getActiveCount() { return activeCount; }
        public int getPoolSize() { return poolSize; }
        public int getQueueSize() { return queueSize; }
        public long getCompletedTaskCount() { return completedTaskCount; }
        
        @Override
        public String toString() {
            return String.format("%s{active=%d/%d, queue=%d, completed=%d}",
                name, activeCount, poolSize, queueSize, completedTaskCount);
        }
    }
}