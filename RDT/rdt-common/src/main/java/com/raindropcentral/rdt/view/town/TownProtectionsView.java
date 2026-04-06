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
import com.raindropcentral.rdt.database.entity.TownRole;
import com.raindropcentral.rdt.utils.TownProtections;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Protection editor for town-global or chunk-specific protection thresholds.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownProtectionsView extends BaseView {

    private static final int[] PROTECTION_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28};
    private static final String WORLD_NAME_KEY = "world_name";
    private static final String CHUNK_X_KEY = "chunk_x";
    private static final String CHUNK_Z_KEY = "chunk_z";

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");

    /**
     * Creates the protections view.
     */
    public TownProtectionsView() {
        super(TownOverviewView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_protections_ui";
    }

    /**
     * Returns the menu layout.
     *
     * @return layout rows
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "         ",
            "         ",
            "         ",
            "    m    ",
            "         "
        };
    }

    /**
     * Renders summary and protection toggles.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RTown town = this.resolveTown(render);
        if (town == null) {
            render.slot(22).renderWith(() -> this.createMissingItem(player));
            return;
        }

        render.slot(4).renderWith(() -> this.createSummaryItem(render));
        render.slot(40).renderWith(() -> this.createModeItem(render))
            .onClick(this::handleModeClick);
        final TownProtections[] protections = TownProtections.values();
        for (int index = 0; index < protections.length && index < PROTECTION_SLOTS.length; index++) {
            final TownProtections protection = protections[index];
            render.slot(PROTECTION_SLOTS[index]).renderWith(() -> this.createProtectionItem(render, protection))
                .onClick(clickContext -> this.handleProtectionClick(clickContext, protection));
        }
    }

    /**
     * Cancels default inventory interaction for the menu.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void handleModeClick(final @NotNull SlotClickContext clickContext) {
        final RTown town = this.resolveTown(clickContext);
        if (town == null) {
            this.sendUpdateFailedMessage(clickContext);
            return;
        }
        if (!town.hasSecurityChunk()) {
            new I18n.Builder(this.getKey() + ".locked.security_required", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        clickContext.openForPlayer(TownProtectionScopeView.class, this.createScopeSelectorData(clickContext));
    }

    private void handleProtectionClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull TownProtections protection
    ) {
        final RTown town = this.resolveTown(clickContext);
        if (town == null) {
            this.sendUpdateFailedMessage(clickContext);
            return;
        }
        if (!town.hasSecurityChunk()) {
            new I18n.Builder(this.getKey() + ".locked.security_required", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final boolean chunkScoped = this.hasScopedChunkTarget(clickContext);
        final List<String> roleOrder = new ArrayList<>(this.plugin.get(clickContext).getTownRuntimeService().getProtectionRoleOrder(town));
        if (!chunkScoped) {
            final String nextRoleId = this.nextRoleId(roleOrder, town.getProtectionRoleId(protection), false);
            final boolean updated = this.plugin.get(clickContext).getTownRuntimeService().setTownProtectionRoleId(
                town,
                protection,
                nextRoleId
            );
            if (!updated) {
                this.sendUpdateFailedMessage(clickContext);
                return;
            }
            new I18n.Builder(this.getKey() + ".updated", clickContext.getPlayer())
                .includePrefix()
                .withPlaceholders(Map.of(
                    "protection", protection.name(),
                    "role", this.resolveRoleDisplay(town, nextRoleId)
                ))
                .build()
                .sendMessage();
            clickContext.update();
            return;
        }

        final RTownChunk scopedChunk = this.resolveChunk(clickContext);
        if (scopedChunk == null) {
            this.sendUpdateFailedMessage(clickContext);
            return;
        }
        final String nextRoleId = this.nextRoleId(roleOrder, scopedChunk.getProtectionRoleId(protection), true);
        final boolean updated = this.plugin.get(clickContext).getTownRuntimeService().setChunkProtectionRoleId(
            scopedChunk,
            protection,
            nextRoleId
        );
        if (!updated) {
            this.sendUpdateFailedMessage(clickContext);
            return;
        }
        new I18n.Builder(this.getKey() + ".updated", clickContext.getPlayer())
            .includePrefix()
            .withPlaceholders(Map.of(
                "protection", protection.name(),
                "role", nextRoleId == null ? "Inherit" : this.resolveRoleDisplay(town, nextRoleId)
            ))
            .build()
            .sendMessage();
        clickContext.update();
    }

    private boolean hasScopedChunkTarget(final @NotNull Context context) {
        return this.resolveScopedWorldName(context) != null
            && this.resolveScopedChunkX(context) != null
            && this.resolveScopedChunkZ(context) != null;
    }

    private @Nullable RTown resolveTown(final @NotNull Context context) {
        final UUID resolvedTownUuid = this.townUuid.get(context);
        return resolvedTownUuid == null || this.plugin.get(context).getTownRuntimeService() == null
            ? null
            : this.plugin.get(context).getTownRuntimeService().getTown(resolvedTownUuid);
    }

    private @Nullable RTownChunk resolveChunk(final @NotNull Context context) {
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
        return this.plugin.get(context).getTownRuntimeService().getTownChunk(
            resolvedTownUuid,
            resolvedWorldName,
            resolvedChunkX,
            resolvedChunkZ
        );
    }

    private @Nullable String nextRoleId(
        final @NotNull List<String> roleOrder,
        final @Nullable String currentRoleId,
        final boolean allowInherit
    ) {
        final List<String> cycle = new ArrayList<>();
        if (allowInherit) {
            cycle.add(null);
        }
        cycle.addAll(roleOrder);
        int currentIndex = cycle.indexOf(currentRoleId);
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        return cycle.get((currentIndex + 1) % cycle.size());
    }

    private @NotNull String resolveRoleDisplay(final @NotNull RTown town, final @Nullable String roleId) {
        if (roleId == null) {
            return "Inherit";
        }
        final TownRole role = town.findRoleById(roleId);
        return role == null ? roleId : role.getRoleName();
    }

    private void sendUpdateFailedMessage(final @NotNull SlotClickContext clickContext) {
        new I18n.Builder(this.getKey() + ".update_failed", clickContext.getPlayer())
            .includePrefix()
            .build()
            .sendMessage();
    }

    private @NotNull Map<String, Object> createScopeSelectorData(final @NotNull Context context) {
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put("plugin", this.plugin.get(context));
        data.put("town_uuid", this.townUuid.get(context));
        if (this.hasScopedChunkTarget(context)) {
            data.put(WORLD_NAME_KEY, this.resolveScopedWorldName(context));
            data.put(CHUNK_X_KEY, this.resolveScopedChunkX(context));
            data.put(CHUNK_Z_KEY, this.resolveScopedChunkZ(context));
        }
        return data;
    }

    private @Nullable Map<String, Object> extractData(final @NotNull Context context) {
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

    private @Nullable String resolveScopedWorldName(final @NotNull Context context) {
        final Map<String, Object> data = this.extractData(context);
        return data == null || !(data.get(WORLD_NAME_KEY) instanceof String rawWorldName) || rawWorldName.isBlank()
            ? null
            : rawWorldName;
    }

    private @Nullable Integer resolveScopedChunkX(final @NotNull Context context) {
        final Map<String, Object> data = this.extractData(context);
        return this.asInteger(data == null ? null : data.get(CHUNK_X_KEY));
    }

    private @Nullable Integer resolveScopedChunkZ(final @NotNull Context context) {
        final Map<String, Object> data = this.extractData(context);
        return this.asInteger(data == null ? null : data.get(CHUNK_Z_KEY));
    }

    private @Nullable Integer asInteger(final @Nullable Object value) {
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

    private @NotNull ItemStack createSummaryItem(final @NotNull Context context) {
        final RTown town = this.resolveTown(context);
        if (town == null) {
            return this.createMissingItem(context.getPlayer());
        }
        final RTownChunk scopedChunk = this.resolveChunk(context);
        final boolean chunkScoped = scopedChunk != null;
        return UnifiedBuilderFactory.item(chunkScoped ? Material.TARGET : Material.SHIELD)
            .setName(this.i18n("summary.name", context.getPlayer()).build().component())
            .setLore(this.i18n("summary.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "town_name", town.getTownName(),
                    "scope", chunkScoped ? "Chunk" : "Town",
                    "security_state", town.hasSecurityChunk() ? "Unlocked" : "Locked"
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createModeItem(final @NotNull Context context) {
        final RTown town = this.resolveTown(context);
        if (town == null) {
            return this.createMissingItem(context.getPlayer());
        }
        final RTownChunk scopedChunk = this.resolveChunk(context);
        final boolean chunkScoped = scopedChunk != null;
        return UnifiedBuilderFactory.item(chunkScoped ? Material.ORANGE_BANNER : Material.BLUE_BANNER)
            .setName(this.i18n("mode.name", context.getPlayer()).build().component())
            .setLore(this.i18n("mode.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "scope", chunkScoped ? "Chunk" : "Town",
                    "chunk_x", chunkScoped ? scopedChunk.getX() : "-",
                    "chunk_z", chunkScoped ? scopedChunk.getZ() : "-"
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createProtectionItem(
        final @NotNull Context context,
        final @NotNull TownProtections protection
    ) {
        final RTown town = this.resolveTown(context);
        if (town == null) {
            return this.createMissingItem(context.getPlayer());
        }
        final RTownChunk scopedChunk = this.resolveChunk(context);
        final boolean unlocked = town.hasSecurityChunk();
        final String currentRoleId = scopedChunk == null
            ? town.getProtectionRoleId(protection)
            : scopedChunk.getProtectionRoleId(protection);
        final boolean inherited = scopedChunk != null && !scopedChunk.overridesProtection(protection);
        final Material material = unlocked ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        final String roleDisplay = inherited
            ? "Inherit"
            : this.resolveRoleDisplay(town, currentRoleId);

        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("entry.name", context.getPlayer())
                .withPlaceholder("protection", protection.name())
                .build()
                .component())
            .setLore(this.i18n("entry.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "protection", protection.name(),
                    "role", roleDisplay,
                    "state", unlocked ? "Editable" : "Locked"
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMissingItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
