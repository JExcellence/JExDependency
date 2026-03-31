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

package com.raindropcentral.rdr.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.service.TradeService;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Trade target selector view used to start a new invite.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class TradeTargetSelectView extends BaseView {

    private static final int[] ENTRY_SLOTS = {
        9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, 26,
        27, 28, 29, 30, 31, 32, 33, 34, 35,
        36, 37, 38, 39, 40, 41, 42, 43, 44
    };

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates a trade target selection view.
     */
    public TradeTargetSelectView() {
        super(TradeHubView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return trade target selection translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "trade_target_select_ui";
    }

    /**
     * Returns the layout used for this view.
     *
     * @return six-row target selector layout
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "         ",
            "         ",
            "         ",
            "         ",
            "         "
        };
    }

    /**
     * Renders trade target entries from local online players and persisted player rows.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final RDR plugin = this.rdr.get(render);
        final TradeService tradeService = plugin.getTradeService();
        final List<TargetEntry> targets = tradeService == null
            ? this.findLocalTargets(player, plugin)
            : this.findTargets(player, tradeService);

        render.layoutSlot('s', this.createSummaryItem(player, targets.size()));
        if (targets.isEmpty()) {
            render.slot(22, this.createEmptyItem(player));
            return;
        }

        final int slotLimit = Math.min(ENTRY_SLOTS.length, targets.size());
        for (int index = 0; index < slotLimit; index++) {
            final TargetEntry target = targets.get(index);
            final int slot = ENTRY_SLOTS[index];
            render.slot(slot, this.createTargetItem(player, target))
                .onClick(clickContext -> {
                    clickContext.setCancelled(true);
                    if (tradeService == null) {
                        new I18n.Builder("trade.message.unavailable", player).build().sendMessage();
                        return;
                    }

                    tradeService.createInviteWithSession(player, target.uuid())
                        .thenAccept(inviteResponse -> plugin.getScheduler().runSync(() -> {
                            final TradeService.InviteResult result = inviteResponse.result();
                            if (result == TradeService.InviteResult.SUCCESS) {
                                final UUID createdTradeUuid = inviteResponse.tradeUuid();
                                if (createdTradeUuid == null) {
                                    new I18n.Builder("trade.message.missing", player).build().sendMessage();
                                    clickContext.openForPlayer(TradeCurrentTradesView.class, Map.of("plugin", plugin));
                                    return;
                                }

                                clickContext.openForPlayer(
                                    TradeSessionView.class,
                                    Map.of("plugin", plugin, "trade_uuid", createdTradeUuid)
                                );

                                new I18n.Builder("trade.message.invite_sent", player)
                                    .withPlaceholder("target", target.name())
                                    .build()
                                    .sendMessage();
                                return;
                            }

                            switch (result) {
                                case DISABLED -> new I18n.Builder("trade.message.disabled", player).build().sendMessage();
                                case SELF_TARGET -> new I18n.Builder("trade.message.self_target", player).build().sendMessage();
                                case PARTICIPANT_BUSY -> new I18n.Builder("trade.message.participant_busy", player)
                                    .withPlaceholder("target", target.name())
                                    .build()
                                    .sendMessage();
                                case COOLDOWN -> new I18n.Builder("trade.message.invite_cooldown", player)
                                    .withPlaceholder("seconds", tradeService.getInviteCooldownRemainingSeconds(player.getUniqueId()))
                                    .build()
                                    .sendMessage();
                                case UNAVAILABLE -> new I18n.Builder("trade.message.unavailable", player).build().sendMessage();
                                case SUCCESS -> {
                                    // handled above
                                }
                            }
                            clickContext.openForPlayer(TradeCurrentTradesView.class, Map.of("plugin", plugin));
                        }));
                });
        }
    }

    /**
     * Cancels default inventory interaction for this menu.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private @NotNull List<TargetEntry> findTargets(
        final @NotNull Player viewer,
        final @NotNull TradeService tradeService
    ) {
        final List<TargetEntry> targets = new ArrayList<>();
        for (final TradeService.TradeTargetSnapshot targetSnapshot
            : tradeService.findTradeTargets(viewer.getUniqueId(), ENTRY_SLOTS.length)) {
            targets.add(new TargetEntry(
                targetSnapshot.targetUuid(),
                targetSnapshot.targetName(),
                targetSnapshot.presenceState(),
                targetSnapshot.serverId()
            ));
        }
        targets.sort(Comparator.comparing(TargetEntry::name, String.CASE_INSENSITIVE_ORDER));
        return targets;
    }

    private @NotNull List<TargetEntry> findLocalTargets(
        final @NotNull Player viewer,
        final @NotNull RDR plugin
    ) {
        final List<TargetEntry> targets = new ArrayList<>();
        final String localServerRouteId = plugin.getServerRouteId();
        for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer == null || onlinePlayer.getUniqueId().equals(viewer.getUniqueId())) {
                continue;
            }
            targets.add(new TargetEntry(
                onlinePlayer.getUniqueId(),
                onlinePlayer.getName(),
                TradeService.PresenceState.LOCAL_ONLINE,
                localServerRouteId
            ));
        }
        targets.sort(Comparator.comparing(TargetEntry::name, String.CASE_INSENSITIVE_ORDER));
        return targets;
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final int targetCount
    ) {
        return UnifiedBuilderFactory.item(Material.COMPASS)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholder("target_count", targetCount)
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createTargetItem(
        final @NotNull Player viewer,
        final @NotNull TargetEntry target
    ) {
        return UnifiedBuilderFactory.item(Material.NAME_TAG)
            .setName(this.i18n("entry.name", viewer)
                .withPlaceholder("target", target.name())
                .build()
                .component())
            .setLore(this.i18n("entry.lore", viewer)
                .withPlaceholders(Map.of(
                    "target", target.name(),
                    "presence_state", this.resolvePresenceStateLabel(target.presenceState()),
                    "server_id", this.normalizeServerIdForUi(target.serverId())
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEmptyItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("empty.name", player).build().component())
            .setLore(this.i18n("empty.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull String resolvePlayerName(final @NotNull UUID playerUuid) {
        final Player onlinePlayer = Bukkit.getPlayer(playerUuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }
        final var offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
        return offlinePlayer.getName() == null ? playerUuid.toString() : offlinePlayer.getName();
    }

    private @NotNull String resolvePresenceStateLabel(final @NotNull TradeService.PresenceState presenceState) {
        return switch (presenceState) {
            case LOCAL_ONLINE -> "Local Online";
            case REMOTE_ONLINE -> "Remote Online";
            case OFFLINE -> "Offline";
        };
    }

    private @NotNull String normalizeServerIdForUi(final @NotNull String serverId) {
        return serverId == null || serverId.isBlank() ? "offline" : serverId;
    }

    private record TargetEntry(
        UUID uuid,
        String name,
        TradeService.PresenceState presenceState,
        String serverId
    ) {
    }
}
