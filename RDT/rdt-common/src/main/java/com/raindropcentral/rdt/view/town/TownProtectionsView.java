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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RChunk;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.TownRole;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rdt.utils.TownProtections;
import com.raindropcentral.rplatform.utility.heads.view.Return;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;

/**
 * Paginated role-assignment view for town and chunk protection rules.
 *
 * <p>When opened with {@code protection_scope = "town"}, changes apply to town-wide defaults.
 * When opened with {@code protection_scope = "chunk"}, changes apply only to the target chunk.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.1
 */
public final class TownProtectionsView extends APaginatedView<TownProtections> {

    private static final String SCOPE_TOWN = "town";
    private static final String SCOPE_CHUNK = "chunk";

    private final State<RDT> rdt = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");
    private final State<String> protectionScope = initialState("protection_scope");
    private final State<Integer> chunkX = initialState("chunk_x");
    private final State<Integer> chunkZ = initialState("chunk_z");
    private final State<Integer> blockX = initialState("block_x");
    private final State<Integer> blockY = initialState("block_y");
    private final State<Integer> blockZ = initialState("block_z");
    private final State<String> blockWorld = initialState("block_world");

    /**
     * Creates the protections view.
     */
    public TownProtectionsView() {
        super();
    }

    /**
     * Returns the i18n key namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_protections_ui";
    }

    /**
     * Resolves placeholders used in the title.
     *
     * @param openContext open context
     * @return title placeholders
     */
    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        final RTown town = this.resolveTown(openContext, openContext.getPlayer());
        final String scope = this.resolveProtectionScope(openContext);
        return Map.of(
                "town_name", town == null ? "Unknown" : town.getTownName(),
                "scope_label", this.resolveScopeLabel(openContext.getPlayer(), scope)
        );
    }

    /**
     * Cancels default inventory movement behavior.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    /**
     * Validates context and permission before rendering.
     *
     * @param render render context
     * @param player viewer
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        if (!this.verifyViewerAccess(render, player)) {
            player.closeInventory();
            return;
        }
        super.onFirstRender(render, player);
    }

    /**
     * Loads all protection entries for pagination.
     *
     * @param context view context
     * @return future protection list
     */
    @Override
    protected @NotNull CompletableFuture<List<TownProtections>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final List<TownProtections> protections = Arrays.stream(TownProtections.values())
                .sorted(Comparator.comparing(TownProtections::name))
                .toList();
        return CompletableFuture.completedFuture(protections);
    }

    /**
     * Renders a single protection entry.
     *
     * @param context context
     * @param builder builder
     * @param index index
     * @param entry protection entry
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull TownProtections entry
    ) {
        final Player viewer = context.getPlayer();
        final RTown town = this.resolveTown(context, viewer);
        if (town == null) {
            builder.withItem(
                    UnifiedBuilderFactory.item(Material.BARRIER)
                            .setName(this.i18n("protection.unavailable.name", viewer).build().component())
                            .setLore(this.i18n("protection.unavailable.lore", viewer).build().children())
                            .build()
            );
            return;
        }

        final @Nullable RChunk chunk = this.resolveTargetChunk(context, town);
        final String requiredRoleId = this.resolveRequiredRoleId(context, town, chunk, entry);
        final String scope = this.resolveProtectionScope(context);
        final String scopeLabel = this.resolveScopeLabel(viewer, scope);
        final String sourceState = this.resolveSourceStateLabel(context, viewer, chunk, entry);
        final Material roleMaterial = this.resolveRoleMaterial(requiredRoleId);

        builder.withItem(
                UnifiedBuilderFactory.item(roleMaterial)
                        .setName(this.i18n("protection.name", viewer)
                                .withPlaceholders(Map.of(
                                        "protection", entry.getProtectionKey(),
                                        "required_role", requiredRoleId
                                ))
                                .build()
                                .component())
                        .setLore(this.i18n("protection.lore", viewer)
                                .withPlaceholders(Map.of(
                                        "protection", entry.getProtectionKey(),
                                        "required_role", requiredRoleId,
                                        "scope_label", scopeLabel,
                                        "source_state", sourceState
                                ))
                                .build()
                                .children())
                        .build()
        ).onClick(click -> this.handleProtectionClick(click, entry));
    }

    /**
     * Renders fixed controls for this paginated view.
     *
     * @param render render context
     * @param player viewer
     */
    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
        render.slot(6, 1)
                .renderWith(() -> UnifiedBuilderFactory.item(new Return().getHead(player))
                        .setName(this.i18n("back.name", player).build().component())
                        .setLore(this.i18n("back.lore", player).build().children())
                        .build())
                .onClick(this::handleBackClick);
    }

    private void handleBackClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);

        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        final RTown town = this.resolveTown(click, player);
        if (plugin == null || town == null) {
            click.closeForPlayer();
            return;
        }

        if (this.isChunkScope(click)) {
            click.openForPlayer(TownChunkView.class, this.buildChunkBackData(click, plugin, town));
            return;
        }
        click.openForPlayer(
                TownInfoView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier()
                )
        );
    }

    private void handleProtectionClick(
            final @NotNull SlotClickContext click,
            final @NotNull TownProtections protection
    ) {
        click.setCancelled(true);

        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null || plugin.getTownRepository() == null) {
            this.i18n("error.system_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, player);
        if (town == null) {
            this.i18n("error.town_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.TOWN_PROTECTIONS)) {
            this.i18n("error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.TOWN_PROTECTIONS.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        final @Nullable RChunk targetChunk = this.resolveTargetChunk(click, town);
        if (this.isChunkScope(click) && targetChunk == null) {
            this.i18n("error.chunk_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final List<String> roleCycle = this.resolveRoleCycle(town);
        if (roleCycle.isEmpty()) {
            this.i18n("error.no_roles", player).includePrefix().build().sendMessage();
            return;
        }

        final String currentRole = this.resolveRequiredRoleId(click, town, targetChunk, protection);
        final int currentIndex = roleCycle.indexOf(currentRole);
        final int nextIndex = currentIndex < 0 ? 0 : (currentIndex + 1) % roleCycle.size();
        final String nextRole = roleCycle.get(nextIndex);

        if (this.isChunkScope(click) && targetChunk != null) {
            targetChunk.setProtectionOverrideRoleId(protection, nextRole);
        } else {
            town.setProtectionRoleId(protection, nextRole);
        }

        plugin.getTownRepository().update(town);

        this.i18n("message.updated", player)
                .includePrefix()
                .withPlaceholders(Map.of(
                        "protection", protection.getProtectionKey(),
                        "required_role", nextRole,
                        "scope_label", this.resolveScopeLabel(player, this.resolveProtectionScope(click))
                ))
                .build()
                .sendMessage();

        click.openForPlayer(TownProtectionsView.class, this.buildOpenData(click, plugin, town));
    }

    private boolean verifyViewerAccess(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RTown town = this.resolveTown(context, player);
        if (town == null) {
            this.i18n("error.town_unavailable", player).includePrefix().build().sendMessage();
            return false;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(context, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.TOWN_PROTECTIONS)) {
            this.i18n("error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.TOWN_PROTECTIONS.getPermissionKey())
                    .build()
                    .sendMessage();
            return false;
        }

        if (this.isChunkScope(context) && this.resolveTargetChunk(context, town) == null) {
            this.i18n("error.chunk_unavailable", player).includePrefix().build().sendMessage();
            return false;
        }

        return true;
    }

    private @Nullable RTown resolveTown(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null) {
            return null;
        }

        final RRTown townRepository = plugin.getTownRepository();
        if (townRepository == null) {
            return null;
        }

        final UUID resolvedTownUuid = this.resolveTownUuid(context, player, plugin);
        if (resolvedTownUuid == null) {
            return null;
        }
        return townRepository.findByTownUUID(resolvedTownUuid);
    }

    private @Nullable UUID resolveTownUuid(
            final @NotNull Context context,
            final @NotNull Player player,
            final @NotNull RDT plugin
    ) {
        try {
            final UUID explicitTownUuid = this.townUuid.get(context);
            if (explicitTownUuid != null) {
                return explicitTownUuid;
            }
        } catch (final Exception ignored) {
        }

        final RRDTPlayer playerRepository = plugin.getPlayerRepository();
        if (playerRepository == null) {
            return null;
        }

        final RDTPlayer townPlayer = playerRepository.findByPlayer(player.getUniqueId());
        if (townPlayer == null) {
            return null;
        }
        return townPlayer.getTownUUID();
    }

    private @Nullable RDTPlayer resolveTownPlayer(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null || plugin.getPlayerRepository() == null) {
            return null;
        }
        return plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
    }

    private @Nullable RChunk resolveTargetChunk(
            final @NotNull Context context,
            final @NotNull RTown town
    ) {
        if (!this.isChunkScope(context)) {
            return null;
        }

        final Integer resolvedChunkX = this.resolveChunkX(context);
        final Integer resolvedChunkZ = this.resolveChunkZ(context);
        if (resolvedChunkX == null || resolvedChunkZ == null) {
            return null;
        }

        for (final RChunk chunk : town.getChunks()) {
            if (chunk.getX_loc() == resolvedChunkX && chunk.getZ_loc() == resolvedChunkZ) {
                return chunk;
            }
        }
        return null;
    }

    private @NotNull String resolveRequiredRoleId(
            final @NotNull Context context,
            final @NotNull RTown town,
            final @Nullable RChunk chunk,
            final @NotNull TownProtections protection
    ) {
        if (this.isChunkScope(context) && chunk != null) {
            final String chunkOverride = chunk.getProtectionOverrideRoleId(protection);
            if (chunkOverride != null) {
                return chunkOverride;
            }
        }
        return town.getProtectionRoleId(protection);
    }

    private @NotNull List<String> resolveRoleCycle(final @NotNull RTown town) {
        final List<String> configuredRoleIds = town.getRoles().stream()
                .map(TownRole::getRoleId)
                .map(RTown::normalizeRoleId)
                .distinct()
                .sorted()
                .toList();

        final List<String> ordered = new ArrayList<>();
        this.addRoleIfPresent(ordered, configuredRoleIds, RTown.PUBLIC_ROLE_ID);
        this.addRoleIfPresent(ordered, configuredRoleIds, RTown.MEMBER_ROLE_ID);
        this.addRoleIfPresent(ordered, configuredRoleIds, RTown.MAYOR_ROLE_ID);
        this.addRoleIfPresent(ordered, configuredRoleIds, RTown.RESTRICTED_ROLE_ID);
        for (final String roleId : configuredRoleIds) {
            if (!ordered.contains(roleId)) {
                ordered.add(roleId);
            }
        }
        return ordered;
    }

    private void addRoleIfPresent(
            final @NotNull List<String> orderedRoles,
            final @NotNull List<String> allRoles,
            final @NonNull String targetRoleId
    ) {
        final String normalizedRoleId = RTown.normalizeRoleId(targetRoleId);
        if (allRoles.contains(normalizedRoleId) && !orderedRoles.contains(normalizedRoleId)) {
            orderedRoles.add(normalizedRoleId);
        }
    }

    private @NotNull String resolveScopeLabel(
            final @NotNull Player player,
            final @NotNull String scope
    ) {
        final String key = SCOPE_CHUNK.equals(scope) ? "scope.chunk" : "scope.town";
        return PlainTextComponentSerializer.plainText().serialize(
                this.i18n(key, player).build().component()
        );
    }

    private @NotNull String resolveSourceStateLabel(
            final @NotNull Context context,
            final @NotNull Player player,
            final @Nullable RChunk chunk,
            final @NotNull TownProtections protection
    ) {
        final String key;
        if (this.isChunkScope(context) && chunk != null && chunk.hasProtectionOverride(protection)) {
            key = "protection.source.chunk";
        } else {
            key = "protection.source.town";
        }
        return PlainTextComponentSerializer.plainText().serialize(
                this.i18n(key, player).build().component()
        );
    }

    private @NotNull Material resolveRoleMaterial(final @NotNull String roleId) {
        if (RTown.PUBLIC_ROLE_ID.equals(roleId)) {
            return Material.LIME_DYE;
        }
        if (RTown.MEMBER_ROLE_ID.equals(roleId)) {
            return Material.ORANGE_DYE;
        }
        if (RTown.MAYOR_ROLE_ID.equals(roleId)) {
            return Material.RED_DYE;
        }
        if (RTown.RESTRICTED_ROLE_ID.equals(roleId)) {
            return Material.BLACK_DYE;
        }
        return Material.LIGHT_BLUE_DYE;
    }

    private boolean isChunkScope(final @NotNull Context context) {
        return SCOPE_CHUNK.equals(this.resolveProtectionScope(context));
    }

    private @NotNull String resolveProtectionScope(final @NotNull Context context) {
        try {
            final String explicitScope = this.protectionScope.get(context);
            if (explicitScope != null && !explicitScope.isBlank()) {
                return explicitScope.trim().toLowerCase(Locale.ROOT);
            }
        } catch (final Exception ignored) {
        }

        if (this.resolveChunkX(context) != null && this.resolveChunkZ(context) != null) {
            return SCOPE_CHUNK;
        }
        return SCOPE_TOWN;
    }

    private @Nullable Integer resolveChunkX(final @NotNull Context context) {
        try {
            return this.chunkX.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @Nullable Integer resolveChunkZ(final @NotNull Context context) {
        try {
            return this.chunkZ.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @Nullable Integer resolveBlockX(final @NotNull Context context) {
        try {
            return this.blockX.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @Nullable Integer resolveBlockY(final @NotNull Context context) {
        try {
            return this.blockY.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @Nullable Integer resolveBlockZ(final @NotNull Context context) {
        try {
            return this.blockZ.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @Nullable String resolveBlockWorld(final @NotNull Context context) {
        try {
            return this.blockWorld.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @NotNull Map<String, Object> buildOpenData(
            final @NotNull Context context,
            final @NotNull RDT plugin,
            final @NotNull RTown town
    ) {
        final Map<String, Object> data = new HashMap<>();
        data.put("plugin", plugin);
        data.put("town_uuid", town.getIdentifier());
        data.put("protection_scope", this.resolveProtectionScope(context));

        final Integer resolvedChunkX = this.resolveChunkX(context);
        final Integer resolvedChunkZ = this.resolveChunkZ(context);
        if (resolvedChunkX != null) {
            data.put("chunk_x", resolvedChunkX);
        }
        if (resolvedChunkZ != null) {
            data.put("chunk_z", resolvedChunkZ);
        }

        final Integer resolvedBlockX = this.resolveBlockX(context);
        final Integer resolvedBlockY = this.resolveBlockY(context);
        final Integer resolvedBlockZ = this.resolveBlockZ(context);
        final String resolvedBlockWorld = this.resolveBlockWorld(context);
        if (resolvedBlockX != null) {
            data.put("block_x", resolvedBlockX);
        }
        if (resolvedBlockY != null) {
            data.put("block_y", resolvedBlockY);
        }
        if (resolvedBlockZ != null) {
            data.put("block_z", resolvedBlockZ);
        }
        if (resolvedBlockWorld != null && !resolvedBlockWorld.isBlank()) {
            data.put("block_world", resolvedBlockWorld);
        }

        return data;
    }

    private @NotNull Map<String, Object> buildChunkBackData(
            final @NotNull Context context,
            final @NotNull RDT plugin,
            final @NotNull RTown town
    ) {
        final Map<String, Object> data = new HashMap<>();
        data.put("plugin", plugin);
        data.put("town_uuid", town.getIdentifier());

        final Integer resolvedChunkX = this.resolveChunkX(context);
        final Integer resolvedChunkZ = this.resolveChunkZ(context);
        if (resolvedChunkX != null) {
            data.put("chunk_x", resolvedChunkX);
        }
        if (resolvedChunkZ != null) {
            data.put("chunk_z", resolvedChunkZ);
        }

        final Integer resolvedBlockX = this.resolveBlockX(context);
        final Integer resolvedBlockY = this.resolveBlockY(context);
        final Integer resolvedBlockZ = this.resolveBlockZ(context);
        final String resolvedBlockWorld = this.resolveBlockWorld(context);
        if (resolvedBlockX != null) {
            data.put("block_x", resolvedBlockX);
        }
        if (resolvedBlockY != null) {
            data.put("block_y", resolvedBlockY);
        }
        if (resolvedBlockZ != null) {
            data.put("block_z", resolvedBlockZ);
        }
        if (resolvedBlockWorld != null && !resolvedBlockWorld.isBlank()) {
            data.put("block_world", resolvedBlockWorld);
        }

        return data;
    }

    private @Nullable RDT resolvePlugin(final @NotNull Context context) {
        try {
            return this.rdt.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }
}
