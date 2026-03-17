/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rplatform.head;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a custom textured head that can be materialized into an {@link ItemStack}.
 * Implementations are expected to supply the underlying texture data required to render the
 * head and the category that determines how the head is surfaced in user interfaces.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public abstract class CustomHead {

    /**
     * Unique identifier used to reference this head across configuration files and translations.
     */
    private final String identifier;

    /**
     * UUID representing the texture profile that the client will resolve for this head.
     */
    private final UUID textureUuid;

    /**
     * Base64 encoded texture payload applied to the player's skull model.
     */
    private final String textureValue;

    /**
     * Category describing where the head is displayed (decoration, inventory, or player-focused).
     */
    private final HeadCategory category;

    /**
     * Translation key prefix used to localize this head within language bundles.
     */
    private final String translationKey;

    /**
     * Creates a new custom head definition using an already parsed texture {@link UUID}.
     *
     * @param identifier   stable identifier for referencing the head.
     * @param textureUuid  UUID pointing to the texture profile assigned to the head.
     * @param textureValue Base64 texture payload rendered on the head.
     * @param category     grouping that indicates how the head should be categorized.
     */
    protected CustomHead(
            final @NotNull String identifier,
            final @NotNull UUID textureUuid,
            final @NotNull String textureValue,
            final @NotNull HeadCategory category
    ) {
        this.identifier = identifier;
        this.textureUuid = textureUuid;
        this.textureValue = textureValue;
        this.category = category;
        this.translationKey = "head." + identifier;
    }

    /**
     * Creates a new custom head definition by parsing the provided texture UUID string.
     *
     * @param identifier   stable identifier for referencing the head.
     * @param textureUuid  string-form UUID pointing to the texture profile assigned to the head.
     * @param textureValue Base64 texture payload rendered on the head.
     * @param category     grouping that indicates how the head should be categorized.
     */
    protected CustomHead(
            final @NotNull String identifier,
            final @NotNull String textureUuid,
            final @NotNull String textureValue,
            final @NotNull HeadCategory category
    ) {
        this(identifier, UUID.fromString(textureUuid), textureValue, category);
    }

    /**
     * Creates a custom head definition that defaults to {@link HeadCategory#INVENTORY}.
     *
     * @param identifier   stable identifier for referencing the head.
     * @param textureUuid  string-form UUID pointing to the texture profile assigned to the head.
     * @param textureValue Base64 texture payload rendered on the head.
     */
    protected CustomHead(
            final @NotNull String identifier,
            final @NotNull String textureUuid,
            final @NotNull String textureValue
    ) {
        this(identifier, textureUuid, textureValue, HeadCategory.INVENTORY);
    }

    /**
     * Creates a new {@link ItemStack} representing this head with its configured texture.
     * Implementations should ensure the stack is safe to hand out to players, including any
     * metadata such as display name or lore entries associated with the translation key.
     *
     * @return a freshly created item stack for this head definition.
     */
    public abstract @NotNull ItemStack createItem();

    /**
     * Retrieves the stable identifier for this head.
     *
     * @return unique identifier assigned to the head.
     */
    public @NotNull String getIdentifier() {
        return identifier;
    }

    /**
     * Provides the UUID associated with the head's texture profile.
     *
     * @return UUID linked to the head texture.
     */
    public @NotNull UUID getTextureUuid() {
        return textureUuid;
    }

    /**
     * Returns the Base64 encoded texture payload used when creating the item.
     *
     * @return texture payload for the skull.
     */
    public @NotNull String getTextureValue() {
        return textureValue;
    }

    /**
     * Gets the categorization for this head, which influences how it is surfaced in menus.
     *
     * @return head category designation.
     */
    public @NotNull HeadCategory getCategory() {
        return category;
    }

    /**
     * Supplies the translation key that localizes this head's metadata in language bundles.
     *
     * @return translation key generated for the head.
     */
    public @NotNull String getTranslationKey() {
        return translationKey;
    }
}
