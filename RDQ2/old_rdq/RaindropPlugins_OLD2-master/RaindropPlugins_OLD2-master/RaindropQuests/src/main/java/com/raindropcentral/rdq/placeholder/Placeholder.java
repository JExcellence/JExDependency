package com.raindropcentral.rdq.placeholder;

import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rplatform.placeholder.APlaceholder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * PlaceholderAPI expansion for RaindropQuests.
 * <p>
 * This class registers and handles custom placeholders for the RaindropQuests plugin,
 * allowing integration with PlaceholderAPI. Supported placeholders include achievement-related
 * identifiers for use in messages, GUIs, and other plugin features.
 * </p>
 *
 * <ul>
 *   <li>Supported placeholders: {@code %rdq_achievements_<achievementId>%}, {@code %rdq_achievements_<achievementId>_title%}</li>
 *   <li>Extend this class to implement additional placeholder logic as needed.</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class Placeholder extends APlaceholder {

    //private final RDQPlaceholderUtil placeholderUtil;

    /**
     * Constructs a new {@code Placeholder} expansion for RaindropQuests.
     *
     * @param rdq The main RaindropQuests plugin instance
     */
    public Placeholder(
            final @NotNull RDQImpl rdq
    ) {
        super(rdq.getPlatform());
        //this.placeholderUtil = new RDQPlaceholderUtil(rdq);
    }

    /**
     * Defines the list of supported placeholder keys for this expansion.
     * <p>
     * Placeholders should be referenced without percent signs or identifiers.
     * </p>
     *
     * @return a list of supported placeholder keys
     */
    @Override
    public @NotNull List<String> setPlaceholder() {
        return
                List.of(
                        "rdq_achievements_<achievementId>",
                        "rdq_achievements_<achievementId>_title"
                );
    }

    /**
     * Handles a placeholder request for a player.
     * <p>
     * This method should be implemented to resolve the value of custom placeholders.
     * </p>
     *
     * @param player The player making the request
     * @param params The parameters for the placeholder request
     * @return The resolved placeholder value, or an empty string if not handled
     */
    @Override
    public @Nullable String onPlaceholder(Player player, @NotNull String params) {
        return "";
    }
}
