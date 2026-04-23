package de.jexcellence.quests.command;

import com.raindropcentral.commands.v2.CommandContext;
import com.raindropcentral.commands.v2.CommandHandler;
import de.jexcellence.jextranslate.R18nManager;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.Bounty;
import de.jexcellence.quests.service.BountyService;
import de.jexcellence.quests.view.BountyOverviewView;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/** JExCommand 2.0 handlers for {@code /bounty}. */
public final class BountyHandler {

    private final JExQuests quests;
    private final BountyService bountyService;

    public BountyHandler(@NotNull JExQuests quests) {
        this.quests = quests;
        this.bountyService = quests.bountyService();
    }

    /**
     * Returns the command handler map for bounty commands.
     */
    public @NotNull Map<String, CommandHandler> handlerMap() {
        return Map.ofEntries(
                Map.entry("bounty.list", this::onList),
                Map.entry("bounty.place", this::onPlace),
                Map.entry("bounty.cancel", this::onCancel),
                Map.entry("bounty.post", this::onPost)
        );
    }

    /**
     * GUI entry point — opens the {@link de.jexcellence.quests.view.BountyCreationView}
     * with the target and currency bound as initial state. The amount
     * is adjusted interactively with +/- buttons inside the view, so
     * this command intentionally doesn't accept one.
     */
    private void onPost(@NotNull CommandContext ctx) {
        final Player issuer = ctx.asPlayer().orElseThrow();
        final OfflinePlayer target = ctx.require("target", OfflinePlayer.class);
        final String currency = ctx.get("currency", String.class).orElse("coins");
        if (target.getUniqueId().equals(issuer.getUniqueId())) {
            r18n().msg("bounty.self-ban").prefix().send(issuer);
            return;
        }
        this.quests.viewFrame().open(
                de.jexcellence.quests.view.BountyCreationView.class,
                issuer,
                Map.of(
                        "plugin", this.quests,
                        "target", target,
                        "currency", currency
                )
        );
    }

    private void onList(@NotNull CommandContext ctx) {
        ctx.asPlayer().ifPresentOrElse(
                player -> this.quests.viewFrame().open(BountyOverviewView.class, player, Map.of("plugin", this.quests)),
                () -> listInChat(ctx)
        );
    }

    @SuppressWarnings("unused")
    private void listInChat(@NotNull CommandContext ctx) {
        this.bountyService.activeAsync().thenAccept(active -> {
            if (active.isEmpty()) {
                r18n().msg("bounty.list-empty").prefix().send(ctx.sender());
                return;
            }
            for (final Bounty bounty : active) {
                r18n().msg("bounty.list-entry").prefix()
                        .with("target", bounty.getTargetUuid().toString())
                        .with("amount", String.valueOf(bounty.getAmount()))
                        .with("issuer", bounty.getIssuerUuid().toString())
                        .send(ctx.sender());
            }
        });
    }

    private void onPlace(@NotNull CommandContext ctx) {
        final Player issuer = ctx.asPlayer().orElseThrow();
        final OfflinePlayer target = ctx.require("target", OfflinePlayer.class);
        final double amount = ctx.require("amount", Double.class);
        final String currency = ctx.get("currency", String.class).orElse("coins");

        if (target.getUniqueId().equals(issuer.getUniqueId())) {
            r18n().msg("bounty.self-ban").prefix().send(issuer);
            return;
        }
        this.bountyService.placeAsync(target.getUniqueId(), issuer.getUniqueId(), currency, amount).thenAccept(bounty -> {
            if (bounty == null) {
                r18n().msg("error.unknown").prefix().with("error", "place failed").send(issuer);
                return;
            }
            r18n().msg("bounty.placed").prefix()
                    .with("amount", String.valueOf(amount))
                    .with("target", target.getName() != null ? target.getName() : "?")
                    .send(issuer);
        });
    }

    private void onCancel(@NotNull CommandContext ctx) {
        final Player issuer = ctx.asPlayer().orElseThrow();
        final OfflinePlayer target = ctx.require("target", OfflinePlayer.class);
        this.bountyService.cancelAsync(target.getUniqueId()).thenAccept(ok -> {
            if (ok) r18n().msg("bounty.cancelled").prefix()
                    .with("target", target.getName() != null ? target.getName() : "?")
                    .with("amount", "refunded").send(issuer);
            else r18n().msg("bounty.not-found").prefix()
                    .with("target", target.getName() != null ? target.getName() : "?").send(issuer);
        });
    }

    private static R18nManager r18n() { return R18nManager.getInstance(); }
}
