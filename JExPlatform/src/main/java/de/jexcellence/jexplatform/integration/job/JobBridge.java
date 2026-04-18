package de.jexcellence.jexplatform.integration.job;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Sealed bridge for job plugin integration.
 *
 * <p>Supports EcoJobs and JobsReborn through reflection.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public sealed interface JobBridge permits EcoJobsJobBridge, JobsRebornJobBridge {

    /**
     * Descriptor for a job type.
     *
     * @param id          the job identifier
     * @param displayName the display name
     */
    record JobDescriptor(@NotNull String id, @NotNull String displayName) { }

    /**
     * Returns the name of the backing job plugin.
     *
     * @return the plugin name
     */
    @NotNull String pluginName();

    /**
     * Returns whether the backing plugin is available and loaded.
     *
     * @return {@code true} if available
     */
    boolean isAvailable();

    /**
     * Returns the job level of a player for a specific job.
     *
     * @param player the player
     * @param jobId  the job identifier
     * @return a future resolving to the job level
     */
    @NotNull CompletableFuture<Integer> getJobLevel(@NotNull Player player,
                                                    @NotNull String jobId);

    /**
     * Returns all available jobs.
     *
     * @return a future resolving to the list of job descriptors
     */
    @NotNull CompletableFuture<List<JobDescriptor>> getAvailableJobs();

    /**
     * Adds levels to a player's job.
     *
     * @param player the player
     * @param jobId  the job identifier
     * @param levels the number of levels to add
     * @return a future resolving to {@code true} on success
     */
    @NotNull CompletableFuture<Boolean> addJobLevels(@NotNull Player player,
                                                     @NotNull String jobId, int levels);

    /**
     * Consumes levels from a player's job.
     *
     * @param player the player
     * @param jobId  the job identifier
     * @param levels the number of levels to consume
     * @return a future resolving to {@code true} on success
     */
    @NotNull CompletableFuture<Boolean> consumeJobLevel(@NotNull Player player,
                                                        @NotNull String jobId, int levels);

    /**
     * Detects the best available job plugin.
     *
     * @param plugin the owning plugin
     * @param logger the platform logger
     * @return the detected bridge, or empty if none available
     */
    static @NotNull Optional<JobBridge> detect(@NotNull JavaPlugin plugin,
                                               @NotNull JExLogger logger) {
        var pm = plugin.getServer().getPluginManager();

        if (pm.isPluginEnabled("EcoJobs")) {
            logger.info("Job bridge: EcoJobs detected");
            return Optional.of(new EcoJobsJobBridge(logger));
        }
        if (pm.isPluginEnabled("Jobs")) {
            logger.info("Job bridge: JobsReborn detected");
            return Optional.of(new JobsRebornJobBridge(logger));
        }

        logger.warn("Job bridge: no provider detected");
        return Optional.empty();
    }
}
