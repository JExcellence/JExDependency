package de.jexcellence.quests.view;

import de.jexcellence.jexplatform.view.BaseView;
import de.jexcellence.jextranslate.R18nManager;
import de.jexcellence.quests.JExQuests;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * GUI flow for posting a bounty on another player.
 *
 * <p>The target and currency are immutable after open (set via initial
 * state — typically from {@code /bounty post <target> [currency]}).
 * The issuer scrolls the amount up or down with step-sized buttons,
 * then clicks emerald to submit or barrier to cancel. Submission goes
 * through {@link de.jexcellence.quests.service.BountyService#placeAsync}.
 *
 * <p>Kept deliberately simple — item rewards were part of the legacy
 * RDQ flow but JExQuests' bounty model is currency-only (the entity
 * carries a single {@code currency/amount} tuple). Adding an item
 * layer would mean extending the schema; if that's desired, the
 * place to start is adding a {@code BountyReward} table and changing
 * {@link de.jexcellence.quests.database.entity.Bounty} from a value
 * object to a parent row.
 */
public class BountyCreationView extends BaseView {

    private static final int SLOT_TARGET = 11;
    private static final int SLOT_DEC_10 = 19;
    private static final int SLOT_DEC_100 = 20;
    private static final int SLOT_AMOUNT = 22;
    private static final int SLOT_INC_100 = 24;
    private static final int SLOT_INC_10 = 25;
    private static final int SLOT_CONFIRM = 31;
    private static final int SLOT_CANCEL = 33;

    private final State<JExQuests> plugin = initialState("plugin");
    private final State<OfflinePlayer> target = initialState("target");
    private final State<String> currency = initialState("currency");
    private final MutableState<Double> amount = mutableState(100.0);

    public BountyCreationView() {
        super(BountyOverviewView.class);
    }

    @Override
    protected String translationKey() {
        return "bounty_creation_ui";
    }

    @Override
    protected int size() {
        return 5;
    }

    @Override
    protected String[] layout() {
        return new String[]{
                "         ",
                "   T     ",
                "MmS A PpC",
                "   V K   ",
                "         "
        };
    }

    @Override
    protected void onRender(@NotNull RenderContext render, @NotNull Player player) {
        final OfflinePlayer target = this.target.get(render);
        final String currency = this.currency.get(render);
        final String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();

        // Target preview — skull with the target's head.
        render.slot(SLOT_TARGET, createItem(
                Material.PLAYER_HEAD,
                i18n("target.name", player)
                        .withPlaceholder("target", targetName)
                        .build().component(),
                i18n("target.lore", player)
                        .withPlaceholder("target", targetName)
                        .build().children()
        ));

        // Amount, re-rendered whenever it changes.
        render.slot(SLOT_AMOUNT, createItem(
                        Material.GOLD_INGOT,
                        i18n("amount.name", player)
                                .withPlaceholder("amount", formatAmount(this.amount.get(render)))
                                .withPlaceholder("currency", currency)
                                .build().component(),
                        i18n("amount.lore", player)
                                .withPlaceholders(Map.of(
                                        "amount", formatAmount(this.amount.get(render)),
                                        "currency", currency
                                ))
                                .build().children()))
                .updateOnStateChange(this.amount);

        // ±100 / ±10 / ±1 arrows. The layout above only reserves four
        // arrow slots; add/remove rows here if you want finer control.
        renderAdjustButton(render, player, SLOT_DEC_100, -100.0, "decrease.hundred");
        renderAdjustButton(render, player, SLOT_DEC_10, -10.0, "decrease.ten");
        renderAdjustButton(render, player, SLOT_INC_10, 10.0, "increase.ten");
        renderAdjustButton(render, player, SLOT_INC_100, 100.0, "increase.hundred");

        render.slot(SLOT_CONFIRM, createItem(
                Material.EMERALD_BLOCK,
                i18n("confirm.name", player).build().component(),
                i18n("confirm.lore", player)
                        .withPlaceholders(Map.of(
                                "target", targetName,
                                "amount", formatAmount(this.amount.get(render)),
                                "currency", currency
                        ))
                        .build().children()
        )).onClick(click -> {
            final var quests = this.plugin.get(click);
            final double value = this.amount.get(click);
            if (value <= 0.0) {
                R18nManager.getInstance().msg("bounty_creation_ui.invalid-amount").prefix().send(player);
                return;
            }
            quests.bountyService().placeAsync(
                    target.getUniqueId(), player.getUniqueId(), currency, value
            ).thenAccept(bounty -> {
                if (bounty == null) {
                    R18nManager.getInstance().msg("bounty_creation_ui.failed").prefix().send(player);
                    return;
                }
                R18nManager.getInstance().msg("bounty_creation_ui.placed").prefix()
                        .with("target", targetName)
                        .with("amount", formatAmount(value))
                        .with("currency", currency)
                        .send(player);
            });
            click.closeForPlayer();
        });

        render.slot(SLOT_CANCEL, createItem(
                Material.BARRIER,
                i18n("cancel.name", player).build().component(),
                i18n("cancel.lore", player).build().children()
        )).onClick(click -> click.closeForPlayer());
    }

    private void renderAdjustButton(
            @NotNull RenderContext render, @NotNull Player player, int slot,
            double delta, @NotNull String translationSuffix
    ) {
        render.slot(slot, createItem(
                delta > 0 ? Material.LIME_DYE : Material.RED_DYE,
                i18n("adjust." + translationSuffix + ".name", player)
                        .withPlaceholder("delta", formatAmount(Math.abs(delta)))
                        .build().component(),
                i18n("adjust." + translationSuffix + ".lore", player)
                        .withPlaceholder("delta", formatAmount(Math.abs(delta)))
                        .build().children()
        )).onClick(click -> {
            final double next = Math.max(0.0, this.amount.get(click) + delta);
            this.amount.set(next, click);
        });
    }

    private static @NotNull String formatAmount(double value) {
        // Economy amounts are doubles; render as integer when whole.
        if (value == Math.floor(value)) return String.valueOf((long) value);
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
