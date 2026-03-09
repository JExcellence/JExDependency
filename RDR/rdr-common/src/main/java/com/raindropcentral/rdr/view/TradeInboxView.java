package com.raindropcentral.rdr.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.database.entity.RTradeDelivery;
import com.raindropcentral.rdr.database.entity.RTradeSession;
import com.raindropcentral.rdr.service.TradeService;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Inbox view for pending trade invites and claimable deliveries.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class TradeInboxView extends BaseView {

    private static final int[] ENTRY_SLOTS = {
        9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, 26,
        27, 28, 29, 30, 31, 32, 33, 34, 35,
        36, 37, 38, 39, 40, 41, 42, 43, 44
    };

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates a trade inbox view.
     */
    public TradeInboxView() {
        super(TradeHubView.class);
    }

    /**
     * Returns translation namespace for this view.
     *
     * @return trade inbox translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "trade_inbox_ui";
    }

    /**
     * Returns layout used for this view.
     *
     * @return six-row inbox layout
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
     * Renders invite and delivery inbox entries.
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
        final List<RTradeSession> invites = tradeService == null ? List.of() : tradeService.findPendingInvites(player.getUniqueId());
        final List<RTradeDelivery> deliveries = tradeService == null ? List.of() : tradeService.findPendingDeliveries(player.getUniqueId());

        render.layoutSlot('s', this.createSummaryItem(player, invites.size(), deliveries.size()));
        final List<InboxEntry> entries = new ArrayList<>();
        for (final RTradeSession invite : invites) {
            entries.add(InboxEntry.invite(invite));
        }
        for (final RTradeDelivery delivery : deliveries) {
            entries.add(InboxEntry.delivery(delivery));
        }

        if (entries.isEmpty()) {
            render.slot(22, this.createEmptyItem(player));
            return;
        }

        final int slotLimit = Math.min(ENTRY_SLOTS.length, entries.size());
        for (int index = 0; index < slotLimit; index++) {
            final InboxEntry entry = entries.get(index);
            final int slot = ENTRY_SLOTS[index];
            render.slot(slot, this.createEntryItem(player, entry))
                .onClick(clickContext -> {
                    clickContext.setCancelled(true);
                    if (tradeService == null) {
                        new I18n.Builder("trade.message.unavailable", player).build().sendMessage();
                        return;
                    }
                    this.handleEntryClick(clickContext, plugin, tradeService, player, entry);
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

    private void handleEntryClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RDR plugin,
        final @NotNull TradeService tradeService,
        final @NotNull Player player,
        final @NotNull InboxEntry entry
    ) {
        if (entry.invite() != null) {
            final RTradeSession invite = entry.invite();
            clickContext.openForPlayer(
                TradeSessionView.class,
                Map.of("plugin", plugin, "trade_uuid", invite.getTradeUuid())
            );
            return;
        }

        if (entry.delivery() != null && entry.delivery().getId() != null) {
            tradeService.claimDelivery(player, entry.delivery().getId())
                .thenAccept(result -> plugin.getScheduler().runSync(() -> {
                    this.sendSessionResultMessage(player, result);
                    clickContext.openForPlayer(TradeInboxView.class, Map.of("plugin", plugin));
                }));
        }
    }

    private void sendSessionResultMessage(
        final @NotNull Player player,
        final @NotNull TradeService.SessionResult result
    ) {
        final String key = switch (result) {
            case SUCCESS -> "trade.message.success";
            case WAITING_FOR_PARTNER -> "trade.message.waiting_for_partner";
            case COMPLETED -> "trade.message.completed";
            case NO_ITEM_IN_HAND -> "trade.message.no_item_in_hand";
            case OFFER_FULL -> "trade.message.offer_full";
            case INSUFFICIENT_FUNDS -> "trade.message.insufficient_funds";
            case MISSING -> "trade.message.missing";
            case FORBIDDEN -> "trade.message.forbidden";
            case INVALID_STATE -> "trade.message.invalid_state";
            case STALE -> "trade.message.stale";
            case EXPIRED -> "trade.message.expired";
            case UNAVAILABLE -> "trade.message.unavailable";
        };
        new I18n.Builder(key, player).build().sendMessage();
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final int inviteCount,
        final int deliveryCount
    ) {
        return UnifiedBuilderFactory.item(Material.CHEST)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "invite_count", inviteCount,
                    "delivery_count", deliveryCount
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEntryItem(
        final @NotNull Player viewer,
        final @NotNull InboxEntry entry
    ) {
        if (entry.invite() != null) {
            final RTradeSession invite = entry.invite();
            return UnifiedBuilderFactory.item(Material.PAPER)
                .setName(this.i18n("entry.invite.name", viewer)
                    .withPlaceholder("from", this.resolvePlayerName(invite.getInitiatorUuid()))
                    .build()
                    .component())
                .setLore(this.i18n("entry.invite.lore", viewer)
                    .withPlaceholders(Map.of(
                        "from", this.resolvePlayerName(invite.getInitiatorUuid()),
                        "trade_uuid", invite.getTradeUuid()
                    ))
                    .build()
                    .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
        }

        final RTradeDelivery delivery = entry.delivery();
        return UnifiedBuilderFactory.item(Material.CHEST_MINECART)
            .setName(this.i18n("entry.delivery.name", viewer)
                .withPlaceholder("trade_uuid", delivery.getTradeUuid())
                .build()
                .component())
            .setLore(this.i18n("entry.delivery.lore", viewer)
                .withPlaceholders(Map.of(
                    "trade_uuid", delivery.getTradeUuid(),
                    "item_count", delivery.getItemPayload().size(),
                    "currency_count", delivery.getCurrencyPayload().size()
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

    private @NotNull String resolvePlayerName(final @NotNull java.util.UUID playerUuid) {
        final Player onlinePlayer = org.bukkit.Bukkit.getPlayer(playerUuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }
        final var offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(playerUuid);
        return offlinePlayer.getName() == null ? playerUuid.toString() : offlinePlayer.getName();
    }

    private record InboxEntry(
        RTradeSession invite,
        RTradeDelivery delivery
    ) {
        static @NotNull InboxEntry invite(final @NotNull RTradeSession invite) {
            return new InboxEntry(invite, null);
        }

        static @NotNull InboxEntry delivery(final @NotNull RTradeDelivery delivery) {
            return new InboxEntry(null, delivery);
        }
    }
}
