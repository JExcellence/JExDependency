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

import java.util.Map;
import java.util.UUID;

/**
 * Chunk detail view for one claimed chunk marker.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownChunkView extends BaseView {

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");
    private final State<String> worldName = initialState("world_name");
    private final State<Integer> chunkX = initialState("chunk_x");
    private final State<Integer> chunkZ = initialState("chunk_z");

    /**
     * Creates the chunk view.
     */
    public TownChunkView() {
        super(TownOverviewView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_chunk_ui";
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
            "   t p   ",
            "   i r   ",
            "         ",
            "         ",
            "         "
        };
    }

    /**
     * Renders the chunk summary and actions.
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
        render.layoutSlot('t', this.createTypeItem(render, townChunk))
            .onClick(clickContext -> this.handleTypeClick(clickContext, townChunk));
        render.layoutSlot('p', this.createProtectionsItem(render, townChunk))
            .onClick(clickContext -> this.handleProtectionsClick(clickContext, townChunk));
        render.layoutSlot('i', this.createInfoItem(render, townChunk));
        render.layoutSlot('r', this.createReturnItem(player))
            .onClick(clickContext -> clickContext.back());
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

    private void handleTypeClick(final @NotNull SlotClickContext clickContext, final @NotNull RTownChunk townChunk) {
        if (!this.plugin.get(clickContext).getTownRuntimeService().hasTownPermission(
            clickContext.getPlayer(),
            TownPermissions.CHANGE_CHUNK_TYPE
        )) {
            new I18n.Builder(this.getKey() + ".type.no_permission", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        clickContext.openForPlayer(
            TownChunkTypeView.class,
            Map.of(
                "plugin", this.plugin.get(clickContext),
                "town_uuid", townChunk.getTown().getTownUUID(),
                "world_name", townChunk.getWorldName(),
                "chunk_x", townChunk.getX(),
                "chunk_z", townChunk.getZ()
            )
        );
    }

    private void handleProtectionsClick(final @NotNull SlotClickContext clickContext, final @NotNull RTownChunk townChunk) {
        clickContext.openForPlayer(
            TownProtectionsView.class,
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
        return UnifiedBuilderFactory.item(Material.LODESTONE)
            .setName(this.i18n("summary.name", context.getPlayer()).build().component())
            .setLore(this.i18n("summary.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "chunk_x", townChunk.getX(),
                    "chunk_z", townChunk.getZ(),
                    "chunk_type", townChunk.getChunkType().name(),
                    "chunk_level", townChunk.getChunkLevel(),
                    "town_name", townChunk.getTown().getTownName()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createTypeItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final boolean canChange = this.plugin.get(context).getTownRuntimeService().hasTownPermission(
            context.getPlayer(),
            TownPermissions.CHANGE_CHUNK_TYPE
        );
        final Material material = canChange
            ? this.plugin.get(context).getDefaultConfig().getChunkTypeIconMaterial(townChunk.getChunkType())
            : Material.GRAY_DYE;
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("type.name", context.getPlayer()).build().component())
            .setLore(this.i18n((canChange ? "type" : "type.locked") + ".lore", context.getPlayer())
                .withPlaceholder("chunk_type", townChunk.getChunkType().name())
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createProtectionsItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final RTown town = townChunk.getTown();
        final boolean unlocked = town.hasSecurityChunk();
        return UnifiedBuilderFactory.item(unlocked ? Material.IRON_SWORD : Material.BARRIER)
            .setName(this.i18n("protections.name", context.getPlayer()).build().component())
            .setLore(this.i18n((unlocked ? "protections" : "protections.locked") + ".lore", context.getPlayer())
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createInfoItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        return UnifiedBuilderFactory.item(Material.BOOK)
            .setName(this.i18n("info.name", context.getPlayer()).build().component())
            .setLore(this.i18n("info.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "world_name", townChunk.getWorldName(),
                    "chunk_x", townChunk.getX(),
                    "chunk_z", townChunk.getZ(),
                    "chunk_level", townChunk.getChunkLevel()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createReturnItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.ARROW)
            .setName(this.i18n("return.name", player).build().component())
            .setLore(this.i18n("return.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMissingChunkItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
