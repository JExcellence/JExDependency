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
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated selector for switching the active protection editing scope between town-global and
 * claimed chunk targets.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownProtectionScopeView extends APaginatedView<TownProtectionScopeView.ScopeOption> {

    private static final String WORLD_NAME_KEY = "world_name";
    private static final String CHUNK_X_KEY = "chunk_x";
    private static final String CHUNK_Z_KEY = "chunk_z";

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");

    /**
     * Creates the protection-scope selector.
     */
    public TownProtectionScopeView() {
        super(TownProtectionsView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_protection_scope_ui";
    }

    @Override
    protected String[] getLayout() {
        return super.getLayout();
    }

    /**
     * Loads the available scope options for the owning town.
     *
     * @param context current view context
     * @return async scope-option list
     */
    @Override
    protected @NotNull CompletableFuture<List<ScopeOption>> getAsyncPaginationSource(final @NotNull Context context) {
        final RTown town = this.resolveTown(context);
        if (town == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        final List<RTownChunk> claimedChunks = new ArrayList<>(town.getChunks());
        claimedChunks.sort(Comparator.comparing(RTownChunk::getWorldName, String.CASE_INSENSITIVE_ORDER)
            .thenComparingInt(RTownChunk::getX)
            .thenComparingInt(RTownChunk::getZ));

        final List<ScopeOption> scopes = new ArrayList<>();
        scopes.add(ScopeOption.global());
        for (final RTownChunk townChunk : claimedChunks) {
            scopes.add(ScopeOption.chunk(townChunk));
        }
        return CompletableFuture.completedFuture(List.copyOf(scopes));
    }

    /**
     * Renders one scope selector entry.
     *
     * @param context current context
     * @param builder item builder
     * @param index entry index
     * @param entry scope entry
     */
    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull ScopeOption entry
    ) {
        builder.withItem(this.createScopeItem(context, entry))
            .onClick(clickContext -> this.handleScopeSelection(clickContext, entry));
    }

    /**
     * Renders the summary or fallback content for the scope selector.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RTown town = this.resolveTown(render);
        if (town == null) {
            render.slot(22).renderWith(() -> this.createMissingItem(player));
            return;
        }
        render.slot(4).renderWith(() -> this.createSummaryItem(render, town));
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

    private void handleScopeSelection(
        final @NotNull SlotClickContext clickContext,
        final @NotNull ScopeOption scopeOption
    ) {
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put("plugin", this.plugin.get(clickContext));
        data.put("town_uuid", this.townUuid.get(clickContext));
        if (scopeOption.chunkScoped()) {
            data.put("world_name", scopeOption.worldName());
            data.put("chunk_x", scopeOption.chunkX());
            data.put("chunk_z", scopeOption.chunkZ());
        }
        clickContext.openForPlayer(TownProtectionsView.class, data);
    }

    private @Nullable RTown resolveTown(final @NotNull Context context) {
        final UUID resolvedTownUuid = this.townUuid.get(context);
        return resolvedTownUuid == null || this.plugin.get(context).getTownRuntimeService() == null
            ? null
            : this.plugin.get(context).getTownRuntimeService().getTown(resolvedTownUuid);
    }

    private @NotNull ItemStack createSummaryItem(final @NotNull Context context, final @NotNull RTown town) {
        return UnifiedBuilderFactory.item(Material.COMPASS)
            .setName(this.i18n("summary.name", context.getPlayer()).build().component())
            .setLore(this.i18n("summary.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "town_name", town.getTownName(),
                    "current_scope", this.resolveCurrentScopeLabel(context),
                    "scope_count", town.getChunks().size() + 1
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createScopeItem(final @NotNull Context context, final @NotNull ScopeOption scopeOption) {
        final boolean selected = this.isSelectedScope(context, scopeOption);
        final String selectedKey = selected ? "selected_lore" : "available_lore";
        if (!scopeOption.chunkScoped()) {
            return UnifiedBuilderFactory.item(selected ? Material.SHIELD : Material.WHITE_BANNER)
                .setName(this.i18n("entry.global.name", context.getPlayer()).build().component())
                .setLore(this.i18n("entry.global." + selectedKey, context.getPlayer()).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
        }

        final ChunkType chunkType = Objects.requireNonNull(scopeOption.chunkType(), "chunkType");
        return UnifiedBuilderFactory.item(selected
                ? Material.TARGET
                : this.plugin.get(context).getDefaultConfig().getChunkTypeIconMaterial(chunkType))
            .setName(this.i18n("entry.chunk.name", context.getPlayer())
                .withPlaceholders(Map.of(
                    "world_name", scopeOption.worldName(),
                    "chunk_x", scopeOption.chunkX(),
                    "chunk_z", scopeOption.chunkZ()
                ))
                .build()
                .component())
            .setLore(this.i18n("entry.chunk." + selectedKey, context.getPlayer())
                .withPlaceholders(Map.of(
                    "world_name", scopeOption.worldName(),
                    "chunk_x", scopeOption.chunkX(),
                    "chunk_z", scopeOption.chunkZ(),
                    "chunk_type", chunkType.name()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private boolean isSelectedScope(final @NotNull Context context, final @NotNull ScopeOption scopeOption) {
        final String selectedWorldName = this.resolveScopedWorldName(context);
        final Integer selectedChunkX = this.resolveScopedChunkX(context);
        final Integer selectedChunkZ = this.resolveScopedChunkZ(context);
        if (!scopeOption.chunkScoped()) {
            return selectedWorldName == null || selectedChunkX == null || selectedChunkZ == null;
        }
        return Objects.equals(selectedWorldName, scopeOption.worldName())
            && Objects.equals(selectedChunkX, scopeOption.chunkX())
            && Objects.equals(selectedChunkZ, scopeOption.chunkZ());
    }

    private @NotNull String resolveCurrentScopeLabel(final @NotNull Context context) {
        final String selectedWorldName = this.resolveScopedWorldName(context);
        final Integer selectedChunkX = this.resolveScopedChunkX(context);
        final Integer selectedChunkZ = this.resolveScopedChunkZ(context);
        if (selectedWorldName == null || selectedChunkX == null || selectedChunkZ == null) {
            return "Town Global";
        }
        return selectedWorldName + ' ' + selectedChunkX + ", " + selectedChunkZ;
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

    private @NotNull ItemStack createMissingItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    /**
     * Immutable descriptor for one selectable protection scope.
     *
     * @param worldName claimed world name, or {@code null} for the town-global scope
     * @param chunkX claimed chunk X, or {@code null} for the town-global scope
     * @param chunkZ claimed chunk Z, or {@code null} for the town-global scope
     * @param chunkType claimed chunk type, or {@code null} for the town-global scope
     */
    public record ScopeOption(
        @Nullable String worldName,
        @Nullable Integer chunkX,
        @Nullable Integer chunkZ,
        @Nullable ChunkType chunkType
    ) {

        private static @NotNull ScopeOption global() {
            return new ScopeOption(null, null, null, null);
        }

        private static @NotNull ScopeOption chunk(final @NotNull RTownChunk townChunk) {
            return new ScopeOption(
                townChunk.getWorldName(),
                townChunk.getX(),
                townChunk.getZ(),
                townChunk.getChunkType()
            );
        }

        private boolean chunkScoped() {
            return this.worldName != null && this.chunkX != null && this.chunkZ != null;
        }
    }
}
