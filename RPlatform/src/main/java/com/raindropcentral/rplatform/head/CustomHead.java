package com.raindropcentral.rplatform.head;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class CustomHead {

    private final String identifier;
    private final UUID textureUuid;
    private final String textureValue;
    private final HeadCategory category;
    private final String translationKey;

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

    protected CustomHead(
            final @NotNull String identifier,
            final @NotNull String textureUuid,
            final @NotNull String textureValue,
            final @NotNull HeadCategory category
    ) {
        this(identifier, UUID.fromString(textureUuid), textureValue, category);
    }

    protected CustomHead(
            final @NotNull String identifier,
            final @NotNull String textureUuid,
            final @NotNull String textureValue
    ) {
        this(identifier, textureUuid, textureValue, HeadCategory.INVENTORY);
    }

    public abstract @NotNull ItemStack createItem();

    public @NotNull String getIdentifier() {
        return identifier;
    }

    public @NotNull UUID getTextureUuid() {
        return textureUuid;
    }

    public @NotNull String getTextureValue() {
        return textureValue;
    }

    public @NotNull HeadCategory getCategory() {
        return category;
    }

    public @NotNull String getTranslationKey() {
        return translationKey;
    }
}
