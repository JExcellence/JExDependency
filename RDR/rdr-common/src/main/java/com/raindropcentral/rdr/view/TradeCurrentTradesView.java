package com.raindropcentral.rdr.view;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.database.entity.RTradeSession;
import com.raindropcentral.rdr.database.entity.TradeSessionStatus;
import com.raindropcentral.rdr.service.TradeService;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * View listing current non-terminal trade sessions for the viewer.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class TradeCurrentTradesView extends BaseView {

    private static final int[] ENTRY_SLOTS = {
        9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, 26,
        27, 28, 29, 30, 31, 32, 33, 34, 35,
        36, 37, 38, 39, 40, 41, 42, 43, 44
    };

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates a current-trades browser view.
     */
    public TradeCurrentTradesView() {
        super(TradeHubView.class);
    }

    /**
     * Returns translation namespace for this view.
     *
     * @return current trades translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "trade_current_ui";
    }

    /**
     * Returns layout used by this view.
     *
     * @return six-row current trade layout
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
     * Renders current trade entries and interaction controls.
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
        final List<RTradeSession> sessions = tradeService == null
            ? List.of()
            : tradeService.findNonTerminalSessions(player.getUniqueId());

        render.layoutSlot('s', this.createSummaryItem(player, sessions.size()))
            .onClick(clickContext -> {
                clickContext.setCancelled(true);
                clickContext.openForPlayer(TradeInboxView.class, Map.of("plugin", plugin));
            });
        render.slot(49, this.createInboxButton(player))
            .onClick(clickContext -> {
                clickContext.setCancelled(true);
                clickContext.openForPlayer(TradeInboxView.class, Map.of("plugin", plugin));
            });

        if (sessions.isEmpty()) {
            render.slot(22, this.createEmptyItem(player));
            return;
        }

        final int slotLimit = Math.min(ENTRY_SLOTS.length, sessions.size());
        for (int index = 0; index < slotLimit; index++) {
            final RTradeSession session = sessions.get(index);
            final int slot = ENTRY_SLOTS[index];
            render.slot(slot, this.createSessionItem(player, session))
                .onClick(clickContext -> {
                    clickContext.setCancelled(true);
                    if (tradeService == null) {
                        return;
                    }
                    this.handleSessionClick(clickContext, plugin, session);
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

    private void handleSessionClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RDR plugin,
        final @NotNull RTradeSession session
    ) {
        clickContext.openForPlayer(
            TradeSessionView.class,
            Map.of("plugin", plugin, "trade_uuid", session.getTradeUuid())
        );
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final int sessionCount
    ) {
        return UnifiedBuilderFactory.item(Material.CLOCK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholder("session_count", sessionCount)
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createSessionItem(
        final @NotNull Player viewer,
        final @NotNull RTradeSession session
    ) {
        final UUID counterpartyUuid = session.getCounterpartyUuid(viewer.getUniqueId());
        final String counterpartyName = this.resolvePlayerName(counterpartyUuid);
        final String statusDisplay = this.resolveStatusDisplay(session.getStatus());

        return UnifiedBuilderFactory.item(this.resolveStatusMaterial(session.getStatus()))
            .setName(this.i18n("entry.name", viewer)
                .withPlaceholders(Map.of(
                    "counterparty", counterpartyName,
                    "status", statusDisplay
                ))
                .build()
                .component())
            .setLore(this.i18n("entry.lore", viewer)
                .withPlaceholders(Map.of(
                    "counterparty", counterpartyName,
                    "status", statusDisplay,
                    "trade_uuid", session.getTradeUuid()
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

    private @NotNull ItemStack createInboxButton(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.CHEST)
            .setName(this.i18n("actions.inbox.name", player).build().component())
            .setLore(this.i18n("actions.inbox.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull Material resolveStatusMaterial(final @NotNull TradeSessionStatus status) {
        return switch (status) {
            case INVITED -> Material.PAPER;
            case ACTIVE -> Material.EMERALD;
            case COMPLETING -> Material.DIAMOND;
            case COMPLETED -> Material.LIME_STAINED_GLASS_PANE;
            case CANCELED -> Material.RED_STAINED_GLASS_PANE;
            case EXPIRED -> Material.GRAY_STAINED_GLASS_PANE;
        };
    }

    private @NotNull String resolveStatusDisplay(final @NotNull TradeSessionStatus status) {
        return switch (status) {
            case INVITED -> "Invited";
            case ACTIVE -> "Active";
            case COMPLETING -> "Completing";
            case COMPLETED -> "Completed";
            case CANCELED -> "Canceled";
            case EXPIRED -> "Expired";
        };
    }

    private @NotNull String resolvePlayerName(final @Nullable UUID playerUuid) {
        if (playerUuid == null) {
            return "Unknown";
        }
        final Player onlinePlayer = org.bukkit.Bukkit.getPlayer(playerUuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }
        final var offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(playerUuid);
        return offlinePlayer.getName() == null ? playerUuid.toString() : offlinePlayer.getName();
    }
}
