package de.jexcellence.jexplatform.reward;

import de.jexcellence.jexplatform.reward.impl.*;
import org.jetbrains.annotations.NotNull;

/**
 * Registers all core platform reward types with a registry.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class CoreRewardTypes {

    /** Item reward. */
    public static final RewardType ITEM =
            RewardType.core("ITEM", ItemReward.class);

    /** Currency deposit reward. */
    public static final RewardType CURRENCY =
            RewardType.core("CURRENCY", CurrencyReward.class);

    /** Experience points/levels reward. */
    public static final RewardType EXPERIENCE =
            RewardType.core("EXPERIENCE", ExperienceReward.class);

    /** Console command reward. */
    public static final RewardType COMMAND =
            RewardType.core("COMMAND", CommandReward.class);

    /** Composite reward (sequential/parallel). */
    public static final RewardType COMPOSITE =
            RewardType.core("COMPOSITE", CompositeReward.class);

    /** Choice-based reward. */
    public static final RewardType CHOICE =
            RewardType.core("CHOICE", ChoiceReward.class);

    /** Permission node reward. */
    public static final RewardType PERMISSION =
            RewardType.core("PERMISSION", PermissionReward.class);

    /** Sound effect reward. */
    public static final RewardType SOUND =
            RewardType.core("SOUND", SoundReward.class);

    /** Particle effect reward. */
    public static final RewardType PARTICLE =
            RewardType.core("PARTICLE", ParticleReward.class);

    /** Teleportation reward. */
    public static final RewardType TELEPORT =
            RewardType.core("TELEPORT", TeleportReward.class);

    /** Title/subtitle display reward. */
    public static final RewardType TITLE =
            RewardType.core("TITLE", TitleReward.class);

    /** Vanishing chest reward. */
    public static final RewardType VANISHING_CHEST =
            RewardType.core("VANISHING_CHEST", VanishingChestReward.class);

    private CoreRewardTypes() {
    }

    /**
     * Registers all core reward types with the given registry.
     *
     * @param registry the reward registry
     */
    public static void registerAll(@NotNull RewardRegistry registry) {
        registry.register(ITEM);
        registry.register(CURRENCY);
        registry.register(EXPERIENCE);
        registry.register(COMMAND);
        registry.register(COMPOSITE);
        registry.register(CHOICE);
        registry.register(PERMISSION);
        registry.register(SOUND);
        registry.register(PARTICLE);
        registry.register(TELEPORT);
        registry.register(TITLE);
        registry.register(VANISHING_CHEST);
    }
}
