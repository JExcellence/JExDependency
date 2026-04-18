package de.jexcellence.jexplatform.integration.job;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * EcoJobs reflection-based job bridge.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class EcoJobsJobBridge extends AbstractReflectionJobBridge
        implements JobBridge {

    /**
     * Creates the EcoJobs bridge.
     *
     * @param logger the platform logger
     */
    EcoJobsJobBridge(@NotNull JExLogger logger) {
        super(logger);
    }

    @Override
    public @NotNull String pluginName() {
        return "EcoJobs";
    }

    @Override
    public boolean isAvailable() {
        return findClass("com.willfp.ecojobs.api.EcoJobsAPI") != null;
    }

    @Override
    public @NotNull CompletableFuture<Integer> getJobLevel(@NotNull Player player,
                                                           @NotNull String jobId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var apiClass = Class.forName("com.willfp.ecojobs.api.EcoJobsAPI");
                var result = apiClass.getMethod("getJobLevel", Player.class, String.class)
                        .invoke(null, player, jobId);
                return result instanceof Number n ? n.intValue() : 0;
            } catch (Exception e) {
                logger.debug("EcoJobs getJobLevel failed: {}", e.getMessage());
                return 0;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<List<JobDescriptor>> getAvailableJobs() {
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public @NotNull CompletableFuture<Boolean> addJobLevels(@NotNull Player player,
                                                            @NotNull String jobId,
                                                            int levels) {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> consumeJobLevel(@NotNull Player player,
                                                               @NotNull String jobId,
                                                               int levels) {
        return CompletableFuture.completedFuture(false);
    }
}
