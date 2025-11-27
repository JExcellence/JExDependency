package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.logger.CentralLogger;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Requirement that validates a player's accumulated playtime.
 * <p>
 * The {@code PlaytimeRequirement} checks if a player has played for a minimum amount of time,
 * either globally across all worlds or specifically in certain worlds. It supports various
 * time units and can track playtime per world for more granular requirements.
 * </p>
 * <p>
 * This requirement uses Bukkit's {@link Statistic#PLAY_ONE_MINUTE} to track playtime,
 * which provides accurate playtime tracking in ticks (20 ticks = 1 second).
 * </p>
 *
 * <ul>
 *   <li>Supports total playtime validation across all worlds</li>
 *   <li>Supports world-specific playtime requirements</li>
 *   <li>Flexible time unit configuration (seconds, minutes, hours, days)</li>
 *   <li>Progress calculation based on current vs required playtime</li>
 *   <li>No resource consumption (playtime cannot be "consumed")</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class PlaytimeRequirement extends AbstractRequirement {
    
    private static final Logger LOGGER = CentralLogger.getLogger(PlaytimeRequirement.class.getName());
    
    /**
     * The required playtime in seconds for total playtime validation.
     */
    @JsonProperty("requiredPlaytimeSeconds")
    private final long requiredPlaytimeSeconds;
    
    /**
     * Map of world names to required playtime in seconds for world-specific validation.
     * If null or empty, total playtime is used instead.
     */
    @JsonProperty("worldPlaytimeRequirements")
    private final Map<String, Long> worldPlaytimeRequirements;
    
    /**
     * Whether to use total playtime (true) or world-specific playtime (false).
     */
    @JsonProperty("useTotalPlaytime")
    private final boolean useTotalPlaytime;
    
    /**
     * Optional description for this playtime requirement.
     */
    @JsonProperty("description")
    private final String description;
    
    /**
     * Constructs a {@code PlaytimeRequirement} for total playtime validation.
     *
     * @param requiredPlaytimeSeconds The required playtime in seconds.
     */
    public PlaytimeRequirement(
        final long requiredPlaytimeSeconds
    ) {
        
        this(
            requiredPlaytimeSeconds,
            null,
            true,
            null
        );
    }
    
    /**
     * Constructs a {@code PlaytimeRequirement} with full configuration options.
     *
     * @param requiredPlaytimeSeconds   The required total playtime in seconds.
     * @param worldPlaytimeRequirements Map of world names to required playtime in seconds.
     * @param useTotalPlaytime          Whether to use total playtime or world-specific playtime.
     * @param description               Optional description for this requirement.
     */
    @JsonCreator
    public PlaytimeRequirement(
        @JsonProperty("requiredPlaytimeSeconds") final long requiredPlaytimeSeconds,
        @JsonProperty("worldPlaytimeRequirements") @Nullable final Map<String, Long> worldPlaytimeRequirements,
        @JsonProperty("useTotalPlaytime") @Nullable final Boolean useTotalPlaytime,
        @JsonProperty("description") @Nullable final String description
    ) {
        
        super(Type.PLAYTIME);
        
        if (
            requiredPlaytimeSeconds < 0
        ) {
            throw new IllegalArgumentException("Required playtime cannot be negative: " + requiredPlaytimeSeconds);
        }
        
        this.requiredPlaytimeSeconds = requiredPlaytimeSeconds;
        this.worldPlaytimeRequirements = worldPlaytimeRequirements != null ?
                                         new HashMap<>(worldPlaytimeRequirements) :
                                         new HashMap<>();
        this.useTotalPlaytime = useTotalPlaytime != null ?
                                useTotalPlaytime :
                                true;
        this.description = description;
        
        if (
            ! this.useTotalPlaytime && this.worldPlaytimeRequirements.isEmpty()
        ) {
            throw new IllegalArgumentException("World playtime requirements cannot be empty when not using total playtime");
        }
    }
    
    /**
     * Factory method to create a PlaytimeRequirement from time configuration values.
     * Useful for configuration-based creation.
     *
     * @param requiredSeconds   Required playtime in seconds.
     * @param requiredMinutes   Required playtime in minutes.
     * @param requiredHours     Required playtime in hours.
     * @param requiredDays      Required playtime in days.
     * @param worldRequirements Map of world names to playtime requirements.
     * @param useTotalPlaytime  Whether to use total playtime.
     * @param description       Optional description.
     *
     * @return A new PlaytimeRequirement instance.
     *
     * @throws IllegalArgumentException If no time requirement is specified or multiple are specified.
     */
    @JsonIgnore
    @NotNull
    public static PlaytimeRequirement fromTimeConfig(
        @Nullable final Long requiredSeconds,
        @Nullable final Long requiredMinutes,
        @Nullable final Long requiredHours,
        @Nullable final Long requiredDays,
        @Nullable final Map<String, Long> worldRequirements,
        @Nullable final Boolean useTotalPlaytime,
        @Nullable final String description
    ) {
        
        int  timeValuesCount         = 0;
        long requiredPlaytimeSeconds = 0;
        
        if (
            requiredSeconds != null && requiredSeconds > 0
        ) {
            timeValuesCount++;
            requiredPlaytimeSeconds = requiredSeconds;
        }
        
        if (
            requiredMinutes != null && requiredMinutes > 0
        ) {
            timeValuesCount++;
            requiredPlaytimeSeconds = TimeUnit.MINUTES.toSeconds(requiredMinutes);
        }
        
        if (
            requiredHours != null && requiredHours > 0
        ) {
            timeValuesCount++;
            requiredPlaytimeSeconds = TimeUnit.HOURS.toSeconds(requiredHours);
        }
        
        if (
            requiredDays != null && requiredDays > 0
        ) {
            timeValuesCount++;
            requiredPlaytimeSeconds = TimeUnit.DAYS.toSeconds(requiredDays);
        }
        
        if (
            timeValuesCount == 0 &&
            (worldRequirements == null || worldRequirements.isEmpty())
        ) {
            throw new IllegalArgumentException("At least one playtime requirement must be specified.");
        }
        
        if (
            timeValuesCount > 1
        ) {
            throw new IllegalArgumentException("Only one global playtime requirement can be specified at a time.");
        }
        
        return new PlaytimeRequirement(
            requiredPlaytimeSeconds,
            worldRequirements,
            useTotalPlaytime,
            description
        );
    }
    
    /**
     * Factory method to create a PlaytimeRequirement for specific worlds.
     *
     * @param worldPlaytimeMap Map of world names to required playtime in seconds.
     * @param description      Optional description.
     *
     * @return A new PlaytimeRequirement instance for world-specific validation.
     */
    @JsonIgnore
    @NotNull
    public static PlaytimeRequirement forWorlds(
        
        final @NotNull Map<String, Long> worldPlaytimeMap,
        @Nullable final String description
    ) {
        
        return new PlaytimeRequirement(
            0,
            worldPlaytimeMap,
            false,
            description
        );
    }
    
    /**
     * Checks if the player meets the playtime requirement.
     *
     * @param player The player to check.
     *
     * @return {@code true} if the player has sufficient playtime, {@code false} otherwise.
     */
    @Override
    public boolean isMet(
        final @NotNull Player player
    ) {
        
        if (
            this.useTotalPlaytime
        ) {
            return this.getTotalPlaytimeSeconds(player) >= this.requiredPlaytimeSeconds;
        } else {
            return this.checkWorldPlaytimeRequirements(player);
        }
    }
    
    /**
     * Calculates the progress toward fulfilling this playtime requirement.
     *
     * @param player The player whose progress is being calculated.
     *
     * @return A double representing the completion progress (0.0 to 1.0).
     */
    @Override
    public double calculateProgress(
        final @NotNull Player player
    ) {
        
        if (
            this.useTotalPlaytime
        ) {
            if (
                this.requiredPlaytimeSeconds <= 0
            ) {
                return 1.0;
            }
            
            final long currentPlaytime = this.getTotalPlaytimeSeconds(player);
            return Math.min(
                1.0,
                (double) currentPlaytime / this.requiredPlaytimeSeconds
            );
        } else {
            return this.calculateWorldPlaytimeProgress(player);
        }
    }
    
    /**
     * Playtime requirements do not consume resources as playtime cannot be "spent".
     * This method performs no action.
     *
     * @param player The player (unused).
     */
    @Override
    public void consume(
        final @NotNull Player player
    ) {
    
    }
    
    /**
     * Returns the translation key for this requirement's description.
     *
     * @return The language key for this requirement's description.
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        
        return "requirement.playtime";
    }
    
    /**
     * Gets the player's total playtime across all worlds in seconds.
     *
     * @param player The player to check.
     *
     * @return Total playtime in seconds.
     */
    @JsonIgnore
    public long getTotalPlaytimeSeconds(
        final @NotNull Player player
    ) {
        
        final int playtimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        return playtimeTicks / 20;
    }
    
    /**
     * Gets the player's playtime in a specific world in seconds.
     *
     * @param player    The player to check.
     * @param worldName The name of the world.
     *
     * @return Playtime in the specified world in seconds, or 0 if the world doesn't exist.
     */
    @JsonIgnore
    public long getWorldPlaytimeSeconds(
        final @NotNull Player player,
        final @NotNull String worldName
    ) {
        
        final World world = Bukkit.getWorld(worldName);
        if (
            world == null
        ) {
            return 0;
        }
        
        try {
            final int playtimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
            return playtimeTicks / 20;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Checks if the player meets all world-specific playtime requirements.
     *
     * @param player The player to check.
     *
     * @return {@code true} if all world requirements are met, {@code false} otherwise.
     */
    private boolean checkWorldPlaytimeRequirements(
        final @NotNull Player player
    ) {
        
        for (Map.Entry<String, Long> entry : worldPlaytimeRequirements.entrySet()) {
            final String worldName       = entry.getKey();
            final long   requiredSeconds = entry.getValue();
            final long   actualSeconds   = getWorldPlaytimeSeconds(
                player,
                worldName
            );
            
            if (actualSeconds < requiredSeconds) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Calculates progress for world-specific playtime requirements.
     *
     * @param player The player to check.
     *
     * @return Average progress across all world requirements (0.0 to 1.0).
     */
    private double calculateWorldPlaytimeProgress(
        final @NotNull Player player
    ) {
        
        if (
            this.worldPlaytimeRequirements.isEmpty()
        ) {
            return 1.0;
        }
        
        double totalProgress     = 0.0;
        int    validRequirements = 0;
        
        for (
            Map.Entry<String, Long> entry : this.worldPlaytimeRequirements.entrySet()
        ) {
            final String worldName       = entry.getKey();
            final long   requiredSeconds = entry.getValue();
            
            if (
                requiredSeconds <= 0
            ) {
                totalProgress += 1.0;
            } else {
                final long actualSeconds = getWorldPlaytimeSeconds(
                    player,
                    worldName
                );
                totalProgress += Math.min(
                    1.0,
                    (double) actualSeconds / requiredSeconds
                );
            }
            validRequirements++;
        }
        
        return validRequirements > 0 ?
               totalProgress / validRequirements :
               1.0;
    }
    
    /**
     * Gets the required playtime in seconds.
     *
     * @return Required playtime in seconds.
     */
    public long getRequiredPlaytimeSeconds() {
        
        return this.requiredPlaytimeSeconds;
    }
    
    /**
     * Gets the required playtime in minutes.
     *
     * @return Required playtime in minutes.
     */
    @JsonIgnore
    public long getRequiredPlaytimeMinutes() {
        
        return TimeUnit.SECONDS.toMinutes(this.requiredPlaytimeSeconds);
    }
    
    /**
     * Gets the required playtime in hours.
     *
     * @return Required playtime in hours.
     */
    @JsonIgnore
    public long getRequiredPlaytimeHours() {
        
        return TimeUnit.SECONDS.toHours(this.requiredPlaytimeSeconds);
    }
    
    /**
     * Gets the required playtime in days.
     *
     * @return Required playtime in days.
     */
    @JsonIgnore
    public long getRequiredPlaytimeDays() {
        
        return TimeUnit.SECONDS.toDays(this.requiredPlaytimeSeconds);
    }
    
    /**
     * Gets the world-specific playtime requirements.
     *
     * @return Map of world names to required playtime in seconds.
     */
    @NotNull
    public Map<String, Long> getWorldPlaytimeRequirements() {
        
        return new HashMap<>(this.worldPlaytimeRequirements);
    }
    
    /**
     * Checks if this requirement uses total playtime.
     *
     * @return {@code true} if using total playtime, {@code false} if using world-specific playtime.
     */
    public boolean isUseTotalPlaytime() {
        
        return this.useTotalPlaytime;
    }
    
    /**
     * Gets the optional description for this playtime requirement.
     *
     * @return The description, or null if not provided.
     */
    @Nullable
    public String getDescription() {
        
        return this.description;
    }
    
    /**
     * Gets a formatted string representation of the required playtime.
     *
     * @return Human-readable playtime requirement.
     */
    @JsonIgnore
    @NotNull
    public String getFormattedRequiredPlaytime() {
        
        if (
            ! this.useTotalPlaytime
        ) {
            return this.formatWorldRequirements();
        }
        
        return formatDuration(this.requiredPlaytimeSeconds);
    }
    
    /**
     * Gets a formatted string representation of the player's current playtime.
     *
     * @param player The player to check.
     *
     * @return Human-readable current playtime.
     */
    @JsonIgnore
    @NotNull
    public String getFormattedCurrentPlaytime(
        final @NotNull Player player
    ) {
        
        if (
            ! this.useTotalPlaytime
        ) {
            return formatCurrentWorldPlaytime(player);
        }
        
        final long currentSeconds = getTotalPlaytimeSeconds(player);
        return formatDuration(currentSeconds);
    }
    
    /**
     * Formats a duration in seconds to a human-readable string.
     *
     * @param seconds Duration in seconds.
     *
     * @return Formatted duration (e.g., "2h 30m", "1d 5h", "45m 30s").
     */
    @JsonIgnore
    @NotNull
    public static String formatDuration(
        final long seconds
    ) {
        
        if (
            seconds <= 0
        ) {
            return "0s";
        }
        
        final long days             = TimeUnit.SECONDS.toDays(seconds);
        final long hours            = TimeUnit.SECONDS.toHours(seconds) % 24;
        final long minutes          = TimeUnit.SECONDS.toMinutes(seconds) % 60;
        final long remainingSeconds = seconds % 60;
        
        final StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (remainingSeconds > 0 && days == 0) {
            sb.append(remainingSeconds).append("s");
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Formats world-specific requirements to a human-readable string.
     *
     * @return Formatted world requirements.
     */
    private String formatWorldRequirements() {
        
        if (
            this.worldPlaytimeRequirements.isEmpty()
        ) {
            return "No world requirements";
        }
        
        final StringBuilder sb = new StringBuilder();
        for (
            Map.Entry<String, Long> entry : this.worldPlaytimeRequirements.entrySet()
        ) {
            if (! sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append(": ").append(formatDuration(entry.getValue()));
        }
        return sb.toString();
    }
    
    /**
     * Formats current world-specific playtime to a human-readable string.
     *
     * @param player The player to check.
     *
     * @return Formatted current world playtime.
     */
    private String formatCurrentWorldPlaytime(
        final @NotNull Player player
    ) {
        
        if (
            this.worldPlaytimeRequirements.isEmpty()
        ) {
            return "No world requirements";
        }
        
        final StringBuilder sb = new StringBuilder();
        for (String worldName : this.worldPlaytimeRequirements.keySet()) {
            if (! sb.isEmpty()) {
                sb.append(", ");
            }
            final long currentSeconds = getWorldPlaytimeSeconds(
                player,
                worldName
            );
            sb.append(worldName).append(": ").append(formatDuration(currentSeconds));
        }
        return sb.toString();
    }
    
    /**
     * Validates the internal state of this playtime requirement.
     *
     * @throws IllegalStateException If the requirement is in an invalid state.
     */
    @JsonIgnore
    public void validate() {
        
        if (
            this.requiredPlaytimeSeconds < 0
        ) {
            throw new IllegalStateException("Required playtime cannot be negative: " + this.requiredPlaytimeSeconds);
        }
        
        if (
            ! this.useTotalPlaytime &&
            this.worldPlaytimeRequirements.isEmpty()
        ) {
            throw new IllegalStateException("World playtime requirements cannot be empty when not using total playtime");
        }
        
        for (
            Map.Entry<String, Long> entry : this.worldPlaytimeRequirements.entrySet()
        ) {
            if (
                entry.getKey() == null ||
                entry.getKey().trim().isEmpty()
            ) {
                throw new IllegalStateException("World name cannot be null or empty");
            }
            if (
                entry.getValue() < 0
            ) {
                throw new IllegalStateException("World playtime requirement cannot be negative: " + entry.getValue());
            }
        }
    }
    
}