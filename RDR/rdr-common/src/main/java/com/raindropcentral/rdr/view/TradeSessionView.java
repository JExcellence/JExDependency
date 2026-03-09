package com.raindropcentral.rdr.view;

import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.database.entity.RTradeSession;
import com.raindropcentral.rdr.database.entity.TradeSessionStatus;
import com.raindropcentral.rdr.service.TradeInboxPollService;
import com.raindropcentral.rdr.service.TradeService;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
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
 * Interactive escrow trade session view for one active trade UUID.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class TradeSessionView extends BaseView {

    private static final int[] SELF_OFFER_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30, 37, 38, 39};
    private static final int[] PARTNER_OFFER_SLOTS = {14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43};

    private final State<RDR> rdr = initialState("plugin");
    private final State<UUID> tradeUuid = initialState("trade_uuid");
    private final Set<UUID> pendingCompletionActors = ConcurrentHashMap.newKeySet();

    /**
     * Creates a trade session view.
     */
    public TradeSessionView() {
        super(TradeCurrentTradesView.class);
    }

    /**
     * Returns translation namespace for this view.
     *
     * @return trade session translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "trade_session_ui";
    }

    /**
     * Returns the layout for this view.
     *
     * @return six-row trade session layout
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
     * Disables automatic filler slots for this custom session display.
     *
     * @return {@code false}
     */
    @Override
    protected boolean shouldAutoFill() {
        return false;
    }

    /**
     * Registers this open session with the polling service.
     *
     * @param open open context
     */
    @Override
    public void onOpen(final @NotNull OpenContext open) {
        super.onOpen(open);
        final RDR plugin = this.rdr.get(open);
        final UUID watchedTradeUuid = this.tradeUuid.get(open);
        final TradeInboxPollService pollService = plugin.getTradeInboxPollService();
        if (pollService != null && watchedTradeUuid != null) {
            pollService.watchTradeSession(watchedTradeUuid, open.getPlayer().getUniqueId());
        }
    }

    /**
     * Unregisters this open session from the polling service.
     *
     * @param close close context
     */
    @Override
    public void onClose(final @NotNull CloseContext close) {
        final RDR plugin = this.rdr.get(close);
        final UUID watchedTradeUuid = this.tradeUuid.get(close);
        final TradeInboxPollService pollService = plugin.getTradeInboxPollService();
        if (pollService != null && watchedTradeUuid != null) {
            pollService.unwatchTradeSession(watchedTradeUuid, close.getPlayer().getUniqueId());
        }
        this.pendingCompletionActors.remove(close.getPlayer().getUniqueId());
    }

    /**
     * Forces a full re-render when navigating back into this session view.
     *
     * @param origin previous context
     * @param target resumed context
     */
    @Override
    public void onResume(
        final @NotNull Context origin,
        final @NotNull Context target
    ) {
        target.update();
    }

    /**
     * Renders current session offers and controls.
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
        final UUID activeTradeUuid = this.tradeUuid.get(render);
        if (tradeService == null || activeTradeUuid == null) {
            render.slot(22, this.createUnavailableItem(player));
            return;
        }

        final RTradeSession session = tradeService.findSession(activeTradeUuid);
        if (session == null || !session.hasParticipant(player.getUniqueId())) {
            render.slot(22, this.createUnavailableItem(player));
            return;
        }

        final UUID playerUuid = player.getUniqueId();
        final Map<Integer, ItemStack> selfOffers = session.getOfferItemsForParticipant(playerUuid);
        final UUID counterpartyUuid = session.getCounterpartyUuid(playerUuid);
        final Map<Integer, ItemStack> partnerOffers = counterpartyUuid == null
            ? Map.of()
            : session.getOfferItemsForParticipant(counterpartyUuid);
        final Map<String, Double> selfCurrency = session.getOfferCurrencyForParticipant(playerUuid);
        final Map<String, Double> partnerCurrency = counterpartyUuid == null
            ? Map.of()
            : session.getOfferCurrencyForParticipant(counterpartyUuid);

        final int slotCount = Math.min(this.rdr.get(render).getDefaultConfig().getTradeMaxOfferSlots(), SELF_OFFER_SLOTS.length);
        render.layoutSlot('s', this.createSummaryItem(player, session, selfCurrency, partnerCurrency));
        this.renderOfferSlots(render, player, plugin, tradeService, session, selfOffers, partnerOffers, slotCount);
        this.renderActionButtons(render, player, plugin, tradeService, session);
    }

    /**
     * Cancels default item movement for this menu.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        if (!click.getClickedContainer().isEntityContainer()) {
            click.setCancelled(true);
            return;
        }
        if (!click.isShiftClick()) {
            click.setCancelled(false);
            return;
        }
        click.setCancelled(true);

        final RDR plugin = this.rdr.get(click);
        final TradeService tradeService = plugin.getTradeService();
        final UUID activeTradeUuid = this.tradeUuid.get(click);
        if (tradeService == null || activeTradeUuid == null) {
            new I18n.Builder("trade.message.unavailable", click.getPlayer()).build().sendMessage();
            return;
        }

        final RTradeSession session = tradeService.findSession(activeTradeUuid);
        if (session == null || !session.hasParticipant(click.getPlayer().getUniqueId())) {
            new I18n.Builder("trade.message.missing", click.getPlayer()).build().sendMessage();
            return;
        }
        this.handleShiftOfferInsert(click, plugin, tradeService, session);
    }

    private void handleShiftOfferInsert(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RDR plugin,
        final @NotNull TradeService tradeService,
        final @NotNull RTradeSession session
    ) {
        if (this.isSelfOfferLocked(session, clickContext.getPlayer().getUniqueId())) {
            this.sendSessionResultMessage(clickContext.getPlayer(), TradeService.SessionResult.INVALID_STATE);
            return;
        }

        final ItemStack clickedItem = clickContext.getClickOrigin().getCurrentItem();
        if (isAir(clickedItem)) {
            return;
        }

        final UUID playerUuid = clickContext.getPlayer().getUniqueId();
        final int slotCount = Math.min(
            plugin.getDefaultConfig().getTradeMaxOfferSlots(),
            SELF_OFFER_SLOTS.length
        );
        final Map<Integer, ItemStack> selfOffers = session.getOfferItemsForParticipant(playerUuid);
        final int offerSlot = findFirstFreeOfferSlot(selfOffers, slotCount);
        if (offerSlot < 0) {
            this.sendSessionResultMessage(clickContext.getPlayer(), TradeService.SessionResult.OFFER_FULL);
            return;
        }

        final ItemStack offeredItem = clickedItem.clone();
        clickContext.getPlayer().getInventory().removeItem(offeredItem.clone());
        final int renderedSlot = SELF_OFFER_SLOTS[offerSlot];
        clickContext.getPlayer().getOpenInventory().getTopInventory().setItem(renderedSlot, offeredItem.clone());
        tradeService.setItemOfferSlot(
                clickContext.getPlayer(),
                session.getTradeUuid(),
                -1L,
                offerSlot,
                offeredItem
            )
            .thenAccept(result -> plugin.getScheduler().runSync(() -> {
                if (result != TradeService.SessionResult.SUCCESS) {
                    this.restoreItemToPlayer(clickContext.getPlayer(), offeredItem);
                    clickContext.getPlayer().getOpenInventory().getTopInventory().setItem(
                        renderedSlot,
                        this.createEmptyOfferItem(clickContext.getPlayer(), true, offerSlot)
                    );
                }
                this.handleResultAndRefresh(clickContext.getPlayer(), plugin, session.getTradeUuid(), result);
            }));
    }

    private void renderOfferSlots(
        final @NotNull RenderContext render,
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull TradeService tradeService,
        final @NotNull RTradeSession session,
        final @NotNull Map<Integer, ItemStack> selfOffers,
        final @NotNull Map<Integer, ItemStack> partnerOffers,
        final int slotCount
    ) {
        for (int index = 0; index < slotCount; index++) {
            final int offerIndex = index;
            final int selfSlot = SELF_OFFER_SLOTS[index];
            final int partnerSlot = PARTNER_OFFER_SLOTS[index];
            final ItemStack selfItem = selfOffers.get(offerIndex);
            final ItemStack partnerItem = partnerOffers.get(offerIndex);

            render.slot(
                    selfSlot,
                    isAir(selfItem) ? this.createEmptyOfferItem(player, true, offerIndex) : selfItem.clone()
                )
                .onClick(clickContext -> {
                    clickContext.setCancelled(true);
                    if (clickContext.isShiftClick()) {
                        return;
                    }
                    if (this.isSelfOfferLocked(session, player.getUniqueId())) {
                        this.sendSessionResultMessage(clickContext.getPlayer(), TradeService.SessionResult.INVALID_STATE);
                        return;
                    }
                    if (clickContext.isRightClick()) {
                        if (isAir(selfItem)) {
                            return;
                        }
                        tradeService.removeItemOfferSlot(player, session.getTradeUuid(), -1L, offerIndex)
                            .thenAccept(result -> plugin.getScheduler().runSync(() -> {
                                if (result == TradeService.SessionResult.SUCCESS) {
                                    player.getOpenInventory().getTopInventory().setItem(
                                        selfSlot,
                                        this.createEmptyOfferItem(player, true, offerIndex)
                                    );
                                }
                                this.handleResultAndRefresh(player, plugin, session.getTradeUuid(), result);
                            }));
                        return;
                    }
                    if (!clickContext.isLeftClick() || !isAir(selfItem)) {
                        return;
                    }

                    final ItemStack cursorItem = clickContext.getClickOrigin().getCursor();
                    if (isAir(cursorItem)) {
                        return;
                    }

                    final ItemStack offeredItem = cursorItem.clone();
                    clickContext.getPlayer().setItemOnCursor(null);
                    clickContext.getPlayer().getOpenInventory().getTopInventory().setItem(selfSlot, offeredItem.clone());
                    tradeService.setItemOfferSlot(
                            player,
                            session.getTradeUuid(),
                            -1L,
                            offerIndex,
                            offeredItem
                        )
                        .thenAccept(result -> plugin.getScheduler().runSync(() -> {
                            if (result != TradeService.SessionResult.SUCCESS) {
                                this.restoreItemToPlayer(player, offeredItem);
                                player.getOpenInventory().getTopInventory().setItem(
                                    selfSlot,
                                    this.createEmptyOfferItem(player, true, offerIndex)
                                );
                            }
                            this.handleResultAndRefresh(player, plugin, session.getTradeUuid(), result);
                        }));
                });

            if (partnerItem == null || partnerItem.isEmpty()) {
                render.slot(partnerSlot, this.createEmptyOfferItem(player, false, offerIndex));
            } else {
                render.slot(partnerSlot, partnerItem.clone())
                    .onClick(clickContext -> clickContext.setCancelled(true));
            }
        }
    }

    private void renderActionButtons(
        final @NotNull RenderContext render,
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull TradeService tradeService,
        final @NotNull RTradeSession session
    ) {
        render.slot(47, this.createCurrencyMenuButton(player))
            .onClick(clickContext -> {
                clickContext.setCancelled(true);
                clickContext.openForPlayer(
                    TradeCurrencySelectView.class,
                    Map.of("plugin", plugin, "trade_uuid", session.getTradeUuid())
                );
            });

        if (session.getStatus() == TradeSessionStatus.INVITED) {
            if (session.getPartnerUuid().equals(player.getUniqueId())) {
                render.slot(49, this.createInviteAcceptButton(player))
                    .onClick(clickContext -> {
                        clickContext.setCancelled(true);
                        this.updateActionButtonMaterial(player, this.createReadyButton(player, false, false));
                        tradeService.acceptInvite(player, session.getTradeUuid(), -1L)
                            .thenAccept(result -> plugin.getScheduler().runSync(() ->
                                this.handleResultAndRefresh(player, plugin, session.getTradeUuid(), result)
                            ));
                    });
            } else {
                render.slot(49, this.createInvitePendingButton(player))
                    .onClick(clickContext -> clickContext.setCancelled(true));
            }
        } else if (session.getStatus() == TradeSessionStatus.ACTIVE) {
            render.slot(49, this.createReadyButton(player, session))
                .onClick(clickContext -> {
                    clickContext.setCancelled(true);
                    final boolean ready = !clickContext.isRightClick();
                    final boolean partnerAccepted = this.isPartnerAccepted(session, player.getUniqueId());
                    this.updateActionButtonMaterial(player, this.createReadyButton(player, ready, partnerAccepted));
                    tradeService.setReady(player, session.getTradeUuid(), -1L, ready)
                        .thenAccept(result -> plugin.getScheduler().runSync(() ->
                            this.handleResultAndRefresh(player, plugin, session.getTradeUuid(), result)
                        ));
                });
        } else if (session.getStatus() == TradeSessionStatus.COMPLETING) {
            render.slot(49, this.createConfirmButton(player, session))
                .onClick(clickContext -> {
                    clickContext.setCancelled(true);
                    if (!this.pendingCompletionActors.add(player.getUniqueId())) {
                        return;
                    }
                    final boolean partnerAccepted = this.isPartnerAccepted(session, player.getUniqueId());
                    this.updateActionButtonMaterial(player, this.createConfirmButton(player, true, partnerAccepted));
                    clickContext.openForPlayer(TradeHubView.class, Map.of("plugin", plugin));
                    tradeService.confirmCompletion(player, session.getTradeUuid(), -1L)
                        .thenAccept(result -> plugin.getScheduler().runSync(() ->
                            this.handleResultAndRefresh(player, plugin, session.getTradeUuid(), result)
                        ));
                });
        }

        render.slot(53, this.createCancelButton(player))
            .onClick(clickContext -> {
                clickContext.setCancelled(true);
                tradeService.cancelSession(player, session.getTradeUuid(), -1L)
                    .thenAccept(result -> plugin.getScheduler().runSync(() ->
                        this.handleResultAndRefresh(player, plugin, session.getTradeUuid(), result)
                    ));
            });
    }

    private void handleResultAndRefresh(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull UUID tradeUuid,
        final @NotNull TradeService.SessionResult result
    ) {
        this.pendingCompletionActors.remove(player.getUniqueId());
        this.sendSessionResultMessage(player, result);
        if (!player.isOnline()) {
            return;
        }

        if (result == TradeService.SessionResult.COMPLETED || result == TradeService.SessionResult.WAITING_FOR_PARTNER) {
            this.openTradeHubNextTick(player, plugin);
            return;
        }
        this.openTradeSessionNextTick(player, plugin, tradeUuid);
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
        final @NotNull RTradeSession session,
        final @NotNull Map<String, Double> selfCurrency,
        final @NotNull Map<String, Double> partnerCurrency
    ) {
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "status", this.resolveStatusDisplay(session.getStatus()),
                    "trade_uuid", session.getTradeUuid(),
                    "revision", session.getRevision(),
                    "self_vault", selfCurrency.getOrDefault("vault", 0.0D),
                    "partner_vault", partnerCurrency.getOrDefault("vault", 0.0D),
                    "self_currency_count", selfCurrency.size(),
                    "partner_currency_count", partnerCurrency.size(),
                    "self_currency_total", formatCurrencyTotal(selfCurrency),
                    "partner_currency_total", formatCurrencyTotal(partnerCurrency)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull String resolveStatusDisplay(
        final @NotNull TradeSessionStatus status
    ) {
        return switch (status) {
            case INVITED -> "Invited";
            case ACTIVE -> "Active";
            case COMPLETING -> "Completing";
            case COMPLETED -> "Completed";
            case CANCELED -> "Canceled";
            case EXPIRED -> "Expired";
        };
    }

    private @NotNull ItemStack createEmptyOfferItem(
        final @NotNull Player player,
        final boolean selfSide,
        final int offerIndex
    ) {
        return UnifiedBuilderFactory.item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(this.i18n(selfSide ? "slots.self_empty_name" : "slots.partner_empty_name", player)
                .withPlaceholder("slot", offerIndex + 1)
                .build()
                .component())
            .setLore(this.i18n(selfSide ? "slots.self_empty_lore" : "slots.partner_empty_lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createCurrencyMenuButton(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.EMERALD)
            .setName(this.i18n("actions.currency_manage_secondary.name", player).build().component())
            .setLore(this.i18n("actions.currency_manage_secondary.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createInviteAcceptButton(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.LIME_CONCRETE)
            .setName(this.i18n("actions.invite_accept.name", player).build().component())
            .setLore(this.i18n("actions.invite_accept.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createInvitePendingButton(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.CLOCK)
            .setName(this.i18n("actions.invite_waiting.name", player).build().component())
            .setLore(this.i18n("actions.invite_waiting.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createReadyButton(
        final @NotNull Player player,
        final @NotNull RTradeSession session
    ) {
        final boolean selfAccepted = this.isSelfAccepted(session, player.getUniqueId());
        final boolean partnerAccepted = this.isPartnerAccepted(session, player.getUniqueId());
        return this.createReadyButton(player, selfAccepted, partnerAccepted);
    }

    private @NotNull ItemStack createReadyButton(
        final @NotNull Player player,
        final boolean selfAccepted,
        final boolean partnerAccepted
    ) {
        final String keyBase = selfAccepted ? "actions.ready_unlock" : "actions.ready_lock";
        return UnifiedBuilderFactory.item(selfAccepted ? Material.LIME_DYE : Material.YELLOW_DYE)
            .setName(this.i18n(keyBase + ".name", player).build().component())
            .setLore(this.i18n(keyBase + ".lore", player)
                .withPlaceholders(Map.of(
                    "self_accepted", this.resolveAcceptanceState(selfAccepted),
                    "partner_accepted", this.resolveAcceptanceState(partnerAccepted)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createConfirmButton(
        final @NotNull Player player,
        final @NotNull RTradeSession session
    ) {
        final boolean noCompletionConfirmsYet = session.getStatus() == TradeSessionStatus.COMPLETING
            && !session.isInitiatorReady()
            && !session.isPartnerReady();
        final boolean selfAccepted = noCompletionConfirmsYet || this.isSelfAccepted(session, player.getUniqueId());
        final boolean partnerAccepted = noCompletionConfirmsYet || this.isPartnerAccepted(session, player.getUniqueId());
        return this.createConfirmButton(player, selfAccepted, partnerAccepted);
    }

    private @NotNull ItemStack createConfirmButton(
        final @NotNull Player player,
        final boolean selfAccepted,
        final boolean partnerAccepted
    ) {
        return UnifiedBuilderFactory.item(selfAccepted ? Material.EMERALD : Material.DIAMOND)
            .setName(this.i18n("actions.confirm.name", player).build().component())
            .setLore(this.i18n("actions.confirm.lore", player)
                .withPlaceholders(Map.of(
                    "self_accepted", this.resolveAcceptanceState(selfAccepted),
                    "partner_accepted", this.resolveAcceptanceState(partnerAccepted)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createCancelButton(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("actions.cancel.name", player).build().component())
            .setLore(this.i18n("actions.cancel.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private boolean isSelfAccepted(
        final @NotNull RTradeSession session,
        final @NotNull UUID participantUuid
    ) {
        return session.isInitiator(participantUuid) ? session.isInitiatorReady() : session.isPartnerReady();
    }

    private boolean isPartnerAccepted(
        final @NotNull RTradeSession session,
        final @NotNull UUID participantUuid
    ) {
        return session.isInitiator(participantUuid) ? session.isPartnerReady() : session.isInitiatorReady();
    }

    private boolean isSelfOfferLocked(
        final @NotNull RTradeSession session,
        final @NotNull UUID participantUuid
    ) {
        if (session.getStatus() == TradeSessionStatus.COMPLETING) {
            return true;
        }
        return this.isSelfAccepted(session, participantUuid);
    }

    private @NotNull String resolveAcceptanceState(final boolean accepted) {
        return accepted ? "Accepted" : "Waiting";
    }

    private void updateActionButtonMaterial(
        final @NotNull Player player,
        final @NotNull ItemStack actionButton
    ) {
        if (!player.isOnline()) {
            return;
        }

        final var topInventory = player.getOpenInventory().getTopInventory();
        if (topInventory.getSize() <= 49) {
            return;
        }
        topInventory.setItem(49, actionButton);
    }

    private void restoreItemToPlayer(
        final @NotNull Player player,
        final @NotNull ItemStack itemStack
    ) {
        if (isAir(itemStack)) {
            return;
        }
        final Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack.clone());
        if (!leftovers.isEmpty()) {
            for (final ItemStack leftover : leftovers.values()) {
                if (!isAir(leftover)) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover.clone());
                }
            }
        }
    }

    private static boolean isAir(final @Nullable ItemStack itemStack) {
        return itemStack == null || itemStack.isEmpty() || itemStack.getType().isAir();
    }

    private static int findFirstFreeOfferSlot(
        final @NotNull Map<Integer, ItemStack> offers,
        final int slotCount
    ) {
        for (int index = 0; index < slotCount; index++) {
            if (isAir(offers.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private static @NotNull String formatCurrencyTotal(final @NotNull Map<String, Double> currencyOffers) {
        double total = 0.0D;
        for (final Double amount : currencyOffers.values()) {
            if (amount == null) {
                continue;
            }
            total += Math.max(0.0D, amount);
        }
        return String.format(Locale.US, "%.2f", total);
    }

    private @NotNull ItemStack createUnavailableItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private void openTradeSessionNextTick(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull UUID tradeUuid
    ) {
        if (plugin.getScheduler() != null) {
            plugin.getScheduler().runDelayed(() -> {
                if (!player.isOnline()) {
                    return;
                }
                plugin.getViewFrame().open(
                    TradeSessionView.class,
                    player,
                    Map.of("plugin", plugin, "trade_uuid", tradeUuid)
                );
            }, 1L);
            return;
        }
        plugin.getViewFrame().open(TradeSessionView.class, player, Map.of("plugin", plugin, "trade_uuid", tradeUuid));
    }

    private void openTradeHubNextTick(
        final @NotNull Player player,
        final @NotNull RDR plugin
    ) {
        if (plugin.getScheduler() != null) {
            plugin.getScheduler().runDelayed(() -> {
                if (!player.isOnline()) {
                    return;
                }
                plugin.getViewFrame().open(TradeHubView.class, player, Map.of("plugin", plugin));
            }, 1L);
            return;
        }
        plugin.getViewFrame().open(TradeHubView.class, player, Map.of("plugin", plugin));
    }
}
