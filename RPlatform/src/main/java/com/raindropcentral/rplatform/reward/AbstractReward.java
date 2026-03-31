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

package com.raindropcentral.rplatform.reward;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.raindropcentral.rplatform.reward.impl.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the type API type.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ItemReward.class, name = "ITEM"),
    @JsonSubTypes.Type(value = CurrencyReward.class, name = "CURRENCY"),
    @JsonSubTypes.Type(value = ExperienceReward.class, name = "EXPERIENCE"),
    @JsonSubTypes.Type(value = CommandReward.class, name = "COMMAND"),
    @JsonSubTypes.Type(value = CompositeReward.class, name = "COMPOSITE"),
    @JsonSubTypes.Type(value = ChoiceReward.class, name = "CHOICE"),
    @JsonSubTypes.Type(value = PermissionReward.class, name = "PERMISSION"),
    @JsonSubTypes.Type(value = TeleportReward.class, name = "TELEPORT"),
    @JsonSubTypes.Type(value = ParticleReward.class, name = "PARTICLE"),
    @JsonSubTypes.Type(value = VanishingChestReward.class, name = "VANISHING_CHEST")
})
@JsonIgnoreProperties(value = {"typeId", "estimatedValue", "descriptionKey"}, allowGetters = true, ignoreUnknown = true)
public abstract non-sealed class AbstractReward implements Reward {

    /**
     * Gets typeId.
     */
    @Override
    public abstract @NotNull String getTypeId();

    /**
     * Executes grant.
     */
    @Override
    public abstract @NotNull CompletableFuture<Boolean> grant(@NotNull Player player);

    /**
     * Gets estimatedValue.
     */
    @Override
    public abstract double getEstimatedValue();

    /**
     * Gets descriptionKey.
     */
    @Override
    public @NotNull String getDescriptionKey() {
        return "reward." + getTypeId().toLowerCase() + ".description";
    }

    /**
     * Executes validate.
     */
    public void validate() {
    }
}
