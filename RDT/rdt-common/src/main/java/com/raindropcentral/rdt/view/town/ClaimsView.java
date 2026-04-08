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
import com.raindropcentral.rdt.items.ChunkBlock;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rplatform.utility.heads.view.Down;
import com.raindropcentral.rplatform.utility.heads.view.Next;
import com.raindropcentral.rplatform.utility.heads.view.Previous;
import com.raindropcentral.rplatform.utility.heads.view.Return;
import com.raindropcentral.rplatform.utility.heads.view.Up;
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
 * Claim map view for GUI-driven chunk expansion.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ClaimsView extends BaseView {

    private static final int GRID_COLUMNS = 7;
    private static final int GRID_ROWS = 4;
    private static final int GRID_FIRST_SLOT = 10;
    private static final int GRID_X_OFFSET_START = -3;
    private static final int GRID_Z_OFFSET_START = -1;

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");
    private final State<Integer> centerX = initialState("center_x");
    private final State<Integer> centerZ = initialState("center_z");

    /**
     * Creates the claims view.
     */
    public ClaimsView() {
        super(TownOverviewView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_claims_ui";
    }

    /**
     * Returns the menu layout.
     *
     * @return layout rows
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    n   i",
            "         ",
            "w       e",
            "         ",
            "         ",
            "b   s   c"
        };
    }

    /**
     * Renders the claim grid and navigation controls.
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

        final int resolvedCenterX = this.resolveCenterX(render);
        final int resolvedCenterZ = this.resolveCenterZ(render);
        render.layoutSlot('i', this.createSummaryItem(render, town, resolvedCenterX, resolvedCenterZ));
        render.layoutSlot('n', this.createNavigationItem(player, "north"))
            .onClick(clickContext -> this.navigate(clickContext, 0, -1));
        render.layoutSlot('w', this.createNavigationItem(player, "west"))
            .onClick(clickContext -> this.navigate(clickContext, -1, 0));
        render.layoutSlot('e', this.createNavigationItem(player, "east"))
            .onClick(clickContext -> this.navigate(clickContext, 1, 0));
        render.layoutSlot('s', this.createNavigationItem(player, "south"))
            .onClick(clickContext -> this.navigate(clickContext, 0, 1));
        render.layoutSlot('b', this.createReturnItem(player))
            .onClick(SlotClickContext::back);
        render.layoutSlot('c', this.createRecenterItem(player))
            .onClick(this::recenterOnPlayer);

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int column = 0; column < GRID_COLUMNS; column++) {
                final int targetChunkX = resolvedCenterX + GRID_X_OFFSET_START + column;
                final int targetChunkZ = resolvedCenterZ + GRID_Z_OFFSET_START + row;
                final int slot = GRID_FIRST_SLOT + (row * 9) + column;
                render.slot(slot).renderWith(() -> this.createChunkItem(render, town, targetChunkX, targetChunkZ))
                    .onClick(clickContext -> this.handleChunkClick(clickContext, town, targetChunkX, targetChunkZ));
            }
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

    private void navigate(final @NotNull SlotClickContext clickContext, final int deltaX, final int deltaZ) {
        clickContext.openForPlayer(
            ClaimsView.class,
            Map.of(
                "plugin", this.plugin.get(clickContext),
                "town_uuid", this.townUuid.get(clickContext),
                "center_x", this.resolveCenterX(clickContext) + deltaX,
                "center_z", this.resolveCenterZ(clickContext) + deltaZ
            )
        );
    }

    private void recenterOnPlayer(final @NotNull SlotClickContext clickContext) {
        clickContext.openForPlayer(
            ClaimsView.class,
            Map.of(
                "plugin", this.plugin.get(clickContext),
                "town_uuid", this.townUuid.get(clickContext),
                "center_x", clickContext.getPlayer().getLocation().getChunk().getX(),
                "center_z", clickContext.getPlayer().getLocation().getChunk().getZ()
            )
        );
    }

    private void handleChunkClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RTown town,
        final int chunkX,
        final int chunkZ
    ) {
        final String worldName = clickContext.getPlayer().getWorld().getName();
        final RTownChunk existingChunk = town.findChunk(worldName, chunkX, chunkZ);
        if (existingChunk != null) {
            clickContext.openForPlayer(
                TownChunkView.class,
                Map.of(
                    "plugin", this.plugin.get(clickContext),
                    "town_uuid", town.getTownUUID(),
                    "world_name", worldName,
                    "chunk_x", chunkX,
                    "chunk_z", chunkZ
                )
            );
            return;
        }

        final boolean canClaim = this.plugin.get(clickContext).getTownRuntimeService() != null
            && this.plugin.get(clickContext).getTownRuntimeService().isChunkClaimable(town, worldName, chunkX, chunkZ);
        if (!canClaim) {
            new I18n.Builder(this.getKey() + ".claim.adjacent_only", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        if (!this.plugin.get(clickContext).getTownRuntimeService().hasTownPermission(clickContext.getPlayer(), TownPermissions.CLAIM_CHUNK)) {
            new I18n.Builder(this.getKey() + ".claim.no_permission", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final ItemStack chunkBlock = ChunkBlock.getChunkBlockItem(
            this.plugin.get(clickContext),
            clickContext.getPlayer(),
            town.getTownUUID(),
            town.getMayorUUID(),
            worldName,
            chunkX,
            chunkZ
        );
        clickContext.getPlayer().getInventory().addItem(chunkBlock)
            .values()
            .forEach(overflow -> clickContext.getPlayer().getWorld().dropItemNaturally(clickContext.getPlayer().getLocation(), overflow));
        clickContext.closeForPlayer();
        new I18n.Builder(this.getKey() + ".claim.granted", clickContext.getPlayer())
            .includePrefix()
            .withPlaceholders(Map.of("chunk_x", chunkX, "chunk_z", chunkZ))
            .build()
            .sendMessage();
        new I18n.Builder(this.getKey() + ".claim.place_hint", clickContext.getPlayer())
            .includePrefix()
            .withPlaceholders(Map.of("chunk_x", chunkX, "chunk_z", chunkZ))
            .build()
            .sendMessage();
    }

    private @Nullable RTown resolveTown(final @NotNull Context context) {
        final UUID resolvedTownUuid = this.townUuid.get(context);
        return resolvedTownUuid == null || this.plugin.get(context).getTownRuntimeService() == null
            ? null
            : this.plugin.get(context).getTownRuntimeService().getTown(resolvedTownUuid);
    }

    private int resolveCenterX(final @NotNull Context context) {
        final Integer storedX = this.centerX.get(context);
        return storedX == null ? context.getPlayer().getLocation().getChunk().getX() : storedX;
    }

    private int resolveCenterZ(final @NotNull Context context) {
        final Integer storedZ = this.centerZ.get(context);
        return storedZ == null ? context.getPlayer().getLocation().getChunk().getZ() : storedZ;
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Context context,
        final @NotNull RTown town,
        final int centerChunkX,
        final int centerChunkZ
    ) {
        return UnifiedBuilderFactory.item(Material.FILLED_MAP)
            .setName(this.i18n("summary.name", context.getPlayer()).build().component())
            .setLore(this.i18n("summary.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "town_name", town.getTownName(),
                    "chunk_count", town.getChunks().size(),
                    "center_x", centerChunkX,
                    "center_z", centerChunkZ
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createNavigationItem(final @NotNull Player player, final @NotNull String direction) {
        return UnifiedBuilderFactory.item(this.resolveNavigationHead(player, direction))
            .setName(this.i18n("navigation." + direction + ".name", player).build().component())
            .setLore(this.i18n("navigation." + direction + ".lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack resolveNavigationHead(final @NotNull Player player, final @NotNull String direction) {
        return switch (direction) {
            case "north" -> new Up().getHead(player);
            case "south" -> new Down().getHead(player);
            case "east" -> new Next().getHead(player);
            case "west" -> new Previous().getHead(player);
            default -> new ItemStack(Material.ARROW);
        };
    }

    private @NotNull ItemStack createReturnItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(new Return().getHead(player))
            .setName(this.i18n("return.name", player).build().component())
            .setLore(this.i18n("return.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createRecenterItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.COMPASS)
            .setName(this.i18n("recenter.name", player).build().component())
            .setLore(this.i18n("recenter.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createChunkItem(
        final @NotNull Context context,
        final @NotNull RTown town,
        final int chunkX,
        final int chunkZ
    ) {
        final String worldName = context.getPlayer().getWorld().getName();
        final RTownChunk existingChunk = town.findChunk(worldName, chunkX, chunkZ);
        if (existingChunk != null) {
            return UnifiedBuilderFactory.item(Material.GREEN_STAINED_GLASS_PANE)
                .setName(this.i18n("chunk.claimed.name", context.getPlayer()).build().component())
                .setLore(this.i18n("chunk.claimed.lore", context.getPlayer())
                    .withPlaceholders(Map.of(
                        "chunk_x", chunkX,
                        "chunk_z", chunkZ,
                        "chunk_type", existingChunk.getChunkType().name()
                    ))
                    .build()
                    .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
        }

        final boolean claimable = this.plugin.get(context).getTownRuntimeService() != null
            && this.plugin.get(context).getTownRuntimeService().isChunkClaimable(town, worldName, chunkX, chunkZ);
        final Material material = claimable ? Material.YELLOW_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        final String key = claimable ? "chunk.claimable" : "chunk.blocked";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n(key + ".name", context.getPlayer()).build().component())
            .setLore(this.i18n(key + ".lore", context.getPlayer())
                .withPlaceholders(Map.of("chunk_x", chunkX, "chunk_z", chunkZ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMissingTownItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
