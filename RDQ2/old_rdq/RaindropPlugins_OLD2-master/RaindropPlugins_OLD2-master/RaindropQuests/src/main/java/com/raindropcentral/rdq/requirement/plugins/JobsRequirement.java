package com.raindropcentral.rdq.requirement.plugins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.api.eco.EcoJobsService;
import com.raindropcentral.rplatform.api.zrips.JobsRebornService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unified requirement implementation for multiple job plugins.
 * <p>
 * This requirement checks whether a player has reached a specified level in particular jobs,
 * or a total combined level across all jobs, as defined by various job plugins.
 * It supports both individual job requirements, multiple job requirements, and a "total" 
 * (aggregate) level requirement across different job plugin systems.
 * </p>
 * <p>
 * <b>Supported Job Plugins:</b>
 * <ul>
 *   <li><b>EcoJobs:</b> Modern job system with custom jobs</li>
 *   <li><b>JobsReborn:</b> Popular job system with various professions</li>
 *   <li><b>Future plugins:</b> Extensible architecture for additional job systems</li>
 * </ul>
 * </p>
 * <ul>
 *   <li>If {@code job} is {@code null} or equals "total" (case-insensitive), the requirement checks the player's total combined job level.</li>
 *   <li>For single job requirements, it checks the player's level in the specified job.</li>
 *   <li>For multiple job requirements, all specified jobs must meet their required levels.</li>
 *   <li>Supports configuration via RequirementSection with flexible field names.</li>
 * </ul>
 *
 * Used in the RaindropQuests system to gate progression, upgrades, or features behind
 * job achievements across different job plugin systems.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class JobsRequirement extends AbstractRequirement {

    private static final Logger LOGGER = Logger.getLogger(JobsRequirement.class.getName());

    /**
     * Enumeration of supported job plugins.
     */
    public enum JobPlugin {
        /**
         * EcoJobs plugin.
         */
        ECO_JOBS("EcoJobs", "ecojobs"),
        
        /**
         * JobsReborn plugin.
         */
        JOBS_REBORN("Jobs", "jobsreborn"),
        
        /**
         * Auto-detect the available job plugin.
         */
        AUTO("Auto", "auto");

        private final String pluginName;
        private final String identifier;

        JobPlugin(final String pluginName, final String identifier) {
            this.pluginName = pluginName;
            this.identifier = identifier;
        }

        /**
         * Gets the plugin name.
         *
         * @return The plugin name.
         */
        public String getPluginName() {
            return this.pluginName;
        }

        /**
         * Gets the identifier.
         *
         * @return The identifier.
         */
        public String getIdentifier() {
            return this.identifier;
        }

        /**
         * Gets a JobPlugin by identifier.
         *
         * @param identifier The identifier.
         * @return The JobPlugin, or AUTO if not found.
         */
        public static JobPlugin fromIdentifier(final String identifier) {
            if (identifier == null) {
                return AUTO;
            }
            
            for (final JobPlugin plugin : values()) {
                if (plugin.identifier.equalsIgnoreCase(identifier)) {
                    return plugin;
                }
            }
            return AUTO;
        }
    }

    /**
     * The job plugin to use for this requirement.
     */
    @JsonProperty("jobPlugin")
    private final JobPlugin jobPlugin;

    /**
     * The name of the job to check, or "total" for aggregate level.
     * Used for single job requirements.
     */
    @JsonProperty("job")
    private final String job;

    /**
     * The required level for the job or total combined level.
     * Used for single job requirements.
     */
    @JsonProperty("level")
    private final int level;

    /**
     * Map of multiple job requirements.
     * Maps job names to required levels for multiple job requirements.
     */
    @JsonProperty("jobs")
    private final Map<String, Integer> jobs;

    /**
     * Optional description for this jobs requirement.
     */
    @JsonProperty("description")
    private final String description;

    /**
     * Cached job service for the specified plugin.
     * This is resolved at runtime and not serialized.
     */
    @JsonIgnore
    private transient Object jobService;

    /**
     * Constructs a new JobsRequirement for a single job with auto-detection.
     *
     * @param job The name of the job to check, or {@code null}/"total" for total combined level.
     * @param level The required level for the job or total combined level.
     */
    public JobsRequirement(
            @Nullable final String job, 
            final int level
    ) {
        this(JobPlugin.AUTO, job, level, null, null);
    }

    /**
     * Constructs a new JobsRequirement for a single job with specific plugin.
     *
     * @param jobPlugin The job plugin to use.
     * @param job The name of the job to check, or {@code null}/"total" for total combined level.
     * @param level The required level for the job or total combined level.
     */
    public JobsRequirement(
            @NotNull final JobPlugin jobPlugin,
            @Nullable final String job, 
            final int level
    ) {
        this(jobPlugin, job, level, null, null);
    }

    /**
     * Constructs a new JobsRequirement for multiple jobs.
     *
     * @param jobPlugin The job plugin to use.
     * @param jobs Map of job names to required levels.
     */
    public JobsRequirement(
            @NotNull final JobPlugin jobPlugin,
            @NotNull final Map<String, Integer> jobs
    ) {
        this(jobPlugin, null, 0, jobs, null);
    }

    /**
     * Constructs a new JobsRequirement with full configuration options.
     *
     * @param jobPlugin The job plugin to use (can be null for auto-detection).
     * @param job Single job name (can be null if using jobs map).
     * @param level Single job level (ignored if using jobs map).
     * @param jobs Map of multiple jobs and levels (can be null if using single job).
     * @param description Optional description for this requirement.
     */
    @JsonCreator
    public JobsRequirement(
            @JsonProperty("jobPlugin") @Nullable final JobPlugin jobPlugin,
            @JsonProperty("job") @Nullable final String job,
            @JsonProperty("level") final int level,
            @JsonProperty("jobs") @Nullable final Map<String, Integer> jobs,
            @JsonProperty("description") @Nullable final String description
    ) {
        super(Type.JOBS);

        // Validate that we have either a single job or multiple jobs
        final boolean hasSingleJob = job != null && level > 0;
        final boolean hasMultipleJobs = jobs != null && !jobs.isEmpty();

        if (!hasSingleJob && !hasMultipleJobs) {
            throw new IllegalArgumentException("Either a single job with level or multiple jobs must be specified.");
        }
	    
	    if (hasMultipleJobs) {
            for (final Map.Entry<String, Integer> entry : jobs.entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                    throw new IllegalArgumentException("Job name cannot be null or empty.");
                }
                if (entry.getValue() == null || entry.getValue() <= 0) {
                    throw new IllegalArgumentException("Job level must be positive for job: " + entry.getKey());
                }
            }
        }

        this.jobPlugin = jobPlugin != null ? jobPlugin : JobPlugin.AUTO;
        this.job = job;
        this.level = level;
        this.jobs = jobs != null ? new HashMap<>(jobs) : new HashMap<>();
        this.description = description;
    }

    /**
     * Factory method to create JobsRequirement from string plugin identifier.
     *
     * @param pluginIdentifier The plugin identifier string.
     * @param job The job name.
     * @param level The required level.
     * @return A new JobsRequirement instance.
     */
    @JsonIgnore
    @NotNull
    public static JobsRequirement fromPluginString(
            @Nullable final String pluginIdentifier,
            @Nullable final String job,
            final int level
    ) {
        final JobPlugin plugin = JobPlugin.fromIdentifier(pluginIdentifier);
        return new JobsRequirement(plugin, job, level, null, null);
    }

    /**
     * Checks if the player meets the job or total combined level requirement.
     *
     * @param player The player to check.
     * @return {@code true} if the player meets or exceeds the required level(s), {@code false} otherwise.
     */
    @Override
    public boolean isMet(
            @NotNull final Player player
    ) {
        final Object service = this.getJobService();
        if (service == null) {
            LOGGER.log(Level.WARNING, "Job service not available for plugin: " + this.jobPlugin);
            return false;
        }

        // Check multiple jobs if specified
        if (!this.jobs.isEmpty()) {
            return this.jobs.entrySet().stream()
                    .allMatch(entry -> this.checkSingleJob(player, service, entry.getKey(), entry.getValue()));
        }

        // Check single job
        if (this.job != null) {
            return this.checkSingleJob(player, service, this.job, this.level);
        }

        return false;
    }

    /**
     * Calculates the progress toward fulfilling this jobs requirement for the specified player.
     * <p>
     * For single job requirements, progress is calculated as {@code currentLevel / requiredLevel}.
     * For multiple job requirements, progress is the average progress across all jobs.
     * Progress is clamped to 1.0 if the requirement is met.
     * </p>
     *
     * @param player The player whose progress is being calculated.
     * @return A double between 0.0 and 1.0 representing completion progress.
     */
    @Override
    public double calculateProgress(
            @NotNull final Player player
    ) {
        final Object service = this.getJobService();
        if (service == null) {
            LOGGER.log(Level.WARNING, "Job service not available for plugin: " + this.jobPlugin);
            return 0.0;
        }

        // Calculate progress for multiple jobs
        if (!this.jobs.isEmpty()) {
            double totalProgress = 0.0;
            for (final Map.Entry<String, Integer> entry : this.jobs.entrySet()) {
                final double jobProgress = this.calculateSingleJobProgress(player, service, entry.getKey(), entry.getValue());
                totalProgress += jobProgress;
            }
            return Math.min(1.0, totalProgress / this.jobs.size());
        }

        // Calculate progress for single job
        if (this.job != null) {
            return this.calculateSingleJobProgress(player, service, this.job, this.level);
        }

        return 0.0;
    }

    /**
     * Consumes resources from the player to fulfill this requirement.
     * <p>
     * Not applicable for jobs requirements; this method is a no-op.
     * </p>
     *
     * @param player The player from whom resources would be consumed.
     */
    @Override
    public void consume(
            @NotNull final Player player
    ) {
        // Job levels are not consumed
    }

    /**
     * Gets the translation key for the requirement's description.
     * <p>
     * Used for localization and display in the UI.
     * </p>
     *
     * @return The language key for this requirement's description.
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.jobs." + this.jobPlugin.getIdentifier();
    }

    /**
     * Gets the job plugin used by this requirement.
     *
     * @return The job plugin.
     */
    @NotNull
    public JobPlugin getJobPlugin() {
        return this.jobPlugin;
    }

    /**
     * Gets the required level for this requirement (single job mode).
     *
     * @return The required job or total combined level.
     */
    public int getLevel() {
        return this.level;
    }

    /**
     * Gets the job name for this requirement (single job mode).
     *
     * @return The job name, or "total" for combined level.
     */
    @Nullable
    public String getJob() {
        return this.job;
    }

    /**
     * Gets the map of required jobs and their levels (multiple jobs mode).
     *
     * @return Unmodifiable map of job names to required levels.
     */
    @NotNull
    public Map<String, Integer> getJobs() {
        return Collections.unmodifiableMap(this.jobs);
    }

    /**
     * Gets the optional description for this jobs requirement.
     *
     * @return The description, or null if not provided.
     */
    @Nullable
    public String getDescription() {
        return this.description;
    }

    /**
     * Gets detailed progress information for each job for the specified player.
     *
     * @param player The player whose progress will be calculated.
     * @return A map of job names to their current levels.
     */
    @JsonIgnore
    @NotNull
    public Map<String, Integer> getCurrentJobLevels(
            @NotNull final Player player
    ) {
        final Object service = this.getJobService();
        if (service == null) {
            return new HashMap<>();
        }

        final Map<String, Integer> currentLevels = new HashMap<>();

        // Get levels for multiple jobs
        if (!this.jobs.isEmpty()) {
            for (final String jobName : this.jobs.keySet()) {
                final int currentLevel = this.getCurrentJobLevel(player, service, jobName);
                currentLevels.put(jobName, currentLevel);
            }
        }

        // Get level for single job
        if (this.job != null) {
            final int currentLevel = this.getCurrentJobLevel(player, service, this.job);
            currentLevels.put(this.job, currentLevel);
        }

        return currentLevels;
    }

    /**
     * Checks if this requirement uses multiple jobs.
     *
     * @return True if multiple jobs are required, false for single job.
     */
    @JsonIgnore
    public boolean isMultipleJobs() {
        return !this.jobs.isEmpty();
    }

    /**
     * Checks if this requirement uses the total job level.
     *
     * @return True if checking total job level, false otherwise.
     */
    @JsonIgnore
    public boolean isTotalJobLevel() {
        return this.job != null && this.job.equalsIgnoreCase("total");
    }

    /**
     * Gets the detected job plugin being used.
     *
     * @return The detected job plugin, or null if none available.
     */
    @JsonIgnore
    @Nullable
    public JobPlugin getDetectedJobPlugin() {
        if (this.jobPlugin != JobPlugin.AUTO) {
            return this.jobPlugin;
        }

        // Auto-detect available plugin
        if (Bukkit.getPluginManager().getPlugin("EcoJobs") != null) {
            return JobPlugin.ECO_JOBS;
        }
        if (Bukkit.getPluginManager().getPlugin("Jobs") != null) {
            return JobPlugin.JOBS_REBORN;
        }

        return null;
    }

    /**
     * Validates the internal state of this jobs' requirement.
     *
     * @throws IllegalStateException If the requirement is in an invalid state.
     */
    @JsonIgnore
    public void validate() {
        final boolean hasSingleJob = this.job != null && this.level > 0;
        final boolean hasMultipleJobs = !this.jobs.isEmpty();

        if (!hasSingleJob && !hasMultipleJobs) {
            throw new IllegalStateException("Either a single job or multiple jobs must be specified.");
        }
	    
	    if (hasMultipleJobs) {
            for (final Map.Entry<String, Integer> entry : this.jobs.entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                    throw new IllegalStateException("Job name cannot be null or empty.");
                }
                if (entry.getValue() == null || entry.getValue() <= 0) {
                    throw new IllegalStateException("Job level must be positive for job: " + entry.getKey());
                }
            }
        }

        // Validate that the job plugin is available
        final JobPlugin detectedPlugin = this.getDetectedJobPlugin();
        if (detectedPlugin == null) {
            throw new IllegalStateException("No supported job plugin found. Available plugins: EcoJobs, JobsReborn");
        }
    }

    /**
     * Gets the job service for the configured plugin, resolving it at runtime if necessary.
     *
     * @return The job service, or null if not available.
     */
    @Nullable
    private Object getJobService() {
        if (this.jobService == null) {
            final JobPlugin targetPlugin = this.jobPlugin == JobPlugin.AUTO ? this.getDetectedJobPlugin() : this.jobPlugin;
            
            if (targetPlugin == null) {
                return null;
            }

            switch (targetPlugin) {
                case ECO_JOBS -> {
                    final Plugin ecoJobsPlugin = Bukkit.getPluginManager().getPlugin("EcoJobs");
                    if (ecoJobsPlugin != null && ecoJobsPlugin.isEnabled()) {
                        this.jobService = new EcoJobsService();
                    }
                }
                case JOBS_REBORN -> {
                    final Plugin jobsPlugin = Bukkit.getPluginManager().getPlugin("Jobs");
                    if (jobsPlugin != null && jobsPlugin.isEnabled()) {
                        this.jobService = new JobsRebornService();
                    }
                }
            }
        }
        return this.jobService;
    }

    /**
     * Checks if a player meets a single job requirement.
     *
     * @param player The player to check.
     * @param service The job service.
     * @param jobName The job name.
     * @param requiredLevel The required level.
     * @return True if the requirement is met.
     */
    private boolean checkSingleJob(
            @NotNull final Player player,
            @NotNull final Object service,
            @NotNull final String jobName,
            final int requiredLevel
    ) {
        if (jobName.equalsIgnoreCase("total")) {
            if (service instanceof final EcoJobsService ecoService) {
                return requiredLevel <= ecoService.getTotalJobLevel(player);
            } else if (service instanceof final JobsRebornService jobsService) {
                return requiredLevel <= jobsService.getTotalJobLevel(player);
            }
        } else {
            if (service instanceof final EcoJobsService ecoService) {
                return requiredLevel <= ecoService.getJobLevel(player, jobName);
            } else if (service instanceof final JobsRebornService jobsService) {
                return requiredLevel <= jobsService.getJobLevel(player, jobName);
            }
        }
        return false;
    }

    /**
     * Calculates progress for a single job.
     *
     * @param player The player.
     * @param service The job service.
     * @param jobName The job name.
     * @param requiredLevel The required level.
     * @return Progress value between 0.0 and 1.0.
     */
    private double calculateSingleJobProgress(
            @NotNull final Player player,
            @NotNull final Object service,
            @NotNull final String jobName,
            final int requiredLevel
    ) {
        if (requiredLevel <= 0) {
            return 1.0;
        }

        final int currentLevel = this.getCurrentJobLevel(player, service, jobName);
        return Math.min(1.0, (double) currentLevel / requiredLevel);
    }

    /**
     * Gets the current level for a specific job.
     *
     * @param player The player.
     * @param service The job service.
     * @param jobName The job name.
     * @return The current level.
     */
    private int getCurrentJobLevel(
            @NotNull final Player player,
            @NotNull final Object service,
            @NotNull final String jobName
    ) {
        if (jobName.equalsIgnoreCase("total")) {
            if (service instanceof final EcoJobsService ecoService) {
                return ecoService.getTotalJobLevel(player);
            } else if (service instanceof final JobsRebornService jobsService) {
                return jobsService.getTotalJobLevel(player);
            }
        } else {
            if (service instanceof final EcoJobsService ecoService) {
                return ecoService.getJobLevel(player, jobName);
            } else if (service instanceof final JobsRebornService jobsService) {
                return jobsService.getJobLevel(player, jobName);
            }
        }
        return 0;
    }
}