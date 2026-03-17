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

package com.raindropcentral.rplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

/**
 * Converts Bukkit {@link World} references to their world name for persistence and restores them during.
 * entity hydration.
 *
 * <p>{@code null} attributes map to {@code null} columns and blank column values return {@code null}
 * attributes. When no world matches the stored name, {@code null} is returned so callers can respond to
 * missing resources.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Converter(autoApply = true)
public class WorldConverter implements AttributeConverter<World, String> {

    /**
     * Serialises the provided {@link World} to its world-name representation.
     *
     * @param attribute the world being persisted; may be {@code null}
     * @return the world name or {@code null} when {@code attribute} is {@code null}
     */
    @Override
    public String convertToDatabaseColumn(@Nullable final World attribute) {
        return attribute == null ? null : attribute.getName();
    }

    /**
     * Resolves the stored world name back into a {@link World} instance.
     *
     * @param dbData the raw column value; blank and {@code null} values produce {@code null}
     * @return the resolved world, or {@code null} if the world cannot be located or the value is blank
     */
    @Override
    public World convertToEntityAttribute(@Nullable final String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return Bukkit.getWorld(dbData.trim());
    }
}
