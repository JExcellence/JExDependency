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
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.utils.ChunkType;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Selector view for choosing the specialization of one claimed town chunk.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownChunkTypeView extends BaseView {

    private static final ChunkType[] SELECTABLE_TYPES = {
        ChunkType.DEFAULT,
        ChunkType.FARM,
        ChunkType.SECURITY,
        ChunkType.OUTPOST,
        ChunkType.MEDIC,
        ChunkType.BANK,
        ChunkType.ARMORY
    };
    private static final char[] TYPE_LAYOUT_KEYS = {'a', 'b', 'c', 'd', 'e', 'f', 'g'};

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");
    private final State<String> worldName = initialState("world_name");
    private final State<Integer> chunkX = initialState("chunk_x");
    private final State<Integer> chunkZ = initialState("chunk_z");

    /**
     * Creates the chunk-type selector view.
     */
    public TownChunkTypeView() {
        super(TownChunkView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_chunk_type_ui";
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
            "  d e f  ",
            "    g    ",
            "         ",
            "         "
        };
    }

    /**
     * Renders the summary and selectable chunk types.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RTownChunk townChunk = this.resolveChunk(render);
        if (townChunk == null) {
            render.slot(22).renderWith(() -> this.createMissingChunkItem(player));
            return;
        }

        render.layoutSlot('s', this.createSummaryItem(render, townChunk));
        for (int index = 0; index < SELECTABLE_TYPES.length && index < TYPE_LAYOUT_KEYS.length; index++) {
            final ChunkType chunkType = SELECTABLE_TYPES[index];
            render.layoutSlot(TYPE_LAYOUT_KEYS[index], this.createTypeItem(render, townChunk, chunkType))
                .onClick(clickContext -> this.handleTypeSelection(clickContext, townChunk, chunkType));
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

    private void handleTypeSelection(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RTownChunk townChunk,
        final @NotNull ChunkType chunkType
    ) {
        if (townChunk.getChunkType() == ChunkType.FOB || !isSelectableType(chunkType)) {
            new I18n.Builder(this.getKey() + ".immutable", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            this.openChunkView(clickContext, townChunk);
            return;
        }
        if (!this.plugin.get(clickContext).getTownRuntimeService().hasTownPermission(
            clickContext.getPlayer(),
            TownPermissions.CHANGE_CHUNK_TYPE
        )) {
            new I18n.Builder("town_chunk_ui.type.no_permission", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        if (townChunk.getChunkType() == chunkType) {
            new I18n.Builder(this.getKey() + ".already_selected", clickContext.getPlayer())
                .includePrefix()
                .withPlaceholder("chunk_type", chunkType.name())
                .build()
                .sendMessage();
            this.openChunkView(clickContext, townChunk);
            return;
        }

        final var result = this.plugin.get(clickContext).getTownRuntimeService().setChunkType(
            clickContext.getPlayer(),
            townChunk,
            chunkType
        );
        final String messageKey = result.success() ? "selected" : "selection_failed";
        new I18n.Builder(this.getKey() + '.' + messageKey, clickContext.getPlayer())
            .includePrefix()
            .withPlaceholder("chunk_type", chunkType.name())
            .build()
            .sendMessage();
        if (result.success() && result.fuelTankGranted()) {
            new I18n.Builder(this.getKey() + ".fuel_tank_granted", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
        }
        if (result.success() && result.fuelTankRemoved()) {
            new I18n.Builder(
                this.getKey() + (result.droppedFuel() ? ".fuel_tank_removed_with_fuel" : ".fuel_tank_removed"),
                clickContext.getPlayer()
            )
                .includePrefix()
                .build()
                .sendMessage();
        }
        if (result.success() && chunkType == ChunkType.FARM) {
            new I18n.Builder(this.getKey() + ".farm_selected_hint", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
        }
        if (result.success() && chunkType == ChunkType.ARMORY) {
            new I18n.Builder(this.getKey() + ".armory_selected_hint", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
        }
        if (result.success() && result.seedBoxGranted()) {
            new I18n.Builder(this.getKey() + ".seed_box_granted", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
        }
        if (result.success() && result.seedBoxRemoved()) {
            new I18n.Builder(
                this.getKey() + (result.droppedSeeds() ? ".seed_box_removed_with_seeds" : ".seed_box_removed"),
                clickContext.getPlayer()
            )
                .includePrefix()
                .build()
                .sendMessage();
        }
        if (result.success() && result.salvageBlockGranted()) {
            new I18n.Builder(this.getKey() + ".salvage_block_granted", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
        }
        if (result.success() && result.salvageBlockRemoved()) {
            new I18n.Builder(this.getKey() + ".salvage_block_removed", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
        }
        if (result.success() && result.repairBlockGranted()) {
            new I18n.Builder(this.getKey() + ".repair_block_granted", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
        }
        if (result.success() && result.repairBlockRemoved()) {
            new I18n.Builder(this.getKey() + ".repair_block_removed", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
        }
        this.openChunkView(clickContext, townChunk);
    }

    private void openChunkView(final @NotNull SlotClickContext clickContext, final @NotNull RTownChunk townChunk) {
        clickContext.openForPlayer(
            TownChunkView.class,
            Map.of(
                "plugin", this.plugin.get(clickContext),
                "town_uuid", townChunk.getTown().getTownUUID(),
                "world_name", townChunk.getWorldName(),
                "chunk_x", townChunk.getX(),
                "chunk_z", townChunk.getZ()
            )
        );
    }

    private @Nullable RTownChunk resolveChunk(final @NotNull Context context) {
        final UUID resolvedTownUuid = this.townUuid.get(context);
        final String resolvedWorldName = this.worldName.get(context);
        final Integer resolvedChunkX = this.chunkX.get(context);
        final Integer resolvedChunkZ = this.chunkZ.get(context);
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

    private @NotNull ItemStack createSummaryItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        return UnifiedBuilderFactory.item(Material.COMPASS)
            .setName(this.i18n("summary.name", context.getPlayer()).build().component())
            .setLore(this.i18n("summary.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "town_name", townChunk.getTown().getTownName(),
                    "chunk_x", townChunk.getX(),
                    "chunk_z", townChunk.getZ(),
                    "chunk_type", townChunk.getChunkType().name()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createTypeItem(
        final @NotNull Context context,
        final @NotNull RTownChunk townChunk,
        final @NotNull ChunkType chunkType
    ) {
        final boolean selected = townChunk.getChunkType() == chunkType;
        final boolean resetsState = this.plugin.get(context).getDefaultConfig().isChunkTypeResetOnChange();
        final String loreKey = selected
            ? "entry.selected_lore"
            : resetsState
                ? "entry.available_reset_lore"
                : "entry.available_lore";
        final List<net.kyori.adventure.text.Component> lore = new ArrayList<>(this.i18n(loreKey, context.getPlayer())
            .withPlaceholder("chunk_type", chunkType.name())
            .build()
            .children());
        lore.addAll(this.i18n(resolveBenefitLoreKey(chunkType), context.getPlayer()).build().children());
        return UnifiedBuilderFactory.item(this.plugin.get(context).getChunkTypeDisplayMaterial(chunkType))
            .setName(this.i18n("entry.name", context.getPlayer())
                .withPlaceholder("chunk_type", chunkType.name())
                .build()
                .component())
            .setLore(lore)
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    static @NotNull String resolveBenefitLoreKey(final @NotNull ChunkType chunkType) {
        return "entry.benefits." + chunkType.name().toLowerCase(java.util.Locale.ROOT);
    }

    static boolean isSelectableType(final @NotNull ChunkType chunkType) {
        for (final ChunkType selectableType : SELECTABLE_TYPES) {
            if (selectableType == chunkType) {
                return true;
            }
        }
        return false;
    }

    private @NotNull ItemStack createMissingChunkItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
