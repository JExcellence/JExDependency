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

    public PlaytimeRequirement(long requiredPlaytimeSeconds) {
        this(requiredPlaytimeSeconds, null, true, null);
    }

    @JsonCreator
    public PlaytimeRequirement(@JsonProperty("requiredPlaytimeSeconds") long requiredPlaytimeSeconds,
                              @JsonProperty("worldPlaytimeRequirements") @Nullable Map<String, Long> worldPlaytimeRequirements,
                              @JsonProperty("useTotalPlaytime") @Nullable Boolean useTotalPlaytime,
                              @JsonProperty("description") @Nullable String description) {
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

    @Override
    public boolean isMet(@NotNull Player player) {
        if (useTotalPlaytime) {
            return getTotalPlaytimeSeconds(player) >= requiredPlaytimeSeconds;
        } else {
            return checkWorldPlaytimeRequirements(player);
        }
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        if (useTotalPlaytime) {
            if (requiredPlaytimeSeconds <= 0) {
                return 1.0;
            }
            var currentPlaytime = getTotalPlaytimeSeconds(player);
            return Math.min(1.0, (double) currentPlaytime / requiredPlaytimeSeconds);
        } else {
            return calculateWorldPlaytimeProgress(player);
        }
    }

    @Override
    public void consume(@NotNull Player player) {
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.playtime";
    }

    @JsonIgnore
    public long getTotalPlaytimeSeconds(@NotNull Player player) {
        var playtimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        return playtimeTicks / 20;
    }

    @JsonIgnore
    public long getWorldPlaytimeSeconds(@NotNull Player player, @NotNull String worldName) {
        var world = getCachedWorld(worldName);
        if (world == null) {
            return 0;
        }

        try {
            var playtimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
            return playtimeTicks / 20;
        } catch (Exception e) {
            return 0;
        }
    }

    public long getRequiredPlaytimeSeconds() {
        return requiredPlaytimeSeconds;
    }

    @JsonIgnore
    public long getRequiredPlaytimeMinutes() {
        return TimeUnit.SECONDS.toMinutes(requiredPlaytimeSeconds);
    }

    @JsonIgnore
    public long getRequiredPlaytimeHours() {
        return TimeUnit.SECONDS.toHours(requiredPlaytimeSeconds);
    }

    @JsonIgnore
    public long getRequiredPlaytimeDays() {
        return TimeUnit.SECONDS.toDays(requiredPlaytimeSeconds);
    }

    @NotNull
    public Map<String, Long> getWorldPlaytimeRequirements() {
        return new HashMap<>(worldPlaytimeRequirements);
    }

    public boolean isUseTotalPlaytime() {
        return useTotalPlaytime;
    }

    @Nullable
    public String getDescription() {
        return description;
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
    private World getCachedWorld(@NotNull String worldName) {
        return worldCache.computeIfAbsent(worldName, Bukkit::getWorld);
    }

    private boolean checkWorldPlaytimeRequirements(@NotNull Player player) {
        for (var entry : worldPlaytimeRequirements.entrySet()) {
            var worldName = entry.getKey();
            var requiredSeconds = entry.getValue();
            var actualSeconds = getWorldPlaytimeSeconds(player, worldName);
            if (actualSeconds < requiredSeconds) {
                return false;
            }
        }
        return true;
    }

    private double calculateWorldPlaytimeProgress(@NotNull Player player) {
        if (worldPlaytimeRequirements.isEmpty()) {
            return 1.0;
        }

        var totalProgress = 0.0;
        var validRequirements = 0;

        for (var entry : worldPlaytimeRequirements.entrySet()) {
            var worldName = entry.getKey();
            var requiredSeconds = entry.getValue();

            if (requiredSeconds <= 0) {
                totalProgress += 1.0;
            } else {
                var actualSeconds = getWorldPlaytimeSeconds(player, worldName);
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
