package de.jexcellence.oneblock.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

/**
 * Evolution Experience Requirement - requires experience for evolution advancement.
 * Extends RPlatform's AbstractRequirement for unified requirement handling.
 *
 * @author JExcellence
 * @version 2.1.0
 * @since 2.0.0
 */
@JsonTypeName("EVOLUTION_EXPERIENCE")
public class EvolutionExperienceRequirement extends AbstractRequirement {

    private static final Logger LOGGER = Logger.getLogger("JExOneblock");

    public enum ExperienceType {
        EVOLUTION_XP("Evolution XP"),
        MINECRAFT_LEVELS("Minecraft Levels"),
        MINECRAFT_POINTS("Minecraft Points");

        private final String displayName;

        ExperienceType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @JsonProperty("requiredExperience")
    private final double requiredExperience;

    @JsonProperty("experienceType")
    private final ExperienceType experienceType;

    @JsonProperty("evolutionName")
    @Nullable
    private final String evolutionName;

    @JsonCreator
    public EvolutionExperienceRequirement(
            @JsonProperty("requiredExperience") double requiredExperience,
            @JsonProperty("experienceType") @Nullable ExperienceType experienceType,
            @JsonProperty("evolutionName") @Nullable String evolutionName,
            @JsonProperty("consumeOnComplete") @Nullable Boolean consumeOnComplete
    ) {
        super("EXPERIENCE_LEVEL", consumeOnComplete != null ? consumeOnComplete : false);
        this.requiredExperience = requiredExperience;
        this.experienceType = experienceType != null ? experienceType : ExperienceType.EVOLUTION_XP;
        this.evolutionName = evolutionName;
    }

    public EvolutionExperienceRequirement(double requiredExperience, ExperienceType experienceType) {
        this(requiredExperience, experienceType, null, false);
    }

    public EvolutionExperienceRequirement(double requiredExperience) {
        this(requiredExperience, ExperienceType.EVOLUTION_XP, null, false);
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        return getCurrentExperience(player) >= requiredExperience;
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        if (requiredExperience <= 0) return 1.0;
        return Math.min(1.0, getCurrentExperience(player) / requiredExperience);
    }

    @Override
    public void consume(@NotNull Player player) {
        if (!shouldConsume()) return;

        double current = getCurrentExperience(player);
        if (current < requiredExperience) {
            LOGGER.warning("Cannot consume experience - player " + player.getName() +
                    " doesn't have enough " + experienceType.getDisplayName());
            return;
        }

        switch (experienceType) {
            case EVOLUTION_XP -> consumeEvolutionXp(player, requiredExperience);
            case MINECRAFT_LEVELS -> consumeMinecraftLevels(player, (int) requiredExperience);
            case MINECRAFT_POINTS -> consumeMinecraftPoints(player, (int) requiredExperience);
        }
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "evolution.requirement.experience";
    }

    private double getCurrentExperience(@NotNull Player player) {
        return switch (experienceType) {
            case EVOLUTION_XP -> getEvolutionXp(player);
            case MINECRAFT_LEVELS -> player.getLevel();
            case MINECRAFT_POINTS -> player.getTotalExperience();
        };
    }

    private double getEvolutionXp(@NotNull Player player) {
        try {
            var provider = Bukkit.getServicesManager()
                    .getRegistration(Class.forName("de.jexcellence.oneblock.repository.OneblockIslandRepository"));
            if (provider != null) {
                var repo = provider.getProvider();
                var method = repo.getClass().getMethod("findByOwnerUuid", java.util.UUID.class);
                @SuppressWarnings("unchecked")
                var result = (java.util.Optional<?>) method.invoke(repo, player.getUniqueId());
                if (result.isPresent()) {
                    var island = result.get();
                    var oneblockMethod = island.getClass().getMethod("getOneblock");
                    var oneblock = oneblockMethod.invoke(island);
                    if (oneblock != null) {
                        var xpMethod = oneblock.getClass().getMethod("getEvolutionExperience");
                        return ((Number) xpMethod.invoke(oneblock)).doubleValue();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.fine("Could not get evolution XP: " + e.getMessage());
        }
        return 0;
    }

    private void consumeEvolutionXp(@NotNull Player player, double amount) {
        // Evolution XP is typically not consumed - it's a milestone
        LOGGER.fine("Evolution XP consumption requested but not implemented (milestone-based)");
    }

    private void consumeMinecraftLevels(@NotNull Player player, int levels) {
        int current = player.getLevel();
        if (current >= levels) {
            player.setLevel(current - levels);
        }
    }

    private void consumeMinecraftPoints(@NotNull Player player, int points) {
        int current = player.getTotalExperience();
        if (current >= points) {
            player.setTotalExperience(current - points);
        }
    }

    public double getRequiredExperience() {
        return requiredExperience;
    }

    @NotNull
    public ExperienceType getExperienceType() {
        return experienceType;
    }

    @Nullable
    public String getEvolutionName() {
        return evolutionName;
    }

    @Override
    public String toString() {
        return "EvolutionExperienceRequirement{xp=" + requiredExperience +
                ", type=" + experienceType + ", evolution='" + evolutionName + "'}";
    }
}
