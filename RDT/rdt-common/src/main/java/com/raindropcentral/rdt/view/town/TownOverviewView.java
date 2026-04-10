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
import com.raindropcentral.rdt.service.LevelProgressSnapshot;
import com.raindropcentral.rdt.service.LevelScope;
import com.raindropcentral.rdt.service.LevelUpResult;
import com.raindropcentral.rdt.service.NexusAccessService;
import com.raindropcentral.rdt.service.TownRuntimeService;
import com.raindropcentral.rdt.utils.TownArchetype;
import com.raindropcentral.rdt.utils.TownOverviewAccessMode;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rdt.view.main.TownHubView;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Main town menu with distinct remote and nexus-governance access modes.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownOverviewView extends BaseView {

    private static final String SESSION_TOKEN_KEY = "nexus_session";
    private static final String NEXUS_WORLD_KEY = "nexus_world";
    private static final String NEXUS_CHUNK_X_KEY = "nexus_chunk_x";
    private static final String NEXUS_CHUNK_Z_KEY = "nexus_chunk_z";

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");

    /**
     * Creates the town overview view.
     */
    public TownOverviewView() {
        super(TownHubView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_overview_ui";
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
            "  c b k  ",
            "  p a n  ",
            "   l u   ",
            "   o f   ",
            "         "
        };
    }

    /**
     * Refreshes nexus lock states while the view remains open.
     *
     * @return periodic refresh interval
     */
    @Override
    protected int getUpdateSchedule() {
        return 20;
    }

    /**
     * Applies rename results returned from the anvil flow.
     *
     * @param origin previous context
     * @param target current context
     */
    @Override
    public void onResume(final @NotNull Context origin, final @NotNull Context target) {
        final Map<String, Object> data = this.extractData(target) != null ? this.extractData(target) : this.extractData(origin);
        final RTown town = this.resolveTown(target);
        if (town == null || data == null) {
            target.update();
            return;
        }

        if (data.get("renamed_town_name") instanceof String renamedTownName) {
            final boolean renamed = this.isNexusGovernanceUnlocked(target, town)
                && this.viewerHasPermission(target, town, TownPermissions.RENAME_TOWN)
                && this.plugin.get(target).getTownRuntimeService().renameTown(town, renamedTownName);
            final String key = renamed ? "rename.success" : "rename.failed";
            new I18n.Builder(this.getKey() + '.' + key, target.getPlayer())
                .includePrefix()
                .withPlaceholder("town_name", renamedTownName)
                .build()
                .sendMessage();
            target.openForPlayer(TownOverviewView.class, this.reopenData(target, town.getTownUUID()));
            return;
        }

        if (data.get("updated_town_color") instanceof String updatedTownColor) {
            final boolean updated = this.isNexusGovernanceUnlocked(target, town)
                && this.viewerHasPermission(target, town, TownPermissions.CHANGE_TOWN_COLOR)
                && this.plugin.get(target).getTownRuntimeService().setTownColor(town, updatedTownColor);
            final String key = updated ? "color.success" : "color.failed";
            new I18n.Builder(this.getKey() + '.' + key, target.getPlayer())
                .includePrefix()
                .withPlaceholder("town_color", updatedTownColor)
                .build()
                .sendMessage();
            target.openForPlayer(TownOverviewView.class, this.reopenData(target, town.getTownUUID()));
            return;
        }

        target.update();
    }

    /**
     * Re-renders the view so dynamic lock states stay current.
     *
     * @param context active context
     */
    @Override
    public void onUpdate(final @NotNull Context context) {
        // Inventory Framework already refreshes rendered components after calling onUpdate.
        // Re-entering context.update() here would recurse back into this handler indefinitely.
    }

    /**
     * Renders town actions for the requested town.
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

        render.slot(4).renderWith(() -> this.createSummaryItem(render, town));
        render.slot(20).renderWith(() -> this.createClaimsItem(render, town))
            .onClick(clickContext -> this.handleClaimsClick(clickContext, town));
        render.slot(22).renderWith(() -> this.createBankItem(render, town))
            .onClick(clickContext -> this.handleBankClick(clickContext, town));
        render.slot(24).renderWith(() -> this.createChunkItem(render, town))
            .onClick(clickContext -> this.handleChunkClick(clickContext, town));
        render.slot(29).renderWith(() -> this.createProtectionsItem(render, town))
            .onClick(clickContext -> this.handleProtectionsClick(clickContext, town));
        render.slot(31).renderWith(() -> this.createArchetypeItem(render, town))
            .onClick(clickContext -> this.handleArchetypeClick(clickContext, town));
        render.slot(33).renderWith(() -> this.createRenameItem(render, town))
            .onClick(clickContext -> this.handleRenameClick(clickContext, town));
        render.slot(30).renderWith(() -> this.createColorItem(render, town))
            .onClick(clickContext -> this.handleColorClick(clickContext, town));
        render.slot(32).renderWith(() -> this.createUpgradeItem(render, town))
            .onClick(clickContext -> this.handleUpgradeClick(clickContext, town));
        render.slot(48).renderWith(() -> this.createFobItem(render, town))
            .onClick(clickContext -> this.handleFobClick(clickContext, town));
        render.slot(49).renderWith(() -> this.createFuelItem(render, town));
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

    private void handleClaimsClick(final @NotNull SlotClickContext clickContext, final @NotNull RTown town) {
        if (!this.isOwnTownViewer(clickContext, town)) {
            return;
        }
        clickContext.openForPlayer(
            ClaimsView.class,
            Map.of(
                "plugin", this.plugin.get(clickContext),
                "town_uuid", town.getTownUUID(),
                "world_name", clickContext.getPlayer().getWorld().getName(),
                "center_x", clickContext.getPlayer().getLocation().getChunk().getX(),
                "center_z", clickContext.getPlayer().getLocation().getChunk().getZ()
            )
        );
    }

    private void handleFobClick(final @NotNull SlotClickContext clickContext, final @NotNull RTown town) {
        if (!this.isOwnTownViewer(clickContext, town)) {
            return;
        }
        if (!this.viewerHasPermission(clickContext, town, TownPermissions.CLAIM_CHUNK)) {
            new I18n.Builder(this.getKey() + ".fob.no_permission", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }
        clickContext.openForPlayer(
            TownFobClaimsView.class,
            Map.of(
                "plugin", this.plugin.get(clickContext),
                "town_uuid", town.getTownUUID(),
                "world_name", clickContext.getPlayer().getWorld().getName(),
                "center_x", clickContext.getPlayer().getLocation().getChunk().getX(),
                "center_z", clickContext.getPlayer().getLocation().getChunk().getZ()
            )
        );
    }

    private void handleBankClick(final @NotNull SlotClickContext clickContext, final @NotNull RTown town) {
        if (!this.isOwnTownViewer(clickContext, town)) {
            return;
        }
        clickContext.openForPlayer(
            TownBankView.class,
            Map.of(
                "plugin", this.plugin.get(clickContext),
                "town_uuid", town.getTownUUID(),
                "remote_bank", false
            )
        );
    }

    private void handleChunkClick(final @NotNull SlotClickContext clickContext, final @NotNull RTown town) {
        if (!this.isOwnTownViewer(clickContext, town)) {
            return;
        }

        final RTownChunk townChunk = this.plugin.get(clickContext).getTownRuntimeService() == null
            ? null
            : this.plugin.get(clickContext).getTownRuntimeService().getChunkAt(clickContext.getPlayer().getLocation());
        if (townChunk == null || !Objects.equals(townChunk.getTown().getTownUUID(), town.getTownUUID())) {
            new I18n.Builder(this.getKey() + ".chunk.not_in_chunk", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        clickContext.openForPlayer(
            TownChunkView.class,
            Map.of(
                "plugin", this.plugin.get(clickContext),
                "town_uuid", town.getTownUUID(),
                "world_name", townChunk.getWorldName(),
                "chunk_x", townChunk.getX(),
                "chunk_z", townChunk.getZ()
            )
        );
    }

    private void handleProtectionsClick(final @NotNull SlotClickContext clickContext, final @NotNull RTown town) {
        if (!this.isOwnTownViewer(clickContext, town)) {
            return;
        }
        if (!town.hasSecurityChunk()) {
            new I18n.Builder(this.getKey() + ".protections.security_required", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }
        if (!this.viewerHasPermission(clickContext, town, TownPermissions.TOWN_PROTECTIONS)) {
            new I18n.Builder(this.getKey() + ".protections.no_permission", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }
        new I18n.Builder(this.getKey() + ".protections.open_from_security_chunk", clickContext.getPlayer())
            .includePrefix()
            .build()
            .sendMessage();
    }

    private void handleArchetypeClick(final @NotNull SlotClickContext clickContext, final @NotNull RTown town) {
        if (!this.isNexusGovernanceUnlocked(clickContext, town)
            || !this.viewerHasPermission(clickContext, town, TownPermissions.SET_ARCHETYPE)) {
            this.sendReturnToNexus(clickContext.getPlayer());
            return;
        }

        clickContext.openForPlayer(TownArchetypeView.class, this.reopenData(clickContext, town.getTownUUID()));
    }

    private void handleRenameClick(final @NotNull SlotClickContext clickContext, final @NotNull RTown town) {
        if (!this.isNexusGovernanceUnlocked(clickContext, town)
            || !this.viewerHasPermission(clickContext, town, TownPermissions.RENAME_TOWN)) {
            this.sendReturnToNexus(clickContext.getPlayer());
            return;
        }

        clickContext.openForPlayer(
            TownRenameAnvilView.class,
            this.reopenData(clickContext, town.getTownUUID(), Map.of("current_town_name", town.getTownName()))
        );
    }

    private void handleColorClick(final @NotNull SlotClickContext clickContext, final @NotNull RTown town) {
        if (!this.isNexusGovernanceUnlocked(clickContext, town)
            || !this.viewerHasPermission(clickContext, town, TownPermissions.CHANGE_TOWN_COLOR)) {
            this.sendReturnToNexus(clickContext.getPlayer());
            return;
        }

        clickContext.openForPlayer(
            TownColorAnvilView.class,
            this.reopenData(clickContext, town.getTownUUID(), Map.of("current_town_color", town.getTownColorHex()))
        );
    }

    private void handleUpgradeClick(final @NotNull SlotClickContext clickContext, final @NotNull RTown town) {
        if (!this.isOwnTownViewer(clickContext, town)) {
            return;
        }

        final LevelProgressSnapshot snapshot = this.resolveNexusSnapshot(clickContext, town);
        final boolean canFinalize = snapshot != null
            && snapshot.readyToLevelUp()
            && this.isNexusGovernanceUnlocked(clickContext, town)
            && this.viewerHasPermission(clickContext, town, TownPermissions.UPGRADE_TOWN);
        if (snapshot != null && canFinalize) {
            final LevelUpResult result = this.plugin.get(clickContext).getTownRuntimeService().levelUpNexus(
                clickContext.getPlayer(),
                town
            );
            this.sendLevelMessage(clickContext.getPlayer(), LevelScope.NEXUS, result);
            clickContext.update();
            return;
        }

        clickContext.openForPlayer(
            TownLevelProgressView.class,
            TownLevelViewSupport.createNexusNavigationData(clickContext, town)
        );
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
        return this.reopenData(context, resolvedTownUuid, Map.of());
    }

    private @NotNull Map<String, Object> reopenData(
        final @NotNull Context context,
        final @NotNull UUID resolvedTownUuid,
        final @NotNull Map<String, Object> extraData
    ) {
        final Map<String, Object> data = new LinkedHashMap<>();
        final Map<String, Object> existingData = this.extractData(context);
        if (existingData != null) {
            data.putAll(existingData);
        }
        data.put("plugin", this.plugin.get(context));
        data.put("town_uuid", resolvedTownUuid);
        data.put("access_mode", this.resolveAccessMode(context));
        data.putAll(extraData);
        return data;
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

    private boolean isOwnTownViewer(final @NotNull Context context, final @NotNull RTown town) {
        final RDTPlayer playerData = this.plugin.get(context).getTownRuntimeService() == null
            ? null
            : this.plugin.get(context).getTownRuntimeService().getPlayerData(context.getPlayer().getUniqueId());
        return playerData != null && Objects.equals(playerData.getTownUUID(), town.getTownUUID());
    }

    private boolean viewerHasPermission(
        final @NotNull Context context,
        final @NotNull RTown town,
        final @NotNull TownPermissions permission
    ) {
        final RDTPlayer playerData = this.plugin.get(context).getTownRuntimeService() == null
            ? null
            : this.plugin.get(context).getTownRuntimeService().getPlayerData(context.getPlayer().getUniqueId());
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

    private void sendReturnToNexus(final @NotNull Player player) {
        new I18n.Builder(this.getKey() + ".governance.return_to_nexus", player)
            .includePrefix()
            .build()
            .sendMessage();
    }

    private @NotNull ItemStack createSummaryItem(final @NotNull Context context, final @NotNull RTown town) {
        final boolean nexusUnlocked = this.isNexusGovernanceUnlocked(context, town);
        return UnifiedBuilderFactory.item(Material.BEACON)
            .setName(this.i18n("summary.name", context.getPlayer())
                .withPlaceholder("town_name", town.getTownName())
                .build()
                .component())
            .setLore(this.i18n("summary.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "town_name", town.getTownName(),
                    "chunk_count", town.getChunks().size(),
                    "member_count", town.getMembers().size(),
                    "town_level", town.getTownLevel(),
                    "archetype", town.getArchetype() == null ? "Unassigned" : town.getArchetype().getDisplayName(),
                    "access_mode", this.resolveAccessMode(context).name(),
                    "governance_state", nexusUnlocked ? "Unlocked" : "Locked"
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createClaimsItem(final @NotNull Context context, final @NotNull RTown town) {
        final Material material = this.isOwnTownViewer(context, town) ? Material.MAP : Material.GRAY_DYE;
        final String loreKey = this.isOwnTownViewer(context, town) ? "claims.lore" : "claims.locked.lore";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("claims.name", context.getPlayer()).build().component())
            .setLore(this.i18n(loreKey, context.getPlayer())
                .withPlaceholder("chunk_count", town.getChunks().size())
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createFobItem(final @NotNull Context context, final @NotNull RTown town) {
        final boolean ownTown = this.isOwnTownViewer(context, town);
        final boolean canManage = ownTown && this.viewerHasPermission(context, town, TownPermissions.CLAIM_CHUNK);
        final RTownChunk fobChunk = town.findFobChunk();
        final String currentFob = this.resolveFobLabel(context.getPlayer(), fobChunk);
        final Material material = canManage ? Material.TARGET : Material.GRAY_DYE;
        final String loreKey = canManage ? "fob.lore" : "fob.locked.lore";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("fob.name", context.getPlayer()).build().component())
            .setLore(this.i18n(loreKey, context.getPlayer())
                .withPlaceholders(Map.of("current_fob", currentFob))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createBankItem(final @NotNull Context context, final @NotNull RTown town) {
        final Material material = this.isOwnTownViewer(context, town) ? Material.CHEST : Material.GRAY_DYE;
        final String loreKey = this.isOwnTownViewer(context, town) ? "bank.lore" : "bank.locked.lore";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("bank.name", context.getPlayer()).build().component())
            .setLore(this.i18n(loreKey, context.getPlayer())
                .withPlaceholders(Map.of(
                    "vault_balance", town.getBank(),
                    "storage_slots", town.getSharedBankStorage().size()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createChunkItem(final @NotNull Context context, final @NotNull RTown town) {
        final boolean ownTown = this.isOwnTownViewer(context, town);
        return UnifiedBuilderFactory.item(ownTown ? Material.OAK_PLANKS : Material.GRAY_DYE)
            .setName(this.i18n("chunk.name", context.getPlayer()).build().component())
            .setLore(this.i18n((ownTown ? "chunk" : "chunk.locked") + ".lore", context.getPlayer())
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createProtectionsItem(final @NotNull Context context, final @NotNull RTown town) {
        final boolean ownTown = this.isOwnTownViewer(context, town);
        final boolean hasPermission = ownTown && this.viewerHasPermission(context, town, TownPermissions.TOWN_PROTECTIONS);
        final boolean unlocked = ownTown && town.hasSecurityChunk() && hasPermission;
        final Material material = unlocked
            ? Material.SHIELD
            : ownTown && !town.hasSecurityChunk()
                ? Material.BARRIER
                : Material.GRAY_DYE;
        final String loreKey = unlocked
            ? "protections.lore"
            : !ownTown
                ? "protections.locked.lore"
                : town.hasSecurityChunk()
                    ? "protections.permission_locked.lore"
                    : "protections.security_locked.lore";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("protections.name", context.getPlayer()).build().component())
            .setLore(this.i18n(loreKey, context.getPlayer()).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createArchetypeItem(final @NotNull Context context, final @NotNull RTown town) {
        final boolean unlocked = this.isNexusGovernanceUnlocked(context, town)
            && this.viewerHasPermission(context, town, TownPermissions.SET_ARCHETYPE);
        final TownArchetype archetype = town.getArchetype();
        final TownRuntimeService townRuntimeService = this.plugin.get(context).getTownRuntimeService();
        final long remainingCooldownMillis = unlocked && townRuntimeService != null
            ? townRuntimeService.getRemainingTownArchetypeChangeCooldownMillis(town)
            : 0L;
        return UnifiedBuilderFactory.item(unlocked ? Material.NETHER_STAR : Material.GRAY_DYE)
            .setName(this.i18n("archetype.name", context.getPlayer()).build().component())
            .setLore(this.i18n((unlocked ? "archetype" : "archetype.locked") + ".lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "archetype", archetype == null ? "Unassigned" : archetype.getDisplayName(),
                    "cooldown_remaining", remainingCooldownMillis > 0L
                        ? formatDurationMillis(remainingCooldownMillis)
                        : formatDurationMillis(0L)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    static @NotNull String formatDurationMillis(final long durationMillis) {
        if (durationMillis <= 0L) {
            return "0 seconds";
        }

        final Duration duration = Duration.ofMillis(durationMillis);
        final long days = duration.toDays();
        final long hours = duration.toHoursPart();
        final long minutes = duration.toMinutesPart();
        final long seconds = duration.toSecondsPart();
        final StringBuilder formatted = new StringBuilder();

        appendDurationPart(formatted, days, "day");
        appendDurationPart(formatted, hours, "hour");
        appendDurationPart(formatted, minutes, "minute");
        if (formatted.length() == 0 || seconds > 0L) {
            appendDurationPart(formatted, seconds, "second");
        }
        return formatted.toString();
    }

    private static void appendDurationPart(
        final @NotNull StringBuilder formatted,
        final long value,
        final @NotNull String unit
    ) {
        if (value <= 0L) {
            return;
        }
        if (formatted.length() > 0) {
            formatted.append(' ');
        }
        formatted.append(value).append(' ').append(unit);
        if (value != 1L) {
            formatted.append('s');
        }
    }

    private @NotNull ItemStack createRenameItem(final @NotNull Context context, final @NotNull RTown town) {
        final boolean unlocked = this.isNexusGovernanceUnlocked(context, town)
            && this.viewerHasPermission(context, town, TownPermissions.RENAME_TOWN);
        return UnifiedBuilderFactory.item(unlocked ? Material.NAME_TAG : Material.GRAY_DYE)
            .setName(this.i18n("rename.name", context.getPlayer()).build().component())
            .setLore(this.i18n((unlocked ? "rename" : "rename.locked") + ".lore", context.getPlayer())
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createColorItem(final @NotNull Context context, final @NotNull RTown town) {
        final boolean unlocked = this.isNexusGovernanceUnlocked(context, town)
            && this.viewerHasPermission(context, town, TownPermissions.CHANGE_TOWN_COLOR);
        return UnifiedBuilderFactory.item(unlocked ? Material.LIGHT_BLUE_DYE : Material.GRAY_DYE)
            .setName(this.i18n("color.name", context.getPlayer()).build().component())
            .setLore(this.i18n((unlocked ? "color" : "color.locked") + ".lore", context.getPlayer())
                .withPlaceholder("town_color", town.getTownColorHex())
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createUpgradeItem(final @NotNull Context context, final @NotNull RTown town) {
        final boolean ownTown = this.isOwnTownViewer(context, town);
        final LevelProgressSnapshot snapshot = ownTown ? this.resolveNexusSnapshot(context, town) : null;
        final boolean canFinalize = snapshot != null
            && snapshot.readyToLevelUp()
            && this.isNexusGovernanceUnlocked(context, town)
            && this.viewerHasPermission(context, town, TownPermissions.UPGRADE_TOWN);
        final Material material = !ownTown
            ? Material.GRAY_DYE
            : snapshot != null && snapshot.maxLevelReached()
                ? Material.NETHER_STAR
                : canFinalize
                    ? Material.EMERALD_BLOCK
                    : Material.EXPERIENCE_BOTTLE;
        final String loreKey = !ownTown
            ? "upgrade.locked.lore"
            : snapshot != null && snapshot.maxLevelReached()
                ? "upgrade.max.lore"
                : canFinalize
                    ? "upgrade.ready.lore"
                    : "upgrade.progress.lore";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("upgrade.name", context.getPlayer()).build().component())
            .setLore(this.i18n(loreKey, context.getPlayer())
                .withPlaceholders(Map.of(
                    "town_level", town.getTownLevel(),
                    "current_level", snapshot == null ? town.getTownLevel() : snapshot.currentLevel(),
                    "target_level", snapshot == null ? town.getTownLevel() : snapshot.displayLevel(),
                    "progress_percent", snapshot == null ? 0 : Math.round(snapshot.progress() * 100.0D)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createFuelItem(final @NotNull Context context, final @NotNull RTown town) {
        final var fuelService = this.plugin.get(context).getTownFuelService();
        final boolean powered = fuelService != null && fuelService.isTownPowered(town);
        final boolean hasSecurityChunk = town.hasSecurityChunk();
        final double pooledFuelUnits = fuelService == null ? 0.0D : fuelService.getTotalFuelUnits(town);
        final double fuelPerHour = fuelService == null ? 0.0D : fuelService.getFuelPerHour(town);
        final int intervalSeconds = this.plugin.get(context).getSecurityConfig().getFuel().getCalculationIntervalSeconds();
        final Material material = hasSecurityChunk
            ? powered
                ? Material.REDSTONE_BLOCK
                : Material.REDSTONE
            : Material.BARRIER;
        final String loreKey = hasSecurityChunk ? "fuel.lore" : "fuel.no_security.lore";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("fuel.name", context.getPlayer()).build().component())
            .setLore(this.i18n(loreKey, context.getPlayer())
                .withPlaceholders(Map.of(
                    "pooled_fuel_fe", this.formatFuelAmount(pooledFuelUnits),
                    "fuel_per_hour", this.formatFuelAmount(fuelPerHour),
                    "interval_seconds", intervalSeconds,
                    "power_state", powered ? "Powered" : "Unpowered"
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @Nullable LevelProgressSnapshot resolveNexusSnapshot(final @NotNull Context context, final @NotNull RTown town) {
        final TownRuntimeService runtimeService = this.plugin.get(context).getTownRuntimeService();
        return runtimeService == null ? null : runtimeService.getNexusLevelProgress(context.getPlayer(), town);
    }

    private @NotNull String resolveFobLabel(final @NotNull Player player, final @Nullable RTownChunk fobChunk) {
        if (fobChunk != null) {
            return fobChunk.getWorldName() + ' ' + fobChunk.getX() + ", " + fobChunk.getZ();
        }
        return PlainTextComponentSerializer.plainText().serialize(
            this.i18n("fob.unclaimed", player).build().component()
        );
    }

    private @NotNull String formatFuelAmount(final double amount) {
        return String.format(Locale.ROOT, "%.2f", Math.max(0.0D, amount));
    }

    private void sendLevelMessage(
        final @NotNull Player player,
        final @NotNull LevelScope scope,
        final @NotNull LevelUpResult result
    ) {
        final String key = switch (result.status()) {
            case SUCCESS -> "level_up_success";
            case NO_PERMISSION -> "no_permission";
            case NOT_READY -> "not_ready";
            case MAX_LEVEL -> "max_level";
            case INVALID_TARGET, FAILED -> "level_up_failed";
        };
        new I18n.Builder("town_level_shared.messages." + key, player)
            .includePrefix()
            .withPlaceholders(Map.of(
                "level_scope", scope.getDisplayName(),
                "current_level", result.previousLevel(),
                "target_level", result.newLevel()
            ))
            .build()
            .sendMessage();
    }

    private @NotNull ItemStack createMissingTownItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
