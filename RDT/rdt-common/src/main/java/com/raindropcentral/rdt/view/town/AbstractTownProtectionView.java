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
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.database.entity.TownRole;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rdt.utils.TownProtectionCategory;
import com.raindropcentral.rdt.utils.TownProtections;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Shared context, permission, and translation helpers for town protection menus.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
abstract class AbstractTownProtectionView extends BaseView {

    protected static final String WORLD_NAME_KEY = "world_name";
    protected static final String CHUNK_X_KEY = "chunk_x";
    protected static final String CHUNK_Z_KEY = "chunk_z";
    protected static final String ORIGIN_WORLD_NAME_KEY = "origin_world_name";
    protected static final String ORIGIN_CHUNK_X_KEY = "origin_chunk_x";
    protected static final String ORIGIN_CHUNK_Z_KEY = "origin_chunk_z";
    protected static final String PROTECTION_CATEGORY_KEY = "protection_category";
    protected static final String PROTECTION_VIEW_KEY = "protection_view";

    protected final State<RDT> plugin = initialState("plugin");
    protected final State<UUID> townUuid = initialState("town_uuid");

    protected AbstractTownProtectionView(final @Nullable Class<? extends View> parentClazz) {
        super(parentClazz);
    }

    @Override
    public void onOpen(final @NotNull OpenContext open) {
        final UUID resolvedTownUuid = this.townUuid.get(open);
        if (resolvedTownUuid != null
            && !TownProtectionEditSessionRegistry.acquire(resolvedTownUuid, open.getPlayer().getUniqueId())) {
            new I18n.Builder("town_protection_shared.messages.in_use", open.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            open.getPlayer().closeInventory();
            return;
        }
        super.onOpen(open);
    }

    @Override
    public void onClose(final @NotNull CloseContext close) {
        final UUID resolvedTownUuid = this.townUuid.get(close);
        if (resolvedTownUuid != null) {
            TownProtectionEditSessionRegistry.release(resolvedTownUuid, close.getPlayer().getUniqueId());
        }
        super.onClose(close);
    }

    protected final boolean hasScopedChunkTarget(final @NotNull Context context) {
        return this.resolveScopedWorldName(context) != null
            && this.resolveScopedChunkX(context) != null
            && this.resolveScopedChunkZ(context) != null;
    }

    protected final @Nullable RTown resolveTown(final @NotNull Context context) {
        final UUID resolvedTownUuid = this.townUuid.get(context);
        return resolvedTownUuid == null || this.plugin.get(context).getTownRuntimeService() == null
            ? null
            : this.plugin.get(context).getTownRuntimeService().getTown(resolvedTownUuid);
    }

    protected final @Nullable RTownChunk resolveChunk(final @NotNull Context context) {
        final UUID resolvedTownUuid = this.townUuid.get(context);
        final String resolvedWorldName = this.resolveScopedWorldName(context);
        final Integer resolvedChunkX = this.resolveScopedChunkX(context);
        final Integer resolvedChunkZ = this.resolveScopedChunkZ(context);
        if (resolvedTownUuid == null
            || resolvedWorldName == null
            || resolvedChunkX == null
            || resolvedChunkZ == null
            || this.plugin.get(context).getTownRuntimeService() == null) {
            return null;
        }
        final RTownChunk townChunk = this.plugin.get(context).getTownRuntimeService().getTownChunk(
            resolvedTownUuid,
            resolvedWorldName,
            resolvedChunkX,
            resolvedChunkZ
        );
        return townChunk != null && townChunk.getChunkType() == ChunkType.SECURITY ? townChunk : null;
    }

    protected final boolean viewerCanManageProtections(final @NotNull Context context, final @NotNull RTown town) {
        if (this.plugin.get(context).getTownRuntimeService() == null) {
            return false;
        }
        final RDTPlayer playerData = this.plugin.get(context).getTownRuntimeService().getPlayerData(context.getPlayer().getUniqueId());
        return playerData != null
            && Objects.equals(playerData.getTownUUID(), town.getTownUUID())
            && playerData.hasTownPermission(TownPermissions.TOWN_PROTECTIONS);
    }

    protected final boolean protectionEditingUnlocked(final @NotNull Context context, final @NotNull RTown town) {
        return town.hasSecurityChunk() && this.viewerCanManageProtections(context, town);
    }

    protected final boolean ensureProtectionAccess(final @NotNull SlotClickContext clickContext, final @NotNull RTown town) {
        if (!town.hasSecurityChunk()) {
            new I18n.Builder(this.getKey() + ".locked.security_required", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return false;
        }
        if (!this.viewerCanManageProtections(clickContext, town)) {
            new I18n.Builder(this.getKey() + ".locked.no_permission", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return false;
        }
        return true;
    }

    protected final @NotNull Map<String, Object> createProtectionNavigationData(
        final @NotNull Context context,
        final @Nullable TownProtectionCategory category
    ) {
        return this.createProtectionNavigationData(context, category, null);
    }

    protected final @NotNull Map<String, Object> createProtectionNavigationData(
        final @NotNull Context context,
        final @Nullable TownProtectionCategory category,
        final @Nullable String viewKey
    ) {
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put("plugin", this.plugin.get(context));
        data.put("town_uuid", this.townUuid.get(context));
        if (category != null) {
            data.put(PROTECTION_CATEGORY_KEY, category.name());
        }
        if (viewKey != null && !viewKey.isBlank()) {
            data.put(PROTECTION_VIEW_KEY, viewKey);
        }
        if (this.hasScopedChunkTarget(context)) {
            data.put(WORLD_NAME_KEY, this.resolveScopedWorldName(context));
            data.put(CHUNK_X_KEY, this.resolveScopedChunkX(context));
            data.put(CHUNK_Z_KEY, this.resolveScopedChunkZ(context));
        }
        copyOriginChunkTarget(this.extractData(context), data);
        return data;
    }

    protected final @Nullable Map<String, Object> createOriginChunkNavigationData(final @NotNull Context context) {
        return createOriginChunkNavigationData(this.plugin.get(context), this.townUuid.get(context), this.extractData(context));
    }

    protected final @Nullable TownProtectionCategory resolveProtectionCategory(final @NotNull Context context) {
        final Map<String, Object> data = this.extractData(context);
        if (data == null) {
            return null;
        }

        final Object rawCategory = data.get(PROTECTION_CATEGORY_KEY);
        if (rawCategory instanceof TownProtectionCategory category) {
            return category;
        }
        return rawCategory instanceof String categoryKey
            ? TownProtectionCategory.fromKey(categoryKey)
            : null;
    }

    protected final @Nullable String resolveProtectionViewKey(final @NotNull Context context) {
        final Map<String, Object> data = this.extractData(context);
        return data == null || !(data.get(PROTECTION_VIEW_KEY) instanceof String rawViewKey) || rawViewKey.isBlank()
            ? null
            : rawViewKey;
    }

    protected final @Nullable Map<String, Object> extractData(final @NotNull Context context) {
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

    protected final @Nullable String resolveScopedWorldName(final @NotNull Context context) {
        final Map<String, Object> data = this.extractData(context);
        return data == null || !(data.get(WORLD_NAME_KEY) instanceof String rawWorldName) || rawWorldName.isBlank()
            ? null
            : rawWorldName;
    }

    protected final @Nullable Integer resolveScopedChunkX(final @NotNull Context context) {
        final Map<String, Object> data = this.extractData(context);
        return this.asInteger(data == null ? null : data.get(CHUNK_X_KEY));
    }

    protected final @Nullable Integer resolveScopedChunkZ(final @NotNull Context context) {
        final Map<String, Object> data = this.extractData(context);
        return this.asInteger(data == null ? null : data.get(CHUNK_Z_KEY));
    }

    protected final @Nullable Integer asInteger(final @Nullable Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue);
            } catch (final NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    static void putOriginChunkTarget(final @NotNull Map<String, Object> target, final @NotNull RTownChunk townChunk) {
        target.put(ORIGIN_WORLD_NAME_KEY, townChunk.getWorldName());
        target.put(ORIGIN_CHUNK_X_KEY, townChunk.getX());
        target.put(ORIGIN_CHUNK_Z_KEY, townChunk.getZ());
    }

    static void copyOriginChunkTarget(
        final @Nullable Map<String, Object> source,
        final @NotNull Map<String, Object> target
    ) {
        final String worldName = resolveChunkWorldName(source, ORIGIN_WORLD_NAME_KEY);
        final Integer chunkX = asIntegerValue(source == null ? null : source.get(ORIGIN_CHUNK_X_KEY));
        final Integer chunkZ = asIntegerValue(source == null ? null : source.get(ORIGIN_CHUNK_Z_KEY));
        if (worldName == null || chunkX == null || chunkZ == null) {
            return;
        }
        target.put(ORIGIN_WORLD_NAME_KEY, worldName);
        target.put(ORIGIN_CHUNK_X_KEY, chunkX);
        target.put(ORIGIN_CHUNK_Z_KEY, chunkZ);
    }

    static @Nullable Map<String, Object> createOriginChunkNavigationData(
        final @NotNull RDT plugin,
        final @Nullable UUID townUuid,
        final @Nullable Map<String, Object> source
    ) {
        final String worldName = resolveChunkWorldName(source, ORIGIN_WORLD_NAME_KEY);
        final Integer chunkX = asIntegerValue(source == null ? null : source.get(ORIGIN_CHUNK_X_KEY));
        final Integer chunkZ = asIntegerValue(source == null ? null : source.get(ORIGIN_CHUNK_Z_KEY));
        if (townUuid == null || worldName == null || chunkX == null || chunkZ == null) {
            return null;
        }

        final Map<String, Object> data = new LinkedHashMap<>();
        data.put("plugin", plugin);
        data.put("town_uuid", townUuid);
        data.put(WORLD_NAME_KEY, worldName);
        data.put(CHUNK_X_KEY, chunkX);
        data.put(CHUNK_Z_KEY, chunkZ);
        return data;
    }

    private static @Nullable String resolveChunkWorldName(
        final @Nullable Map<String, Object> data,
        final @NotNull String key
    ) {
        return data == null || !(data.get(key) instanceof String rawWorldName) || rawWorldName.isBlank()
            ? null
            : rawWorldName;
    }

    private static @Nullable Integer asIntegerValue(final @Nullable Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue);
            } catch (final NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    protected final @NotNull ItemStack createSummaryItem(final @NotNull Context context) {
        final RTown town = this.resolveTown(context);
        if (town == null) {
            return this.createMissingItem(context.getPlayer());
        }

        return UnifiedBuilderFactory.item(this.resolveChunk(context) == null ? Material.SHIELD : Material.TARGET)
            .setName(this.i18n("summary.name", context.getPlayer()).build().component())
            .setLore(this.i18n("summary.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "town_name", town.getTownName(),
                    "scope", this.resolveScopeDisplay(context),
                    "security_state", this.resolveSecurityStateDisplay(town, context.getPlayer()),
                    "access_state", this.resolveAccessStateDisplay(context, town)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    protected final @NotNull ItemStack createScopeItem(final @NotNull Context context) {
        final RTown town = this.resolveTown(context);
        if (town == null) {
            return this.createMissingItem(context.getPlayer());
        }

        final RTownChunk scopedChunk = this.resolveChunk(context);
        final boolean chunkScoped = scopedChunk != null;
        return UnifiedBuilderFactory.item(scopedChunk == null ? Material.BLUE_BANNER : Material.ORANGE_BANNER)
            .setName(this.i18n("mode.name", context.getPlayer()).build().component())
            .setLore(this.i18n("mode.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "scope", this.resolveScopeDisplay(context),
                    "selection", chunkScoped
                        ? scopedChunk.getChunkType().name() + " " + scopedChunk.getX() + ", " + scopedChunk.getZ()
                        : this.sharedText("scope.town_global", context.getPlayer()),
                    "world_name", chunkScoped ? scopedChunk.getWorldName() : "-",
                    "chunk_type", chunkScoped ? scopedChunk.getChunkType().name() : "-",
                    "chunk_x", chunkScoped ? scopedChunk.getX() : "-",
                    "chunk_z", chunkScoped ? scopedChunk.getZ() : "-"
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    protected final @NotNull ItemStack createMissingItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    protected final @NotNull Component categoryNameComponent(
        final @NotNull TownProtectionCategory category,
        final @NotNull Player player
    ) {
        return new I18n.Builder("town_protection_shared.categories." + category.getTranslationKey(), player)
            .build()
            .component();
    }

    protected final @NotNull String categoryNameText(
        final @NotNull TownProtectionCategory category,
        final @NotNull Player player
    ) {
        return this.toPlainText(this.categoryNameComponent(category, player));
    }

    protected final @NotNull Component protectionNameComponent(
        final @NotNull TownProtections protection,
        final @NotNull Player player
    ) {
        return new I18n.Builder("town_protection_shared.protections." + protection.getTranslationKey(), player)
            .build()
            .component();
    }

    protected final @NotNull String protectionNameText(
        final @NotNull TownProtections protection,
        final @NotNull Player player
    ) {
        return this.toPlainText(this.protectionNameComponent(protection, player));
    }

    protected final @NotNull String resolveRoleDisplay(
        final @NotNull RTown town,
        final @Nullable String roleId,
        final @NotNull Player player
    ) {
        if (roleId == null) {
            return this.sharedText("states.inherit", player);
        }
        final TownRole role = town.findRoleById(roleId);
        return role == null ? roleId : role.getRoleName();
    }

    protected final @NotNull String resolveBinaryStateDisplay(final @Nullable String roleId, final @NotNull Player player) {
        if (roleId == null) {
            return this.sharedText("states.inherit", player);
        }
        return Objects.equals(TownProtections.normalizeBinaryRoleId(roleId), RTown.PUBLIC_ROLE_ID)
            ? this.sharedText("states.allowed", player)
            : this.sharedText("states.restricted", player);
    }

    protected final @NotNull String resolveScopeDisplay(final @NotNull Context context) {
        return this.sharedText(this.resolveChunk(context) == null ? "scope.town" : "scope.chunk", context.getPlayer());
    }

    protected final @NotNull String resolveSecurityStateDisplay(final @NotNull RTown town, final @NotNull Player player) {
        return this.sharedText(town.hasSecurityChunk() ? "security.unlocked" : "security.locked", player);
    }

    protected final @NotNull String resolveAccessStateDisplay(final @NotNull Context context, final @NotNull RTown town) {
        return this.sharedText(
            this.protectionEditingUnlocked(context, town) ? "states.editable" : "states.locked",
            context.getPlayer()
        );
    }

    protected final @NotNull String sharedText(final @NotNull String suffix, final @NotNull Player player) {
        return this.toPlainText(this.sharedComponent(suffix, player));
    }

    protected final @NotNull Component sharedComponent(final @NotNull String suffix, final @NotNull Player player) {
        return new I18n.Builder("town_protection_shared." + suffix, player).build().component();
    }

    private @NotNull String toPlainText(final @NotNull Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
