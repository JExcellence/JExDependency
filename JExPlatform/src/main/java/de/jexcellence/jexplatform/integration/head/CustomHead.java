package de.jexcellence.jexplatform.integration.head;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Abstract custom head with texture information and category.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public abstract class CustomHead {

    private final String identifier;
    private final UUID textureUuid;
    private final String textureValue;
    private final HeadCategory category;
    private final String translationKey;

    /**
     * Creates a custom head.
     *
     * @param identifier     the unique identifier
     * @param textureUuid    the texture profile UUID
     * @param textureValue   the Base64 texture value
     * @param category       the head category
     * @param translationKey the translation key for the display name
     */
    protected CustomHead(@NotNull String identifier, @NotNull UUID textureUuid,
                         @NotNull String textureValue, @NotNull HeadCategory category,
                         @NotNull String translationKey) {
        this.identifier = identifier;
        this.textureUuid = textureUuid;
        this.textureValue = textureValue;
        this.category = category;
        this.translationKey = translationKey;
    }

    /**
     * Returns the unique identifier.
     *
     * @return the identifier
     */
    public @NotNull String identifier() {
        return identifier;
    }

    /**
     * Returns the texture profile UUID.
     *
     * @return the texture UUID
     */
    public @NotNull UUID textureUuid() {
        return textureUuid;
    }

    /**
     * Returns the Base64 texture value.
     *
     * @return the texture value
     */
    public @NotNull String textureValue() {
        return textureValue;
    }

    /**
     * Returns the head category.
     *
     * @return the category
     */
    public @NotNull HeadCategory category() {
        return category;
    }

    /**
     * Returns the translation key for the display name.
     *
     * @return the translation key
     */
    public @NotNull String translationKey() {
        return translationKey;
    }

    /**
     * Creates the ItemStack representation of this head.
     *
     * @return the head item stack
     */
    public abstract @NotNull ItemStack createItem();
}
