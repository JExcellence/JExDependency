package com.raindropcentral.core.service.statistics.queue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.type.EStatisticType.StatisticDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * Manages persistence of the statistics queue to disk for durability.
 * Supports JSON-based serialization and write-ahead logging.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class QueuePersistenceManager {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");
    private static final String QUEUE_FILE = "statistics-queue.json";
    private static final String WAL_FILE = "statistics-queue.wal";
    private static final String BACKUP_SUFFIX = ".backup";

    private final Path dataFolder;
    private final Path queueFile;
    private final Path walFile;
    private final Gson gson;

    private volatile boolean walEnabled = true;

    // Offline mode settings
    private static final int OFFLINE_MAX_CAPACITY = 50000;
    private static final double WARNING_THRESHOLD = 0.75;

    /**
     * Creates a new persistence manager.
     *
     * @param plugin the plugin for data folder access
     */
    public QueuePersistenceManager(final @NotNull Plugin plugin) {
        this.dataFolder = plugin.getDataFolder().toPath();
        this.queueFile = dataFolder.resolve(QUEUE_FILE);
        this.walFile = dataFolder.resolve(WAL_FILE);
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

        ensureDataFolder();
    }

    /**
     * Creates a new persistence manager with a custom path.
     *
     * @param dataFolder the data folder path
     */
    public QueuePersistenceManager(final @NotNull Path dataFolder) {
        this.dataFolder = dataFolder;
        this.queueFile = dataFolder.resolve(QUEUE_FILE);
        this.walFile = dataFolder.resolve(WAL_FILE);
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

        ensureDataFolder();
    }

    private void ensureDataFolder() {
        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            LOGGER.severe("Failed to create data folder: " + e.getMessage());
        }
    }

    /**
     * Persists all queues to disk.
     *
     * @param queues the queues to persist, keyed by priority
     */
    public void persist(final @NotNull Map<DeliveryPriority, ? extends Collection<QueuedStatistic>> queues) {
        try {
            // Create backup of existing file
            if (Files.exists(queueFile)) {
                Files.copy(queueFile, queueFile.resolveSibling(QUEUE_FILE + BACKUP_SUFFIX),
                    StandardCopyOption.REPLACE_EXISTING);
            }

            // Convert to serializable format
            Map<String, List<SerializableStatistic>> serializable = new HashMap<>();
            int totalCount = 0;

            for (var entry : queues.entrySet()) {
                List<SerializableStatistic> list = new ArrayList<>();
                for (QueuedStatistic stat : entry.getValue()) {
                    list.add(SerializableStatistic.from(stat));
                    totalCount++;
                }
                serializable.put(entry.getKey().name(), list);
            }

            // Write to file
            try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(queueFile.toFile()), StandardCharsets.UTF_8))) {
                gson.toJson(serializable, writer);
            }

            // Clear WAL after successful persist
            clearWal();

            LOGGER.fine("Persisted " + totalCount + " statistics to disk");

        } catch (IOException e) {
            LOGGER.severe("Failed to persist queue to disk: " + e.getMessage());
        }
    }

    /**
     * Loads queues from disk.
     *
     * @return the loaded queues, keyed by priority
     */
    public Map<DeliveryPriority, List<QueuedStatistic>> load() {
        Map<DeliveryPriority, List<QueuedStatistic>> result = new EnumMap<>(DeliveryPriority.class);

        // Initialize empty lists for all priorities
        for (DeliveryPriority priority : DeliveryPriority.values()) {
            result.put(priority, new ArrayList<>());
        }

        // Load from main file
        if (Files.exists(queueFile)) {
            try {
                loadFromFile(queueFile, result);
            } catch (Exception e) {
                LOGGER.warning("Failed to load queue file, trying backup: " + e.getMessage());
                // Try backup
                Path backup = queueFile.resolveSibling(QUEUE_FILE + BACKUP_SUFFIX);
                if (Files.exists(backup)) {
                    try {
                        loadFromFile(backup, result);
                    } catch (Exception e2) {
                        LOGGER.severe("Failed to load backup queue file: " + e2.getMessage());
                    }
                }
            }
        }

        // Apply WAL entries
        applyWal(result);

        int totalCount = result.values().stream().mapToInt(List::size).sum();
        LOGGER.info("Loaded " + totalCount + " statistics from disk");

        return result;
    }

    private void loadFromFile(
        final Path file,
        final Map<DeliveryPriority, List<QueuedStatistic>> result
    ) throws IOException {
        try (Reader reader = new BufferedReader(
            new InputStreamReader(new FileInputStream(file.toFile()), StandardCharsets.UTF_8))) {

            Type type = new TypeToken<Map<String, List<SerializableStatistic>>>() {}.getType();
            Map<String, List<SerializableStatistic>> loaded = gson.fromJson(reader, type);

            if (loaded != null) {
                for (var entry : loaded.entrySet()) {
                    try {
                        DeliveryPriority priority = DeliveryPriority.valueOf(entry.getKey());
                        for (SerializableStatistic ser : entry.getValue()) {
                            QueuedStatistic stat = ser.toQueuedStatistic();
                            if (stat != null) {
                                result.get(priority).add(stat);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        LOGGER.warning("Unknown priority in queue file: " + entry.getKey());
                    }
                }
            }
        }
    }

    /**
     * Appends a statistic to the write-ahead log.
     *
     * @param statistic the statistic to log
     */
    public void appendToLog(final @NotNull QueuedStatistic statistic) {
        if (!walEnabled) return;

        try (Writer writer = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(walFile.toFile(), true), StandardCharsets.UTF_8))) {
            writer.write(gson.toJson(SerializableStatistic.from(statistic)));
            writer.write("\n");
        } catch (IOException e) {
            LOGGER.warning("Failed to append to WAL: " + e.getMessage());
        }
    }

    /**
     * Compacts the write-ahead log by removing processed entries.
     */
    public void compactLog() {
        clearWal();
    }

    private void clearWal() {
        try {
            Files.deleteIfExists(walFile);
        } catch (IOException e) {
            LOGGER.warning("Failed to clear WAL: " + e.getMessage());
        }
    }

    private void applyWal(final Map<DeliveryPriority, List<QueuedStatistic>> queues) {
        if (!Files.exists(walFile)) return;

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new FileInputStream(walFile.toFile()), StandardCharsets.UTF_8))) {

            String line;
            int applied = 0;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    SerializableStatistic ser = gson.fromJson(line, SerializableStatistic.class);
                    QueuedStatistic stat = ser.toQueuedStatistic();
                    if (stat != null) {
                        queues.get(stat.priority()).add(stat);
                        applied++;
                    }
                } catch (Exception e) {
                    LOGGER.warning("Failed to parse WAL entry: " + e.getMessage());
                }
            }

            if (applied > 0) {
                LOGGER.info("Applied " + applied + " entries from WAL");
            }

        } catch (IOException e) {
            LOGGER.warning("Failed to read WAL: " + e.getMessage());
        }
    }

    /**
     * Validates queue integrity and repairs if needed.
     */
    public void validateAndRepair() {
        LOGGER.info("Validating queue integrity...");

        if (!Files.exists(queueFile)) {
            LOGGER.info("No queue file found, nothing to validate");
            return;
        }

        try {
            Map<DeliveryPriority, List<QueuedStatistic>> loaded = load();
            int totalCount = loaded.values().stream().mapToInt(List::size).sum();

            // Re-persist to clean up any corrupted entries
            Map<DeliveryPriority, Collection<QueuedStatistic>> toSave = new EnumMap<>(DeliveryPriority.class);
            for (var entry : loaded.entrySet()) {
                toSave.put(entry.getKey(), new ConcurrentLinkedQueue<>(entry.getValue()));
            }
            persist(toSave);

            LOGGER.info("Queue validation complete. " + totalCount + " valid entries.");

        } catch (Exception e) {
            LOGGER.severe("Queue validation failed: " + e.getMessage());
            // Create empty queue file
            try {
                Files.deleteIfExists(queueFile);
                Files.deleteIfExists(walFile);
            } catch (IOException ignored) {}
        }
    }

    /**
     * Enables or disables write-ahead logging.
     *
     * @param enabled true to enable WAL
     */
    public void setWalEnabled(final boolean enabled) {
        this.walEnabled = enabled;
    }

    /**
     * Gets the maximum offline capacity.
     *
     * @return the maximum number of entries for offline mode
     */
    public int getOfflineMaxCapacity() {
        return OFFLINE_MAX_CAPACITY;
    }

    /**
     * Checks if the queue is at warning capacity.
     *
     * @param currentSize the current queue size
     * @return true if at or above warning threshold
     */
    public boolean isAtWarningCapacity(final int currentSize) {
        boolean atWarning = currentSize >= (OFFLINE_MAX_CAPACITY * WARNING_THRESHOLD);
        if (atWarning) {
            LOGGER.warning("Queue at " + String.format("%.0f%%", (currentSize * 100.0 / OFFLINE_MAX_CAPACITY)) +
                " capacity (" + currentSize + "/" + OFFLINE_MAX_CAPACITY + ")");
        }
        return atWarning;
    }

    /**
     * Checks if the queue is at maximum capacity.
     *
     * @param currentSize the current queue size
     * @return true if at maximum capacity
     */
    public boolean isAtMaxCapacity(final int currentSize) {
        return currentSize >= OFFLINE_MAX_CAPACITY;
    }

    /**
     * Discards low-priority entries to make room for higher priority ones.
     *
     * @param queues the queues to discard from
     * @param count  the number of entries to discard
     * @return the number of entries actually discarded
     */
    public int discardLowPriority(
        final @NotNull Map<DeliveryPriority, ? extends Collection<QueuedStatistic>> queues,
        final int count
    ) {
        int discarded = 0;

        // Discard from LOW priority first
        Collection<QueuedStatistic> lowQueue = queues.get(DeliveryPriority.LOW);
        if (lowQueue != null) {
            Iterator<QueuedStatistic> iter = lowQueue.iterator();
            while (iter.hasNext() && discarded < count) {
                iter.next();
                iter.remove();
                discarded++;
            }
        }

        // Then discard from BULK if needed
        if (discarded < count) {
            Collection<QueuedStatistic> bulkQueue = queues.get(DeliveryPriority.BULK);
            if (bulkQueue != null) {
                Iterator<QueuedStatistic> iter = bulkQueue.iterator();
                while (iter.hasNext() && discarded < count) {
                    iter.next();
                    iter.remove();
                    discarded++;
                }
            }
        }

        if (discarded > 0) {
            LOGGER.severe("Discarded " + discarded + " low-priority statistics due to capacity limits");
        }

        return discarded;
    }

    /**
     * Serializable representation of a QueuedStatistic for JSON persistence.
     */
    private record SerializableStatistic(
        String playerUuid,
        String statisticKey,
        Object value,
        String dataType,
        long collectionTimestamp,
        String priority,
        boolean isDelta,
        String sourcePlugin
    ) {
        static SerializableStatistic from(QueuedStatistic stat) {
            return new SerializableStatistic(
                stat.playerUuid().toString(),
                stat.statisticKey(),
                stat.value(),
                stat.dataType().name(),
                stat.collectionTimestamp(),
                stat.priority().name(),
                stat.isDelta(),
                stat.sourcePlugin()
            );
        }

        QueuedStatistic toQueuedStatistic() {
            try {
                return new QueuedStatistic(
                    UUID.fromString(playerUuid),
                    statisticKey,
                    value,
                    StatisticDataType.valueOf(dataType),
                    collectionTimestamp,
                    DeliveryPriority.valueOf(priority),
                    isDelta,
                    sourcePlugin
                );
            } catch (Exception e) {
                return null;
            }
        }
    }
}
