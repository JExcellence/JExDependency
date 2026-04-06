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
import com.raindropcentral.rdt.service.NexusAccessService;
import com.raindropcentral.rdt.service.TownRuntimeService;
import com.raindropcentral.rdt.utils.TownArchetype;
import com.raindropcentral.rdt.utils.TownOverviewAccessMode;
import com.raindropcentral.rdt.utils.TownPermissions;
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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Selector view for choosing one of the supported town archetypes.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownArchetypeView extends BaseView {

    private static final TownArchetype[] SELECTABLE_ARCHETYPES = TownArchetype.values();
    private static final char[] ARCHETYPE_LAYOUT_KEYS = {'a', 'b', 'c', 'd', 'e'};
    private static final String SESSION_TOKEN_KEY = "nexus_session";
    private static final String NEXUS_WORLD_KEY = "nexus_world";
    private static final String NEXUS_CHUNK_X_KEY = "nexus_chunk_x";
    private static final String NEXUS_CHUNK_Z_KEY = "nexus_chunk_z";

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");

    /**
     * Creates the archetype selector view.
     */
    public TownArchetypeView() {
        super(TownOverviewView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_archetype_ui";
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
            "  a b c  ",
            "   d e   ",
            "         ",
            "         ",
            "         "
        };
    }

    /**
     * Renders the archetype summary and selectable options.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RTown town = this.resolveTown(render);
        if (town == null) {
            render.slot(22).renderWith(() -> this.createMissingTownItem(player));
            return;
        }

        render.layoutSlot('s', this.createSummaryItem(render, town));
        for (int index = 0; index < SELECTABLE_ARCHETYPES.length && index < ARCHETYPE_LAYOUT_KEYS.length; index++) {
            final TownArchetype archetype = SELECTABLE_ARCHETYPES[index];
            render.layoutSlot(ARCHETYPE_LAYOUT_KEYS[index], this.createArchetypeItem(render, town, archetype))
                .onClick(clickContext -> this.handleArchetypeSelection(clickContext, town, archetype));
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

    private void handleArchetypeSelection(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RTown town,
        final @NotNull TownArchetype archetype
    ) {
        if (!this.isNexusGovernanceUnlocked(clickContext, town)) {
            this.sendReturnToNexus(clickContext.getPlayer());
            this.openOverviewView(clickContext, town.getTownUUID());
            return;
        }
        if (!this.viewerHasPermission(clickContext, town, TownPermissions.SET_ARCHETYPE)) {
            new I18n.Builder(this.getKey() + ".no_permission", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            this.openOverviewView(clickContext, town.getTownUUID());
            return;
        }

        if (town.getArchetype() == archetype) {
            new I18n.Builder(this.getKey() + ".already_selected", clickContext.getPlayer())
                .includePrefix()
                .withPlaceholder("archetype", archetype.getDisplayName())
                .build()
                .sendMessage();
            this.openOverviewView(clickContext, town.getTownUUID());
            return;
        }

        final TownRuntimeService townRuntimeService = this.plugin.get(clickContext).getTownRuntimeService();
        if (townRuntimeService == null) {
            new I18n.Builder(this.getKey() + ".selection_failed", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            clickContext.update();
            return;
        }

        if (townRuntimeService.setTownArchetype(town, archetype)) {
            new I18n.Builder(this.getKey() + ".selected", clickContext.getPlayer())
                .includePrefix()
                .withPlaceholder("archetype", archetype.getDisplayName())
                .build()
                .sendMessage();
            this.openOverviewView(clickContext, town.getTownUUID());
            return;
        }

        final long remainingCooldownMillis = townRuntimeService.getRemainingTownArchetypeChangeCooldownMillis(town);
        final I18n.Builder messageBuilder = new I18n.Builder(
            this.getKey() + '.' + (remainingCooldownMillis > 0L ? "cooldown" : "selection_failed"),
            clickContext.getPlayer()
        ).includePrefix();
        if (remainingCooldownMillis > 0L) {
            messageBuilder.withPlaceholder("remaining", TownOverviewView.formatDurationMillis(remainingCooldownMillis));
        }
        messageBuilder.build().sendMessage();
        clickContext.update();
    }

    private void openOverviewView(final @NotNull SlotClickContext clickContext, final @NotNull UUID resolvedTownUuid) {
        clickContext.openForPlayer(TownOverviewView.class, this.reopenData(clickContext, resolvedTownUuid));
    }

    private @Nullable RTown resolveTown(final @NotNull Context context) {
        final RDT rdt = this.plugin.get(context);
        if (rdt.getTownRuntimeService() == null) {
            return null;
        }

        final UUID targetTownUuid = this.townUuid.get(context);
        if (targetTownUuid != null) {
            return rdt.getTownRuntimeService().getTown(targetTownUuid);
        }
        return rdt.getTownRuntimeService().getTownFor(context.getPlayer().getUniqueId());
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

    private @NotNull Map<String, Object> reopenData(final @NotNull Context context, final @NotNull UUID resolvedTownUuid) {
        final Map<String, Object> data = new LinkedHashMap<>();
        final Map<String, Object> existingData = this.extractData(context);
        if (existingData != null) {
            data.putAll(existingData);
        }
        data.put("plugin", this.plugin.get(context));
        data.put("town_uuid", resolvedTownUuid);
        data.put("access_mode", this.resolveAccessMode(context));
        return data;
    }

    private @NotNull TownOverviewAccessMode resolveAccessMode(final @NotNull Context context) {
        final Map<String, Object> data = this.extractData(context);
        if (data == null) {
            return TownOverviewAccessMode.REMOTE;
        }

        final Object rawAccessMode = data.get("access_mode");
        if (rawAccessMode instanceof TownOverviewAccessMode resolvedMode) {
            return resolvedMode;
        }
        if (rawAccessMode instanceof String serializedAccessMode) {
            try {
                return TownOverviewAccessMode.valueOf(serializedAccessMode.trim().toUpperCase(Locale.ROOT));
            } catch (final IllegalArgumentException ignored) {
                return TownOverviewAccessMode.REMOTE;
            }
        }
        return TownOverviewAccessMode.REMOTE;
    }

    private boolean viewerHasPermission(
        final @NotNull Context context,
        final @NotNull RTown town,
        final @NotNull TownPermissions permission
    ) {
        final TownRuntimeService townRuntimeService = this.plugin.get(context).getTownRuntimeService();
        final RDTPlayer playerData = townRuntimeService == null
            ? null
            : townRuntimeService.getPlayerData(context.getPlayer().getUniqueId());
        return playerData != null
            && Objects.equals(playerData.getTownUUID(), town.getTownUUID())
            && playerData.hasTownPermission(permission);
    }

    private boolean isNexusGovernanceUnlocked(final @NotNull Context context, final @NotNull RTown town) {
        if (this.resolveAccessMode(context) != TownOverviewAccessMode.NEXUS) {
            return false;
        }

        final RDT rdt = this.plugin.get(context);
        final NexusAccessService nexusAccessService = rdt.getNexusAccessService();
        if (nexusAccessService == null || !nexusAccessService.hasValidSession(context.getPlayer(), town)) {
            return false;
        }

        final NexusAccessService.NexusSession session = nexusAccessService.getSession(context.getPlayer().getUniqueId());
        return session != null
            && Objects.equals(session.townUuid(), town.getTownUUID())
            && Objects.equals(session.sessionToken(), this.resolveTrustedSessionToken(context))
            && Objects.equals(session.worldName(), this.resolveTrustedNexusWorld(context))
            && session.chunkX() == this.resolveTrustedNexusChunkX(context)
            && session.chunkZ() == this.resolveTrustedNexusChunkZ(context);
    }

    private void sendReturnToNexus(final @NotNull Player player) {
        new I18n.Builder("town_overview_ui.governance.return_to_nexus", player)
            .includePrefix()
            .build()
            .sendMessage();
    }

    private @Nullable UUID resolveTrustedSessionToken(final @NotNull Context context) {
        final Map<String, Object> data = this.extractData(context);
        if (data == null) {
            return null;
        }
        return this.asUuid(data.get(SESSION_TOKEN_KEY));
    }

    private @Nullable String resolveTrustedNexusWorld(final @NotNull Context context) {
        final Map<String, Object> data = this.extractData(context);
        return data == null || !(data.get(NEXUS_WORLD_KEY) instanceof String worldName) ? null : worldName;
    }

    private int resolveTrustedNexusChunkX(final @NotNull Context context) {
        final Map<String, Object> data = this.extractData(context);
        return this.asInteger(data == null ? null : data.get(NEXUS_CHUNK_X_KEY));
    }

    private int resolveTrustedNexusChunkZ(final @NotNull Context context) {
        final Map<String, Object> data = this.extractData(context);
        return this.asInteger(data == null ? null : data.get(NEXUS_CHUNK_Z_KEY));
    }

    private @Nullable UUID asUuid(final @Nullable Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String rawUuid) {
            try {
                return UUID.fromString(rawUuid);
            } catch (final IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private int asInteger(final @Nullable Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue);
            } catch (final NumberFormatException ignored) {
                return Integer.MIN_VALUE;
            }
        }
        return Integer.MIN_VALUE;
    }

    private @NotNull ItemStack createSummaryItem(final @NotNull Context context, final @NotNull RTown town) {
        final TownArchetype currentArchetype = town.getArchetype();
        return UnifiedBuilderFactory.item(Material.NETHER_STAR)
            .setName(this.i18n("summary.name", context.getPlayer()).build().component())
            .setLore(this.i18n("summary.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "town_name", town.getTownName(),
                    "archetype", currentArchetype == null ? "Unassigned" : currentArchetype.getDisplayName()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createArchetypeItem(
        final @NotNull Context context,
        final @NotNull RTown town,
        final @NotNull TownArchetype archetype
    ) {
        final boolean selected = town.getArchetype() == archetype;
        final String loreKey = selected ? "entry.selected_lore" : "entry.available_lore";
        return UnifiedBuilderFactory.item(this.resolveArchetypeMaterial(archetype))
            .setName(this.i18n("entry.name", context.getPlayer())
                .withPlaceholder("archetype", archetype.getDisplayName())
                .build()
                .component())
            .setLore(this.i18n(loreKey, context.getPlayer())
                .withPlaceholder("archetype", archetype.getDisplayName())
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull Material resolveArchetypeMaterial(final @NotNull TownArchetype archetype) {
        return switch (archetype) {
            case CAPITALIST -> Material.EMERALD;
            case COMMUNIST -> Material.REDSTONE_BLOCK;
            case SOCIALIST -> Material.COPPER_INGOT;
            case THEOCRACY -> Material.ENCHANTED_BOOK;
            case MONARCHY -> Material.GOLDEN_HELMET;
        };
    }

    private @NotNull ItemStack createMissingTownItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
