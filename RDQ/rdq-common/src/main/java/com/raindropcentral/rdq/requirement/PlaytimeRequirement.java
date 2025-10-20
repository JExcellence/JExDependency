package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Requirement that validates a player's accumulated playtime.
 * <p>
 * The {@code PlaytimeRequirement} checks if a player has played for a minimum amount of time,
 * either globally across all worlds or specifically in certain worlds.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
public final class PlaytimeRequirement extends AbstractRequirement {

    private static final Logger LOGGER = CentralLogger.getLogger(PlaytimeRequirement.class.getName());

    @JsonProperty("requiredPlaytimeSeconds")
    private final long requiredPlaytimeSeconds;

    @JsonProperty("worldPlaytimeRequirements")
    private final Map<String, Long> worldPlaytimeRequirements;

    @JsonProperty("useTotalPlaytime")
    private final boolean useTotalPlaytime;

    @JsonProperty("description")
    private final String description;

    @JsonIgnore
    private transient final Map<String, World> worldCache = new ConcurrentHashMap<>();

    /**
     * Creates a requirement that validates against total playtime using the supplied threshold.
     *
     * @param requiredPlaytimeSeconds the total playtime required, in seconds
     */
    public PlaytimeRequirement(final long requiredPlaytimeSeconds) {
        this(requiredPlaytimeSeconds, null, true, null);
    }

    /**
     * Creates a requirement that can evaluate either global playtime or world-specific thresholds.
     *
     * @param requiredPlaytimeSeconds   the global playtime required in seconds
     * @param worldPlaytimeRequirements the per-world playtime requirements
     * @param useTotalPlaytime          whether global playtime should be evaluated
     * @param description               an optional human-readable description of the requirement
     * @throws IllegalArgumentException if negative thresholds are supplied or configuration is invalid
     */
    @JsonCreator
    public PlaytimeRequirement(
            @JsonProperty("requiredPlaytimeSeconds") final long requiredPlaytimeSeconds,
            @JsonProperty("worldPlaytimeRequirements") final @Nullable Map<String, Long> worldPlaytimeRequirements,
            @JsonProperty("useTotalPlaytime") final @Nullable Boolean useTotalPlaytime,
            @JsonProperty("description") final @Nullable String description
    ) {
        super(Type.PLAYTIME);

        if (requiredPlaytimeSeconds < 0) {
            throw new IllegalArgumentException("Required playtime cannot be negative: " + requiredPlaytimeSeconds);
        }

        this.requiredPlaytimeSeconds = requiredPlaytimeSeconds;
        this.worldPlaytimeRequirements = worldPlaytimeRequirements != null
                ? new HashMap<>(worldPlaytimeRequirements)
                : new HashMap<>();
        this.useTotalPlaytime = useTotalPlaytime != null ? useTotalPlaytime : true;
        this.description = description;

        if (!this.useTotalPlaytime && this.worldPlaytimeRequirements.isEmpty()) {
            throw new IllegalArgumentException(
                    "World playtime requirements cannot be empty when not using total playtime"
            );
        }
    }

    /**
     * Builds a requirement from a configuration that allows expressing the global threshold with
     * one of multiple time units.
     *
     * @param requiredSeconds    the required playtime in seconds, or {@code null}
     * @param requiredMinutes    the required playtime in minutes, or {@code null}
     * @param requiredHours      the required playtime in hours, or {@code null}
     * @param requiredDays       the required playtime in days, or {@code null}
     * @param worldRequirements  the per-world playtime requirements, or {@code null}
     * @param useTotalPlaytime   whether global playtime should be evaluated, or {@code null}
     * @param description        an optional human-readable description, or {@code null}
     * @return a configured {@link PlaytimeRequirement}
     * @throws IllegalArgumentException if the configuration is invalid or ambiguous
     */
    @JsonIgnore
    @NotNull
    public static PlaytimeRequirement fromTimeConfig(
            final @Nullable Long requiredSeconds,
            final @Nullable Long requiredMinutes,
            final @Nullable Long requiredHours,
            final @Nullable Long requiredDays,
            final @Nullable Map<String, Long> worldRequirements,
            final @Nullable Boolean useTotalPlaytime,
            final @Nullable String description
    ) {
        int timeValuesCount = 0;
        long requiredPlaytimeSeconds = 0;

        if (requiredSeconds != null && requiredSeconds > 0) {
            timeValuesCount++;
            requiredPlaytimeSeconds = requiredSeconds;
        }
        if (requiredMinutes != null && requiredMinutes > 0) {
            timeValuesCount++;
            requiredPlaytimeSeconds = TimeUnit.MINUTES.toSeconds(requiredMinutes);
        }
        if (requiredHours != null && requiredHours > 0) {
            timeValuesCount++;
            requiredPlaytimeSeconds = TimeUnit.HOURS.toSeconds(requiredHours);
        }
        if (requiredDays != null && requiredDays > 0) {
            timeValuesCount++;
            requiredPlaytimeSeconds = TimeUnit.DAYS.toSeconds(requiredDays);
        }

        if (timeValuesCount == 0 && (worldRequirements == null || worldRequirements.isEmpty())) {
            throw new IllegalArgumentException("At least one playtime requirement must be specified.");
        }
        if (timeValuesCount > 1) {
            throw new IllegalArgumentException("Only one global playtime requirement can be specified at a time.");
        }

        return new PlaytimeRequirement(requiredPlaytimeSeconds, worldRequirements, useTotalPlaytime, description);
    }

    /**
     * Creates a requirement that evaluates playtime against the supplied world-specific thresholds.
     *
     * @param worldPlaytimeMap the per-world playtime requirements to enforce
     * @param description      an optional human-readable description, or {@code null}
     * @return a configured {@link PlaytimeRequirement}
     */
    @JsonIgnore
    @NotNull
    public static PlaytimeRequirement forWorlds(
            final @NotNull Map<String, Long> worldPlaytimeMap,
            final @Nullable String description
    ) {
        return new PlaytimeRequirement(0, worldPlaytimeMap, false, description);
    }

    /**
     * Determines whether the player has satisfied the required playtime thresholds.
     *
     * @param player the player being validated
     * @return {@code true} when the requirement is met; otherwise {@code false}
     */
    @Override
    public boolean isMet(final @NotNull Player player) {
        if (this.useTotalPlaytime) {
            return this.getTotalPlaytimeSeconds(player) >= this.requiredPlaytimeSeconds;
        } else {
            return this.checkWorldPlaytimeRequirements(player);
        }
    }

    /**
     * Calculates the player's progress toward satisfying the requirement.
     *
     * @param player the player being evaluated
     * @return the completion ratio ranging from {@code 0.0} to {@code 1.0}
     */
    @Override
    public double calculateProgress(final @NotNull Player player) {
        if (this.useTotalPlaytime) {
            if (this.requiredPlaytimeSeconds <= 0) {
                return 1.0;
            }
            final long currentPlaytime = this.getTotalPlaytimeSeconds(player);
            return Math.min(1.0, (double) currentPlaytime / this.requiredPlaytimeSeconds);
        } else {
            return this.calculateWorldPlaytimeProgress(player);
        }
    }

    /**
     * Consuming a playtime requirement has no side effects because playtime is a historical metric.
     *
     * @param player the player who satisfied the requirement
     */
    @Override
    public void consume(final @NotNull Player player) {
    }

    /**
     * Provides the translation key that describes this requirement.
     *
     * @return the translation key used for localized descriptions
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.playtime";
    }

    /**
     * Retrieves the player's total playtime in seconds using the Bukkit statistics API.
     *
     * @param player the player whose playtime is being queried
     * @return the total playtime in seconds
     */
    @JsonIgnore
    public long getTotalPlaytimeSeconds(final @NotNull Player player) {
        final int playtimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        return playtimeTicks / 20;
    }

    /**
     * Retrieves the player's playtime in the specified world.
     *
     * @param player   the player whose playtime is being queried
     * @param worldName the world identifier to inspect
     * @return the accumulated playtime in the world, or {@code 0} when unavailable
     */
    @JsonIgnore
    public long getWorldPlaytimeSeconds(final @NotNull Player player, final @NotNull String worldName) {
        final World world = this.getCachedWorld(worldName);
        if (world == null) {
            return 0;
        }

        try {
            final int playtimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
            return playtimeTicks / 20;
        } catch (final Exception e) {
            return 0;
        }
    }

    /**
     * Obtains the required global playtime in seconds.
     *
     * @return the required playtime threshold expressed in seconds
     */
    public long getRequiredPlaytimeSeconds() {
        return this.requiredPlaytimeSeconds;
    }

    /**
     * Converts the required global playtime into minutes.
     *
     * @return the required playtime threshold expressed in minutes
     */
    @JsonIgnore
    public long getRequiredPlaytimeMinutes() {
        return TimeUnit.SECONDS.toMinutes(this.requiredPlaytimeSeconds);
    }

    /**
     * Converts the required global playtime into hours.
     *
     * @return the required playtime threshold expressed in hours
     */
    @JsonIgnore
    public long getRequiredPlaytimeHours() {
        return TimeUnit.SECONDS.toHours(this.requiredPlaytimeSeconds);
    }

    /**
     * Converts the required global playtime into days.
     *
     * @return the required playtime threshold expressed in days
     */
    @JsonIgnore
    public long getRequiredPlaytimeDays() {
        return TimeUnit.SECONDS.toDays(this.requiredPlaytimeSeconds);
    }

    /**
     * Provides a defensive copy of the configured world playtime requirements.
     *
     * @return the per-world playtime thresholds
     */
    @NotNull
    public Map<String, Long> getWorldPlaytimeRequirements() {
        return new HashMap<>(this.worldPlaytimeRequirements);
    }

    /**
     * Indicates whether the requirement evaluates total playtime instead of world-specific values.
     *
     * @return {@code true} when global playtime is used; otherwise {@code false}
     */
    public boolean isUseTotalPlaytime() {
        return this.useTotalPlaytime;
    }

    /**
     * Retrieves the optional description supplied for the requirement.
     *
     * @return the description, or {@code null} if none was provided
     */
    @Nullable
    public String getDescription() {
        return this.description;
    }

    /**
     * Formats the required playtime for display depending on whether global or per-world thresholds are used.
     *
     * @return the formatted representation of the required playtime
     */
    @JsonIgnore
    @NotNull
    public String getFormattedRequiredPlaytime() {
        if (!this.useTotalPlaytime) {
            return this.formatWorldRequirements();
        }
        return formatDuration(this.requiredPlaytimeSeconds);
    }

    /**
     * Formats the player's current playtime for display depending on the active evaluation mode.
     *
     * @param player the player whose playtime should be formatted
     * @return the formatted representation of the player's current playtime
     */
    @JsonIgnore
    @NotNull
    public String getFormattedCurrentPlaytime(final @NotNull Player player) {
        if (!this.useTotalPlaytime) {
            return formatCurrentWorldPlaytime(player);
        }
        final long currentSeconds = getTotalPlaytimeSeconds(player);
        return formatDuration(currentSeconds);
    }

    /**
     * Formats a duration expressed in seconds using a compact human-readable representation.
     *
     * @param seconds the duration in seconds
     * @return the formatted duration string
     */
    @JsonIgnore
    @NotNull
    public static String formatDuration(final long seconds) {
        if (seconds <= 0) {
            return "0s";
        }

        final long days = TimeUnit.SECONDS.toDays(seconds);
        final long hours = TimeUnit.SECONDS.toHours(seconds) % 24;
        final long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;
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
     * Validates the configuration and throws an exception when invalid thresholds are detected.
     *
     * @throws IllegalStateException if any configured values are invalid
     */
    @JsonIgnore
    public void validate() {
        if (this.requiredPlaytimeSeconds < 0) {
            throw new IllegalStateException("Required playtime cannot be negative: " + this.requiredPlaytimeSeconds);
        }
        if (!this.useTotalPlaytime && this.worldPlaytimeRequirements.isEmpty()) {
            throw new IllegalStateException(
                    "World playtime requirements cannot be empty when not using total playtime"
            );
        }

        for (final Map.Entry<String, Long> entry : this.worldPlaytimeRequirements.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                throw new IllegalStateException("World name cannot be null or empty");
            }
            if (entry.getValue() < 0) {
                throw new IllegalStateException("World playtime requirement cannot be negative: " + entry.getValue());
            }
        }
    }

    @Nullable
    private World getCachedWorld(final @NotNull String worldName) {
        return this.worldCache.computeIfAbsent(worldName, Bukkit::getWorld);
    }

    private boolean checkWorldPlaytimeRequirements(final @NotNull Player player) {
        for (final Map.Entry<String, Long> entry : worldPlaytimeRequirements.entrySet()) {
            final String worldName = entry.getKey();
            final long requiredSeconds = entry.getValue();
            final long actualSeconds = getWorldPlaytimeSeconds(player, worldName);
            if (actualSeconds < requiredSeconds) {
                return false;
            }
        }
        return true;
    }

    private double calculateWorldPlaytimeProgress(final @NotNull Player player) {
        if (this.worldPlaytimeRequirements.isEmpty()) {
            return 1.0;
        }

        double totalProgress = 0.0;
        int validRequirements = 0;

        for (final Map.Entry<String, Long> entry : this.worldPlaytimeRequirements.entrySet()) {
            final String worldName = entry.getKey();
            final long requiredSeconds = entry.getValue();

            if (requiredSeconds <= 0) {
                totalProgress += 1.0;
            } else {
                final long actualSeconds = getWorldPlaytimeSeconds(player, worldName);
                totalProgress += Math.min(1.0, (double) actualSeconds / requiredSeconds);
            }
            validRequirements++;
        }

        return validRequirements > 0 ? totalProgress / validRequirements : 1.0;
    }

    @NotNull
    private String formatWorldRequirements() {
        if (this.worldPlaytimeRequirements.isEmpty()) {
            return "No world requirements";
        }

        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, Long> entry : this.worldPlaytimeRequirements.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append(": ").append(formatDuration(entry.getValue()));
        }
        return sb.toString();
    }

    @NotNull
    private String formatCurrentWorldPlaytime(final @NotNull Player player) {
        if (this.worldPlaytimeRequirements.isEmpty()) {
            return "No world requirements";
        }

        final StringBuilder sb = new StringBuilder();
        for (final String worldName : this.worldPlaytimeRequirements.keySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            final long currentSeconds = getWorldPlaytimeSeconds(player, worldName);
            sb.append(worldName).append(": ").append(formatDuration(currentSeconds));
        }
        return sb.toString();
    }
}
