package de.jexcellence.oneblock.memory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Object Pool Manager
 * 
 * Manages object pools for frequently created objects to reduce garbage collection
 * pressure and improve performance through object reuse.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class ObjectPoolManager {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final ConcurrentHashMap<String, ObjectPool<?>> pools = new ConcurrentHashMap<>();
    
    /**
     * Creates or gets an object pool
     * 
     * @param name pool name
     * @param factory object factory
     * @param maxSize maximum pool size
     * @param <T> object type
     * @return object pool
     */
    @NotNull
    public <T> ObjectPool<T> getOrCreatePool(@NotNull String name, @NotNull Supplier<T> factory, int maxSize) {
        @SuppressWarnings("unchecked")
        ObjectPool<T> pool = (ObjectPool<T>) pools.computeIfAbsent(name, k -> new ObjectPool<>(name, factory, maxSize));
        return pool;
    }
    
    /**
     * Gets an existing pool
     * 
     * @param name pool name
     * @param <T> object type
     * @return object pool or null if not found
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> ObjectPool<T> getPool(@NotNull String name) {
        return (ObjectPool<T>) pools.get(name);
    }
    
    /**
     * Removes a pool
     * 
     * @param name pool name
     */
    public void removePool(@NotNull String name) {
        ObjectPool<?> pool = pools.remove(name);
        if (pool != null) {
            pool.clear();
            LOGGER.fine("Removed object pool: " + name);
        }
    }
    
    /**
     * Performs cleanup based on memory pressure
     * 
     * @param level memory pressure level
     */
    public void cleanup(@NotNull MemoryManager.MemoryPressureLevel level) {
        int totalCleaned = 0;
        
        for (ObjectPool<?> pool : pools.values()) {
            int cleaned = pool.cleanup(level);
            totalCleaned += cleaned;
        }
        
        if (totalCleaned > 0) {
            LOGGER.fine("Object pool cleanup: removed " + totalCleaned + " objects (level: " + level + ")");
        }
    }
    
    /**
     * Gets pool statistics
     * 
     * @return pool statistics
     */
    @NotNull
    public PoolStatistics getStatistics() {
        int totalPools = pools.size();
        int totalObjects = 0;
        int totalCapacity = 0;
        long totalBorrows = 0;
        long totalReturns = 0;
        
        for (ObjectPool<?> pool : pools.values()) {
            ObjectPool.PoolStats stats = pool.getStatistics();
            totalObjects += stats.getCurrentSize();
            totalCapacity += stats.getMaxSize();
            totalBorrows += stats.getBorrowCount();
            totalReturns += stats.getReturnCount();
        }
        
        double averageUtilization = totalCapacity > 0 ? (double) totalObjects / totalCapacity : 0.0;
        
        return new PoolStatistics(totalPools, totalObjects, totalCapacity, 
            averageUtilization, totalBorrows, totalReturns);
    }
    
    /**
     * Shuts down all pools
     */
    public void shutdown() {
        for (ObjectPool<?> pool : pools.values()) {
            pool.clear();
        }
        pools.clear();
        LOGGER.info("ObjectPoolManager shut down");
    }
    
    /**
     * Object pool implementation
     */
    public static class ObjectPool<T> {
        private final String name;
        private final Supplier<T> factory;
        private final int maxSize;
        private final ConcurrentLinkedQueue<T> pool = new ConcurrentLinkedQueue<>();
        
        // Statistics
        private final AtomicInteger currentSize = new AtomicInteger(0);
        private final AtomicLong borrowCount = new AtomicLong(0);
        private final AtomicLong returnCount = new AtomicLong(0);
        private final AtomicLong createCount = new AtomicLong(0);
        
        public ObjectPool(@NotNull String name, @NotNull Supplier<T> factory, int maxSize) {
            this.name = name;
            this.factory = factory;
            this.maxSize = maxSize;
        }
        
        /**
         * Borrows an object from the pool
         * 
         * @return object from pool or newly created
         */
        @NotNull
        public T borrow() {
            borrowCount.incrementAndGet();
            
            T object = pool.poll();
            if (object != null) {
                currentSize.decrementAndGet();
                return object;
            }
            
            // Create new object if pool is empty
            createCount.incrementAndGet();
            return factory.get();
        }
        
        /**
         * Returns an object to the pool
         * 
         * @param object object to return
         */
        public void returnObject(@NotNull T object) {
            returnCount.incrementAndGet();
            
            if (currentSize.get() < maxSize) {
                // Reset object state if it implements Poolable
                if (object instanceof Poolable) {
                    ((Poolable) object).reset();
                }
                
                pool.offer(object);
                currentSize.incrementAndGet();
            }
            // If pool is full, let object be garbage collected
        }
        
        /**
         * Clears the pool
         */
        public void clear() {
            pool.clear();
            currentSize.set(0);
        }
        
        /**
         * Performs cleanup based on memory pressure
         * 
         * @param level memory pressure level
         * @return number of objects removed
         */
        public int cleanup(@NotNull MemoryManager.MemoryPressureLevel level) {
            int toRemove = 0;
            
            switch (level) {
                case CRITICAL:
                    toRemove = currentSize.get(); // Remove all
                    break;
                case HIGH:
                    toRemove = currentSize.get() / 2; // Remove half
                    break;
                case MEDIUM:
                    toRemove = currentSize.get() / 4; // Remove quarter
                    break;
                case LOW:
                    // No cleanup needed
                    break;
            }
            
            int removed = 0;
            for (int i = 0; i < toRemove && !pool.isEmpty(); i++) {
                if (pool.poll() != null) {
                    currentSize.decrementAndGet();
                    removed++;
                }
            }
            
            return removed;
        }
        
        /**
         * Gets pool statistics
         * 
         * @return pool statistics
         */
        @NotNull
        public PoolStats getStatistics() {
            return new PoolStats(
                name, currentSize.get(), maxSize,
                borrowCount.get(), returnCount.get(), createCount.get()
            );
        }
        
        /**
         * Pool statistics
         */
        public static class PoolStats {
            private final String name;
            private final int currentSize;
            private final int maxSize;
            private final long borrowCount;
            private final long returnCount;
            private final long createCount;
            
            public PoolStats(String name, int currentSize, int maxSize, 
                           long borrowCount, long returnCount, long createCount) {
                this.name = name;
                this.currentSize = currentSize;
                this.maxSize = maxSize;
                this.borrowCount = borrowCount;
                this.returnCount = returnCount;
                this.createCount = createCount;
            }
            
            public String getName() { return name; }
            public int getCurrentSize() { return currentSize; }
            public int getMaxSize() { return maxSize; }
            public long getBorrowCount() { return borrowCount; }
            public long getReturnCount() { return returnCount; }
            public long getCreateCount() { return createCount; }
            
            public double getUtilization() {
                return maxSize > 0 ? (double) currentSize / maxSize : 0.0;
            }
            
            public double getHitRate() {
                return borrowCount > 0 ? (double) (borrowCount - createCount) / borrowCount : 0.0;
            }
            
            @Override
            public String toString() {
                return String.format("%s{size=%d/%d, util=%.1f%%, hit=%.1f%%, borrows=%d, creates=%d}",
                    name, currentSize, maxSize, getUtilization() * 100, getHitRate() * 100,
                    borrowCount, createCount);
            }
        }
    }
    
    /**
     * Interface for poolable objects
     */
    public interface Poolable {
        /**
         * Resets the object state for reuse
         */
        void reset();
    }
    
    /**
     * Overall pool statistics
     */
    public static class PoolStatistics {
        private final int totalPools;
        private final int totalObjects;
        private final int totalCapacity;
        private final double averageUtilization;
        private final long totalBorrows;
        private final long totalReturns;
        
        public PoolStatistics(int totalPools, int totalObjects, int totalCapacity,
                            double averageUtilization, long totalBorrows, long totalReturns) {
            this.totalPools = totalPools;
            this.totalObjects = totalObjects;
            this.totalCapacity = totalCapacity;
            this.averageUtilization = averageUtilization;
            this.totalBorrows = totalBorrows;
            this.totalReturns = totalReturns;
        }
        
        public int getTotalPools() { return totalPools; }
        public int getTotalObjects() { return totalObjects; }
        public int getTotalCapacity() { return totalCapacity; }
        public double getAverageUtilization() { return averageUtilization; }
        public long getTotalBorrows() { return totalBorrows; }
        public long getTotalReturns() { return totalReturns; }
        
        @Override
        public String toString() {
            return String.format("PoolStats{pools=%d, objects=%d/%d, util=%.1f%%, borrows=%d, returns=%d}",
                totalPools, totalObjects, totalCapacity, averageUtilization * 100,
                totalBorrows, totalReturns);
        }
    }
}