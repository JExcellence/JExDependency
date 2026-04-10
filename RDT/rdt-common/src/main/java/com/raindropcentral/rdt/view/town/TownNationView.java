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
import com.raindropcentral.rdt.database.entity.NationInvite;
import com.raindropcentral.rdt.database.entity.NationInviteStatus;
import com.raindropcentral.rdt.database.entity.RNation;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.service.LevelProgressSnapshot;
import com.raindropcentral.rdt.service.NationActionResult;
import com.raindropcentral.rdt.service.NationActionStatus;
import com.raindropcentral.rdt.service.NationCreationProgressSnapshot;
import com.raindropcentral.rdt.service.NationInviteResponseResult;
import com.raindropcentral.rdt.service.TownRuntimeService;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dedicated nation hub opened from the nexus chunk.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownNationView extends BaseView {

    private static final String ACTION_LEAVE = "leave";
    private static final String ACTION_DISBAND = "disband";

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");

    /**
     * Creates the nation hub view.
     */
    public TownNationView() {
        super(TownChunkView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "town_nation_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "  a b c  ",
            "  d e f  ",
            "    g    ",
            "         ",
            "r        "
        };
    }

    /**
     * Applies nation naming results returned from the anvil flow.
     *
     * @param origin previous context
     * @param target current context
     */
    @Override
    public void onResume(final @NotNull Context origin, final @NotNull Context target) {
        final Map<String, Object> data = TownNationViewSupport.copyInitialData(target) != null
            ? TownNationViewSupport.copyInitialData(target)
            : TownNationViewSupport.copyInitialData(origin);
        final RTown town = this.resolveTown(target);
        final TownRuntimeService runtimeService = this.plugin.get(target).getTownRuntimeService();
        if (data == null || town == null || runtimeService == null) {
            target.update();
            return;
        }

        if (data.get(TownNationViewSupport.DRAFT_NATION_NAME_KEY) instanceof String draftNationName) {
            target.openForPlayer(
                TownNationFormationSelectionView.class,
                TownNationViewSupport.mergeInitialData(
                    target,
                    Map.of(TownNationViewSupport.DRAFT_NATION_NAME_KEY, draftNationName)
                )
            );
            return;
        }

        if (data.get(TownNationViewSupport.RENAMED_NATION_NAME_KEY) instanceof String renamedNationName) {
            final RNation nation = runtimeService.getNationForTown(town);
            final NationActionResult result = nation == null
                ? new NationActionResult(NationActionStatus.INVALID_TARGET, null)
                : runtimeService.renameNation(target.getPlayer(), nation, renamedNationName);
            this.sendActionMessage(target.getPlayer(), "rename", result, null);
            target.openForPlayer(TownNationView.class, this.reopenData(target, town));
            return;
        }

        target.update();
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RTown town = this.resolveTown(render);
        final TownRuntimeService runtimeService = this.plugin.get(render).getTownRuntimeService();
        if (town == null || runtimeService == null) {
            render.slot(22).renderWith(() -> this.createMissingItem(player));
            return;
        }

        final RNation activeNation = runtimeService.getNationForTown(town);
        final RNation pendingNation = activeNation == null ? runtimeService.getPendingNationCreatedBy(town) : null;
        final NationInvite pendingInvite = activeNation == null && pendingNation == null
            ? runtimeService.getPendingNationInviteFor(town)
            : null;
        final RNation inviteNation = pendingInvite == null ? null : runtimeService.getNation(pendingInvite.getNationUuid());
        final NationMenuState menuState = this.resolveMenuState(town, activeNation, pendingNation, pendingInvite);

        render.layoutSlot('s', this.createSummaryItem(render, town, menuState, activeNation, pendingNation, pendingInvite, inviteNation));
        render.layoutSlot('r', this.createReturnItem(player)).onClick(SlotClickContext::back);

        switch (menuState) {
            case NO_NATION -> this.renderCreationState(render, town);
            case PENDING_CREATED -> this.renderPendingCreatedState(render, town, pendingNation);
            case PENDING_INVITE -> this.renderPendingInviteState(render, town, pendingInvite, inviteNation);
            case ACTIVE_CAPITAL -> this.renderActiveCapitalState(render, town, activeNation);
            case ACTIVE_MEMBER -> this.renderActiveMemberState(render, town, activeNation);
        }
    }

    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void renderCreationState(final @NotNull RenderContext render, final @NotNull RTown town) {
        final NationCreationProgressSnapshot snapshot = TownNationViewSupport.resolveCreationSnapshot(render);
        if (snapshot == null) {
            return;
        }

        render.layoutSlot('a', this.createRequirementsItem(render.getPlayer(), snapshot))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownNationRequirementsView.class,
                this.reopenData(clickContext, town)
            ));
        render.layoutSlot('b', this.createRewardsItem(render.getPlayer(), snapshot))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownNationRewardsView.class,
                this.reopenData(clickContext, town)
            ));
        render.layoutSlot('c', this.createCreateItem(render, town, snapshot))
            .onClick(clickContext -> this.handleCreateNationClick(clickContext, town, snapshot));
    }

    private void renderPendingCreatedState(
        final @NotNull RenderContext render,
        final @NotNull RTown town,
        final @Nullable RNation pendingNation
    ) {
        render.layoutSlot('a', this.createPendingInvitesItem(render, pendingNation))
            .onClick(clickContext -> {
                if (pendingNation != null) {
                    clickContext.openForPlayer(
                        TownNationInviteListView.class,
                        TownNationViewSupport.createNationNavigationData(clickContext, town, pendingNation)
                    );
                }
            });
        render.layoutSlot('b', this.createWaitingItem(render.getPlayer()));
    }

    private void renderPendingInviteState(
        final @NotNull RenderContext render,
        final @NotNull RTown town,
        final @Nullable NationInvite pendingInvite,
        final @Nullable RNation inviteNation
    ) {
        render.layoutSlot('a', this.createInviteDecisionItem(render.getPlayer(), "accept"))
            .onClick(clickContext -> {
                if (pendingInvite == null) {
                    return;
                }
                final NationInviteResponseResult result = this.plugin.get(clickContext)
                    .getTownRuntimeService()
                    .acceptNationInvite(clickContext.getPlayer(), pendingInvite);
                this.sendInviteResponseMessage(clickContext.getPlayer(), "accept", result, inviteNation);
                clickContext.openForPlayer(TownNationView.class, this.reopenData(clickContext, town));
            });
        render.layoutSlot('b', this.createInviteDecisionItem(render.getPlayer(), "decline"))
            .onClick(clickContext -> {
                if (pendingInvite == null) {
                    return;
                }
                final NationInviteResponseResult result = this.plugin.get(clickContext)
                    .getTownRuntimeService()
                    .declineNationInvite(clickContext.getPlayer(), pendingInvite);
                this.sendInviteResponseMessage(clickContext.getPlayer(), "decline", result, inviteNation);
                clickContext.openForPlayer(TownNationView.class, this.reopenData(clickContext, town));
            });
        render.layoutSlot('c', this.createIncomingInviteItem(render.getPlayer(), pendingInvite, inviteNation));
    }

    private void renderActiveCapitalState(
        final @NotNull RenderContext render,
        final @NotNull RTown town,
        final @Nullable RNation nation
    ) {
        if (nation == null) {
            return;
        }

        render.layoutSlot('a', this.createMembersItem(render, nation))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownNationMemberListView.class,
                TownNationViewSupport.createNationNavigationData(clickContext, town, nation)
            ));
        render.layoutSlot('b', this.createPendingInvitesItem(render, nation))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownNationInviteListView.class,
                TownNationViewSupport.createNationNavigationData(clickContext, town, nation)
            ));
        render.layoutSlot('c', this.createManageItem(render.getPlayer(), "invite_town"))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownNationInviteTownView.class,
                TownNationViewSupport.createNationNavigationData(clickContext, town, nation)
            ));
        render.layoutSlot('d', this.createManageItem(render.getPlayer(), "rename"))
            .onClick(clickContext -> clickContext.openForPlayer(
                NationRenameAnvilView.class,
                TownNationViewSupport.mergeInitialData(
                    clickContext,
                    Map.of(
                        "nation_uuid", nation.getNationUuid(),
                        "current_nation_name", nation.getNationName()
                    )
                )
            ));
        render.layoutSlot('e', this.createManageItem(render.getPlayer(), "promote"))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownNationCapitalSelectionView.class,
                TownNationViewSupport.createNationNavigationData(clickContext, town, nation)
            ));
        render.layoutSlot('f', this.createManageItem(render.getPlayer(), "disband"))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownNationConfirmView.class,
                TownNationViewSupport.mergeInitialData(
                    clickContext,
                    Map.of(
                        "nation_uuid", nation.getNationUuid(),
                        "confirm_action", ACTION_DISBAND
                    )
                )
            ));
        render.layoutSlot('g', this.createProgressionItem(
                render.getPlayer(),
                this.plugin.get(render).getTownRuntimeService().getNationLevelProgress(render.getPlayer(), town)
            ))
            .onClick(clickContext -> this.handleProgressionClick(clickContext, town));
    }

    private void renderActiveMemberState(
        final @NotNull RenderContext render,
        final @NotNull RTown town,
        final @Nullable RNation nation
    ) {
        if (nation == null) {
            return;
        }

        render.layoutSlot('a', this.createMembersItem(render, nation))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownNationMemberListView.class,
                TownNationViewSupport.createNationNavigationData(clickContext, town, nation)
            ));
        render.layoutSlot('b', this.createPendingInvitesItem(render, nation))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownNationInviteListView.class,
                TownNationViewSupport.createNationNavigationData(clickContext, town, nation)
            ));
        render.layoutSlot('c', this.createManageItem(render.getPlayer(), "leave"))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownNationConfirmView.class,
                TownNationViewSupport.mergeInitialData(
                    clickContext,
                    Map.of(
                        "nation_uuid", nation.getNationUuid(),
                        "confirm_action", ACTION_LEAVE
                    )
                )
            ));
        render.layoutSlot('g', this.createProgressionItem(
                render.getPlayer(),
                this.plugin.get(render).getTownRuntimeService().getNationLevelProgress(render.getPlayer(), town)
            ))
            .onClick(clickContext -> this.handleProgressionClick(clickContext, town));
    }

    private void handleCreateNationClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RTown town,
        final @NotNull NationCreationProgressSnapshot snapshot
    ) {
        if (!snapshot.available()) {
            return;
        }
        if (!snapshot.readyToCreate()) {
            clickContext.openForPlayer(TownNationRequirementsView.class, this.reopenData(clickContext, town));
            return;
        }
        clickContext.openForPlayer(CreateNationNameAnvilView.class, this.reopenData(clickContext, town));
    }

    private void handleProgressionClick(final @NotNull SlotClickContext clickContext, final @NotNull RTown town) {
        clickContext.openForPlayer(
            TownLevelProgressView.class,
            TownLevelViewSupport.createNationNavigationData(clickContext, town)
        );
    }

    private @Nullable RTown resolveTown(final @NotNull Context context) {
        return this.plugin.get(context).getTownRuntimeService() == null || this.townUuid.get(context) == null
            ? null
            : this.plugin.get(context).getTownRuntimeService().getTown(this.townUuid.get(context));
    }

    private @NotNull NationMenuState resolveMenuState(
        final @NotNull RTown town,
        final @Nullable RNation activeNation,
        final @Nullable RNation pendingNation,
        final @Nullable NationInvite pendingInvite
    ) {
        if (pendingInvite != null) {
            return NationMenuState.PENDING_INVITE;
        }
        if (pendingNation != null) {
            return NationMenuState.PENDING_CREATED;
        }
        if (activeNation != null) {
            return activeNation.getCapitalTownUuid().equals(town.getTownUUID())
                ? NationMenuState.ACTIVE_CAPITAL
                : NationMenuState.ACTIVE_MEMBER;
        }
        return NationMenuState.NO_NATION;
    }

    private @NotNull Map<String, Object> reopenData(final @NotNull Context context, final @NotNull RTown town) {
        final Map<String, Object> data = new LinkedHashMap<>();
        final Map<String, Object> copiedData = TownNationViewSupport.copyInitialData(context);
        if (copiedData != null) {
            data.putAll(TownNationViewSupport.stripTransientData(copiedData));
        }
        data.put("plugin", this.plugin.get(context));
        data.put("town_uuid", town.getTownUUID());
        return data;
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Context context,
        final @NotNull RTown town,
        final @NotNull NationMenuState menuState,
        final @Nullable RNation activeNation,
        final @Nullable RNation pendingNation,
        final @Nullable NationInvite pendingInvite,
        final @Nullable RNation inviteNation
    ) {
        final Player player = context.getPlayer();
        return switch (menuState) {
            case NO_NATION -> {
                final NationCreationProgressSnapshot snapshot = TownNationViewSupport.resolveCreationSnapshot(context);
                final TownRuntimeService runtimeService = this.plugin.get(context).getTownRuntimeService();
                final int unlockLevel = runtimeService == null ? 8 : runtimeService.getTownNationUnlockLevel();
                final int alliedTownCount = runtimeService == null
                    ? 0
                    : runtimeService.getEligibleNationFormationTowns(town).size();
                yield UnifiedBuilderFactory.item(snapshot != null && snapshot.available() ? Material.BEACON : Material.GRAY_DYE)
                    .setName(this.i18n("summary.none.name", player).build().component())
                    .setLore(this.i18n("summary.none.lore", player)
                        .withPlaceholders(Map.of(
                            "town_name", town.getTownName(),
                            "nexus_level", town.getNexusLevel(),
                            "unlock_level", unlockLevel,
                            "allied_town_count", alliedTownCount,
                            "progress_percent", snapshot == null ? 0 : Math.round(snapshot.progress() * 100.0D),
                            "requirement_count", snapshot == null ? 0 : snapshot.requirements().size(),
                            "reward_count", snapshot == null ? 0 : snapshot.rewards().size(),
                            "ready_state", snapshot != null && snapshot.readyToCreate() ? "Ready" : "In Progress"
                        ))
                        .build()
                        .children())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
            }
            case PENDING_CREATED -> {
                final List<NationInvite> invites = pendingNation == null
                    ? List.of()
                    : this.plugin.get(context).getTownRuntimeService().getNationInvites(pendingNation);
                final long acceptedCount = invites.stream().filter(invite -> invite.getStatus() == NationInviteStatus.ACCEPTED).count();
                final long declinedCount = invites.stream()
                    .filter(invite -> invite.getStatus() != NationInviteStatus.PENDING && invite.getStatus() != NationInviteStatus.ACCEPTED)
                    .count();
                yield UnifiedBuilderFactory.item(Material.CLOCK)
                    .setName(this.i18n("summary.pending.name", player).build().component())
                    .setLore(this.i18n("summary.pending.lore", player)
                        .withPlaceholders(Map.of(
                            "nation_name", pendingNation == null ? "-" : pendingNation.getNationName(),
                            "invite_count", invites.size(),
                            "accepted_count", acceptedCount,
                            "declined_count", declinedCount,
                            "expires_in", pendingNation == null
                                ? "Unknown"
                                : TownOverviewView.formatDurationMillis(Math.max(0L, pendingNation.getExpiresAt() - System.currentTimeMillis()))
                        ))
                        .build()
                        .children())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
            }
            case PENDING_INVITE -> UnifiedBuilderFactory.item(Material.BELL)
                .setName(this.i18n("summary.invite.name", player).build().component())
                .setLore(this.i18n("summary.invite.lore", player)
                    .withPlaceholders(Map.of(
                        "nation_name", inviteNation == null ? "-" : inviteNation.getNationName(),
                        "capital_town", inviteNation == null ? "-" : this.resolveCapitalTownName(context, inviteNation),
                        "invite_type", pendingInvite == null
                            ? "Unknown"
                            : TownNationViewSupport.inviteTypeText(player, pendingInvite.getInviteType()),
                        "expires_in", pendingInvite == null
                            ? "Unknown"
                            : TownOverviewView.formatDurationMillis(Math.max(0L, pendingInvite.getExpiresAt() - System.currentTimeMillis()))
                    ))
                    .build()
                    .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
            case ACTIVE_CAPITAL, ACTIVE_MEMBER -> {
                final RNation nation = activeNation;
                final LevelProgressSnapshot progressionSnapshot = nation == null
                    ? null
                    : this.plugin.get(context).getTownRuntimeService().getNationLevelProgress(player, town);
                final List<RTown> memberTowns = nation == null
                    ? List.of()
                    : this.plugin.get(context).getTownRuntimeService().getNationMemberTowns(nation);
                final long pendingInviteCount = nation == null
                    ? 0L
                    : this.plugin.get(context).getTownRuntimeService().getNationInvites(nation).stream().filter(NationInvite::isPending).count();
                yield UnifiedBuilderFactory.item(menuState == NationMenuState.ACTIVE_CAPITAL ? Material.EMERALD_BLOCK : Material.LIME_DYE)
                    .setName(this.i18n("summary.active.name", player).build().component())
                    .setLore(this.i18n("summary.active.lore", player)
                        .withPlaceholders(Map.of(
                            "nation_name", nation == null ? "-" : nation.getNationName(),
                            "capital_town", nation == null ? "-" : this.resolveCapitalTownName(context, nation),
                            "member_count", memberTowns.size(),
                            "pending_invite_count", pendingInviteCount,
                            "viewer_role", TownNationViewSupport.nationRoleText(player, menuState == NationMenuState.ACTIVE_CAPITAL),
                            "nation_level", progressionSnapshot == null ? 1 : progressionSnapshot.currentLevel(),
                            "target_level", progressionSnapshot == null ? 1 : progressionSnapshot.displayLevel(),
                            "max_level", progressionSnapshot == null ? 1 : progressionSnapshot.maxLevel(),
                            "progress_percent", progressionSnapshot == null ? 0 : Math.round(progressionSnapshot.progress() * 100.0D)
                        ))
                        .build()
                        .children())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
            }
        };
    }

    private @NotNull ItemStack createRequirementsItem(
        final @NotNull Player player,
        final @NotNull NationCreationProgressSnapshot snapshot
    ) {
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
            .setName(this.i18n("requirements.name", player).build().component())
            .setLore(this.i18n("requirements.lore", player)
                .withPlaceholders(Map.of(
                    "requirement_count", snapshot.requirements().size(),
                    "progress_percent", Math.round(snapshot.progress() * 100.0D)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createProgressionItem(
        final @NotNull Player player,
        final @NotNull LevelProgressSnapshot snapshot
    ) {
        final Material material = !snapshot.available()
            ? Material.BARRIER
            : snapshot.maxLevelReached()
                ? Material.NETHER_STAR
                : snapshot.readyToLevelUp()
                    ? Material.EMERALD_BLOCK
                    : Material.BEACON;
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("progression.name", player).build().component())
            .setLore(this.i18n("progression.lore", player)
                .withPlaceholders(Map.of(
                    "nation_level", snapshot.currentLevel(),
                    "target_level", snapshot.displayLevel(),
                    "max_level", snapshot.maxLevel(),
                    "progress_percent", Math.round(snapshot.progress() * 100.0D)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createRewardsItem(
        final @NotNull Player player,
        final @NotNull NationCreationProgressSnapshot snapshot
    ) {
        return UnifiedBuilderFactory.item(Material.CHEST)
            .setName(this.i18n("rewards.name", player).build().component())
            .setLore(this.i18n("rewards.lore", player)
                .withPlaceholder("reward_count", snapshot.rewards().size())
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createCreateItem(
        final @NotNull Context context,
        final @NotNull RTown town,
        final @NotNull NationCreationProgressSnapshot snapshot
    ) {
        final TownRuntimeService runtimeService = this.plugin.get(context).getTownRuntimeService();
        final int unlockLevel = runtimeService == null ? 8 : runtimeService.getTownNationUnlockLevel();
        final int minimumTowns = runtimeService == null ? 2 : runtimeService.getTownNationMinTowns();
        final int eligibleTownCount = runtimeService == null ? 0 : runtimeService.getEligibleNationFormationTowns(town).size();
        final Material material = !snapshot.available()
            ? Material.BARRIER
            : snapshot.readyToCreate()
                ? Material.LIME_DYE
                : Material.EXPERIENCE_BOTTLE;
        final String loreKey = !snapshot.available()
            ? "create.locked.lore"
            : snapshot.readyToCreate()
                ? "create.ready.lore"
                : "create.progress.lore";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("create.name", context.getPlayer()).build().component())
            .setLore(this.i18n(loreKey, context.getPlayer())
                .withPlaceholders(Map.of(
                    "unlock_level", unlockLevel,
                    "minimum_towns", minimumTowns,
                    "eligible_town_count", eligibleTownCount,
                    "progress_percent", Math.round(snapshot.progress() * 100.0D)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .setGlowing(snapshot.readyToCreate())
            .build();
    }

    private @NotNull ItemStack createMembersItem(final @NotNull Context context, final @NotNull RNation nation) {
        final int memberCount = this.plugin.get(context).getTownRuntimeService().getNationMemberTowns(nation).size();
        return UnifiedBuilderFactory.item(Material.FILLED_MAP)
            .setName(this.i18n("members.name", context.getPlayer()).build().component())
            .setLore(this.i18n("members.lore", context.getPlayer())
                .withPlaceholder("member_count", memberCount)
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createPendingInvitesItem(final @NotNull Context context, final @Nullable RNation nation) {
        final long pendingInviteCount = nation == null
            ? 0L
            : this.plugin.get(context).getTownRuntimeService().getNationInvites(nation).stream().filter(NationInvite::isPending).count();
        return UnifiedBuilderFactory.item(pendingInviteCount > 0L ? Material.PAPER : Material.GRAY_DYE)
            .setName(this.i18n("invites.name", context.getPlayer()).build().component())
            .setLore(this.i18n("invites.lore", context.getPlayer())
                .withPlaceholder("pending_invite_count", pendingInviteCount)
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createWaitingItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.CLOCK)
            .setName(this.i18n("waiting.name", player).build().component())
            .setLore(this.i18n("waiting.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createInviteDecisionItem(final @NotNull Player player, final @NotNull String action) {
        final Material material = "accept".equals(action) ? Material.LIME_DYE : Material.RED_DYE;
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("invite." + action + ".name", player).build().component())
            .setLore(this.i18n("invite." + action + ".lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createIncomingInviteItem(
        final @NotNull Player player,
        final @Nullable NationInvite invite,
        final @Nullable RNation nation
    ) {
        return UnifiedBuilderFactory.item(Material.NAME_TAG)
            .setName(this.i18n("invite.info.name", player).build().component())
            .setLore(this.i18n("invite.info.lore", player)
                .withPlaceholders(Map.of(
                    "nation_name", nation == null ? "-" : nation.getNationName(),
                    "capital_town", this.resolveCapitalTownName(player, nation),
                    "invite_type", invite == null ? "Unknown" : TownNationViewSupport.inviteTypeText(player, invite.getInviteType())
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createManageItem(final @NotNull Player player, final @NotNull String actionKey) {
        final Material material = switch (actionKey) {
            case "invite_town" -> Material.WRITABLE_BOOK;
            case "rename" -> Material.NAME_TAG;
            case "promote" -> Material.GOLD_INGOT;
            case "leave" -> Material.OAK_DOOR;
            case "disband" -> Material.TNT;
            default -> Material.PAPER;
        };
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("manage." + actionKey + ".name", player).build().component())
            .setLore(this.i18n("manage." + actionKey + ".lore", player).build().children())
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

    private @NotNull ItemStack createMissingItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private void sendActionMessage(
        final @NotNull Player player,
        final @NotNull String actionKey,
        final @NotNull NationActionResult result,
        final @Nullable RTown targetTown
    ) {
        final RNation nation = result.nation();
        new I18n.Builder("town_nation_shared.messages." + actionKey + '.' + TownNationViewSupport.toActionMessageKey(result.status()), player)
            .includePrefix()
            .withPlaceholders(Map.of(
                "nation_name", nation == null ? "-" : nation.getNationName(),
                "capital_town", this.resolveCapitalTownName(player, nation),
                "target_town", targetTown == null ? "-" : targetTown.getTownName()
            ))
            .build()
            .sendMessage();
    }

    private void sendInviteResponseMessage(
        final @NotNull Player player,
        final @NotNull String actionKey,
        final @NotNull NationInviteResponseResult result,
        final @Nullable RNation fallbackNation
    ) {
        final RNation nation = result.nation() == null ? fallbackNation : result.nation();
        new I18n.Builder("town_nation_shared.messages.invite_" + actionKey + '.'
            + TownNationViewSupport.toInviteResponseMessageKey(result.status()), player)
            .includePrefix()
            .withPlaceholders(Map.of(
                "nation_name", nation == null ? "-" : nation.getNationName(),
                "capital_town", this.resolveCapitalTownName(player, nation)
            ))
            .build()
            .sendMessage();
    }

    private @NotNull String resolveCapitalTownName(final @NotNull Context context, final @Nullable RNation nation) {
        if (nation == null) {
            return "-";
        }
        final TownRuntimeService runtimeService = this.plugin.get(context).getTownRuntimeService();
        if (runtimeService == null) {
            return nation.getCapitalTownUuid().toString();
        }
        final RTown capitalTown = runtimeService.getTown(nation.getCapitalTownUuid());
        return capitalTown == null ? nation.getCapitalTownUuid().toString() : capitalTown.getTownName();
    }

    private @NotNull String resolveCapitalTownName(final @NotNull Player player, final @Nullable RNation nation) {
        return nation == null ? "-" : nation.getCapitalTownUuid().toString();
    }

    private enum NationMenuState {
        NO_NATION,
        PENDING_CREATED,
        PENDING_INVITE,
        ACTIVE_CAPITAL,
        ACTIVE_MEMBER
    }
}
