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
 * @version 1.0.0
 * @since TBD
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

    public PlaytimeRequirement(final long requiredPlaytimeSeconds) {
        this(requiredPlaytimeSeconds, null, true, null);
    }

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

    @JsonIgnore
    @NotNull
    public static PlaytimeRequirement forWorlds(
            final @NotNull Map<String, Long> worldPlaytimeMap,
            final @Nullable String description
    ) {
        return new PlaytimeRequirement(0, worldPlaytimeMap, false, description);
    }

    @Override
    public boolean isMet(final @NotNull Player player) {
        if (this.useTotalPlaytime) {
            return this.getTotalPlaytimeSeconds(player) >= this.requiredPlaytimeSeconds;
        } else {
            return this.checkWorldPlaytimeRequirements(player);
        }
    }

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

    @Override
    public void consume(final @NotNull Player player) {
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.playtime";
    }

    @JsonIgnore
    public long getTotalPlaytimeSeconds(final @NotNull Player player) {
        final int playtimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        return playtimeTicks / 20;
    }

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

    public long getRequiredPlaytimeSeconds() {
        return this.requiredPlaytimeSeconds;
    }

    @JsonIgnore
    public long getRequiredPlaytimeMinutes() {
        return TimeUnit.SECONDS.toMinutes(this.requiredPlaytimeSeconds);
    }

    @JsonIgnore
    public long getRequiredPlaytimeHours() {
        return TimeUnit.SECONDS.toHours(this.requiredPlaytimeSeconds);
    }

    @JsonIgnore
    public long getRequiredPlaytimeDays() {
        return TimeUnit.SECONDS.toDays(this.requiredPlaytimeSeconds);
    }

    @NotNull
    public Map<String, Long> getWorldPlaytimeRequirements() {
        return new HashMap<>(this.worldPlaytimeRequirements);
    }

    public boolean isUseTotalPlaytime() {
        return this.useTotalPlaytime;
    }

    @Nullable
    public String getDescription() {
        return this.description;
    }

    @JsonIgnore
    @NotNull
    public String getFormattedRequiredPlaytime() {
        if (!this.useTotalPlaytime) {
            return this.formatWorldRequirements();
        }
        return formatDuration(this.requiredPlaytimeSeconds);
    }

    @JsonIgnore
    @NotNull
    public String getFormattedCurrentPlaytime(final @NotNull Player player) {
        if (!this.useTotalPlaytime) {
            return formatCurrentWorldPlaytime(player);
        }
        final long currentSeconds = getTotalPlaytimeSeconds(player);
        return formatDuration(currentSeconds);
    }

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