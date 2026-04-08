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

package com.raindropcentral.rdt.view.town;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.service.LevelProgressSnapshot;
import com.raindropcentral.rdt.service.LevelScope;
import me.devnatan.inventoryframework.context.Context;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared initial-data and progression-resolution helpers for level views.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
final class TownLevelViewSupport {

    static final String SCOPE_KEY = "level_scope";
    static final String PREVIEW_LEVEL_KEY = "preview_level";
    static final String ENTRY_KEY = "level_entry_key";
    static final String CONTRIBUTION_STATUS_KEY = "level_contribution_status";
    static final String CONTRIBUTION_AMOUNT_KEY = "level_contribution_amount";
    static final String CONTRIBUTION_COMPLETED_KEY = "level_requirement_completed";
    static final String LEVEL_READY_KEY = "level_ready";
    static final String LEVEL_UP_STATUS_KEY = "level_up_status";
    static final String LEVEL_UP_NEW_LEVEL_KEY = "level_up_new_level";
    static final String LEVEL_UP_PREVIOUS_LEVEL_KEY = "level_up_previous_level";

    private TownLevelViewSupport() {
    }

    static @Nullable Map<String, Object> copyInitialData(final @NotNull Context context) {
        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> rawMap)) {
            return null;
        }

        final Map<String, Object> copied = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                copied.put(key, entry.getValue());
            }
        }
        return copied;
    }

    static @NotNull Map<String, Object> mergeInitialData(
        final @NotNull Context context,
        final @NotNull Map<String, Object> extraData
    ) {
        final Map<String, Object> copiedData = copyInitialData(context);
        final Map<String, Object> mergedData = copiedData == null
            ? new LinkedHashMap<>()
            : new LinkedHashMap<>(copiedData);
        mergedData.putAll(extraData);
        return mergedData;
    }

    static @NotNull Map<String, Object> stripTransientData(final @NotNull Map<String, Object> data) {
        final Map<String, Object> sanitizedData = new LinkedHashMap<>(data);
        sanitizedData.remove(ENTRY_KEY);
        sanitizedData.remove(CONTRIBUTION_STATUS_KEY);
        sanitizedData.remove(CONTRIBUTION_AMOUNT_KEY);
        sanitizedData.remove(CONTRIBUTION_COMPLETED_KEY);
        sanitizedData.remove(LEVEL_READY_KEY);
        sanitizedData.remove(LEVEL_UP_STATUS_KEY);
        sanitizedData.remove(LEVEL_UP_NEW_LEVEL_KEY);
        sanitizedData.remove(LEVEL_UP_PREVIOUS_LEVEL_KEY);
        return sanitizedData;
    }

    static @NotNull Map<String, Object> createNexusNavigationData(
        final @NotNull Context context,
        final @NotNull RTown town
    ) {
        return mergeInitialData(
            context,
            Map.of(
                "plugin", plugin(context),
                "town_uuid", town.getTownUUID(),
                SCOPE_KEY, LevelScope.NEXUS
            )
        );
    }

    static @NotNull Map<String, Object> createSecurityNavigationData(
        final @NotNull Context context,
        final @NotNull RTownChunk townChunk
    ) {
        return createChunkNavigationData(context, townChunk);
    }

    static @NotNull Map<String, Object> createChunkNavigationData(
        final @NotNull Context context,
        final @NotNull RTownChunk townChunk
    ) {
        final LevelScope scope = LevelScope.fromChunkType(townChunk.getChunkType());
        if (scope == null) {
            throw new IllegalArgumentException("Chunk type " + townChunk.getChunkType() + " has no progression path");
        }
        return mergeInitialData(
            context,
            Map.of(
                "plugin", plugin(context),
                "town_uuid", townChunk.getTown().getTownUUID(),
                "world_name", townChunk.getWorldName(),
                "chunk_x", townChunk.getX(),
                "chunk_z", townChunk.getZ(),
                SCOPE_KEY, scope
            )
        );
    }

    static @Nullable RDT plugin(final @NotNull Context context) {
        final Map<String, Object> data = copyInitialData(context);
        return data != null && data.get("plugin") instanceof RDT plugin ? plugin : null;
    }

    static @NotNull LevelScope scope(final @NotNull Context context) {
        final Map<String, Object> data = copyInitialData(context);
        if (data == null) {
            return LevelScope.NEXUS;
        }
        final Object rawScope = data.get(SCOPE_KEY);
        if (rawScope instanceof LevelScope scope) {
            return scope;
        }
        if (rawScope instanceof String serializedScope) {
            try {
                return LevelScope.valueOf(serializedScope.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (final IllegalArgumentException ignored) {
                return LevelScope.NEXUS;
            }
        }
        return LevelScope.NEXUS;
    }

    static @Nullable RTown resolveTown(final @NotNull Context context) {
        final RDT plugin = plugin(context);
        if (plugin == null || plugin.getTownRuntimeService() == null) {
            return null;
        }
        final Map<String, Object> data = copyInitialData(context);
        if (data == null || !(data.get("town_uuid") instanceof UUID townUuid)) {
            return null;
        }
        return plugin.getTownRuntimeService().getTown(townUuid);
    }

    static @Nullable RTownChunk resolveChunk(final @NotNull Context context) {
        final RDT plugin = plugin(context);
        if (plugin == null || plugin.getTownRuntimeService() == null) {
            return null;
        }
        final Map<String, Object> data = copyInitialData(context);
        if (data == null
            || !(data.get("town_uuid") instanceof UUID townUuid)
            || !(data.get("world_name") instanceof String worldName)
            || !(data.get("chunk_x") instanceof Number chunkX)
            || !(data.get("chunk_z") instanceof Number chunkZ)) {
            return null;
        }
        return plugin.getTownRuntimeService().getTownChunk(townUuid, worldName, chunkX.intValue(), chunkZ.intValue());
    }

    static @Nullable Integer previewLevel(final @NotNull Context context) {
        final Map<String, Object> data = copyInitialData(context);
        if (data == null) {
            return null;
        }
        final Object rawPreviewLevel = data.get(PREVIEW_LEVEL_KEY);
        if (rawPreviewLevel instanceof Number number) {
            return number.intValue();
        }
        if (rawPreviewLevel instanceof String serializedPreviewLevel) {
            try {
                return Integer.parseInt(serializedPreviewLevel.trim());
            } catch (final NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    static @Nullable LevelProgressSnapshot resolveSnapshot(final @NotNull Context context) {
        final RDT plugin = plugin(context);
        final RTown town = resolveTown(context);
        if (plugin == null || plugin.getTownRuntimeService() == null || town == null) {
            return null;
        }

        return switch (scope(context)) {
            case NEXUS -> previewLevel(context) == null
                ? plugin.getTownRuntimeService().getNexusLevelProgress(context.getPlayer(), town)
                : plugin.getTownRuntimeService().getNexusLevelProgress(context.getPlayer(), town, previewLevel(context));
            case SECURITY, BANK, FARM, OUTPOST -> {
                final RTownChunk townChunk = resolveChunk(context);
                if (townChunk == null) {
                    yield null;
                }
                if (LevelScope.fromChunkType(townChunk.getChunkType()) == null) {
                    yield null;
                }
                yield previewLevel(context) == null
                    ? plugin.getTownRuntimeService().getChunkLevelProgress(context.getPlayer(), townChunk)
                    : plugin.getTownRuntimeService().getChunkLevelProgress(context.getPlayer(), townChunk, previewLevel(context));
            }
        };
    }
}
