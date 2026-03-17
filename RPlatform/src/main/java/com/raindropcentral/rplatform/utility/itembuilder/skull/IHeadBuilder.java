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

package com.raindropcentral.rplatform.utility.itembuilder.skull;

import com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fluent builder contract for skull metadata providing player and custom texture entry points.
 *
 * <p>Implementations are expected to honour the fluent contract by returning their concrete builder
 * instance from every method and never {@code null}. Optional player parameters annotated with
 * {@link Nullable} must be handled gracefully, leaving any previously configured metadata in place
 * when {@code null} values are supplied. Implementations should only persist accumulated mutations
 * when {@link #build()} is invoked, ensuring intermediate calls remain chainable and reversible.</p>
 *
 * @param <B> concrete builder type returned from fluent calls
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public interface IHeadBuilder<B extends IHeadBuilder<B>> extends IUnifiedItemBuilder<SkullMeta, B> {

        /**
         * Applies the head texture from an online player reference.
         *
         * @param player player whose profile should supply the texture
         * @return fluent builder reference for chaining
         */
        B setPlayerHead(@Nullable Player player);

        /**
         * Applies the head texture from an offline player reference.
         *
         * @param offlinePlayer offline player whose profile should supply the texture
         * @return fluent builder reference for chaining
         */
        B setPlayerHead(@Nullable OfflinePlayer offlinePlayer);

        /**
         * Applies a custom base64 encoded texture for the provided synthetic profile UUID.
         *
         * @param uuid profile identifier associated with the texture data
         * @param textures base64 encoded texture payload
         * @return fluent builder reference for chaining
         */
        B setCustomTexture(
                @NotNull UUID uuid,
                @NotNull String textures
        );

}