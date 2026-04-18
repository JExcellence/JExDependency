package de.jexcellence.jexplatform.integration.job;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * JobsReborn reflection-based job bridge.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class JobsRebornJobBridge extends AbstractReflectionJobBridge
        implements JobBridge {

    /**
     * Creates the JobsReborn bridge.
     *
     * @param logger the platform logger
     */
    JobsRebornJobBridge(@NotNull JExLogger logger) {
        super(logger);
    }

    @Override
    public @NotNull String pluginName() {
        return "Jobs";
    }

    @Override
    public boolean isAvailable() {
        return findClass("com.gamingmesh.jobs.Jobs") != null;
    }

    @Override
    public @NotNull CompletableFuture<Integer> getJobLevel(@NotNull Player player,
                                                            @NotNull String jobId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var jobsClass = Class.forName("com.gamingmesh.jobs.Jobs");
                var playerManager = jobsClass.getMethod("getPlayerManager").invoke(null);
                var jobPlayer = playerManager.getClass()
                        .getMethod("getJobsPlayer", Player.class)
                        .invoke(playerManager, player);
                if (jobPlayer == null) return 0;

                var progressions = jobPlayer.getClass().getMethod("getJobProgression")
                        .invoke(jobPlayer);
                if (progressions instanceof List<?> list) {
                    for (var prog : list) {
                        var job = prog.getClass().getMethod("getJob").invoke(prog);
                        var name = job.getClass().getMethod("getName").invoke(job);
                        if (jobId.equalsIgnoreCase(name.toString())) {
                            var level = prog.getClass().getMethod("getLevel").invoke(prog);
                            return level instanceof Number n ? n.intValue() : 0;
                        }
                    }
                }
                return 0;
            } catch (Exception e) {
                logger.debug("JobsReborn getJobLevel failed: {}", e.getMessage());
                return 0;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<List<JobDescriptor>> getAvailableJobs() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var jobsClass = Class.forName("com.gamingmesh.jobs.Jobs");
                var jobs = jobsClass.getMethod("getJobs").invoke(null);
                if (jobs instanceof List<?> list) {
                    return list.stream()
                            .map(job -> {
                                try {
                                    var name = job.getClass().getMethod("getName").invoke(job);
                                    return new JobDescriptor(name.toString(), name.toString());
                                } catch (Exception e) {
                                    return null;
                                }
                            })
                            .filter(java.util.Objects::nonNull)
                            .toList();
                }
                return List.<JobDescriptor>of();
            } catch (Exception e) {
                logger.debug("JobsReborn getAvailableJobs failed: {}", e.getMessage());
                return List.of();
            }
        });
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
