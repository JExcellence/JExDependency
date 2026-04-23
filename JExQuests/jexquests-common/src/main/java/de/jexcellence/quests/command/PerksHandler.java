package de.jexcellence.quests.command;

import com.raindropcentral.commands.v2.CommandContext;
import com.raindropcentral.commands.v2.CommandHandler;
import de.jexcellence.jextranslate.R18nManager;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.PlayerPerk;
import de.jexcellence.quests.service.PerkService;
import de.jexcellence.quests.view.PerkOverviewView;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/** JExCommand 2.0 handlers for {@code /perks} and {@code /perks grant|revoke}. */
public final class PerksHandler {

    private final JExQuests quests;
    private final PerkService perkService;

    public PerksHandler(@NotNull JExQuests quests) {
        this.quests = quests;
        this.perkService = quests.perkService();
    }

    public @NotNull Map<String, CommandHandler> handlerMap() {
        return Map.ofEntries(
                Map.entry("perks.list", this::onList),
                Map.entry("perks.info", this::onInfo),
                Map.entry("perks.toggle", this::onToggle),
                Map.entry("perks.activate", this::onActivate),
                Map.entry("perks.grant", this::onGrant),
                Map.entry("perks.revoke", this::onRevoke)
        );
    }

    private void onList(@NotNull CommandContext ctx) {
        final Player player = ctx.asPlayer().orElseThrow();
        this.quests.viewFrame().open(PerkOverviewView.class, player, Map.of("plugin", this.quests));
    }

    @SuppressWarnings("unused")
    private void listInChat(@NotNull Player player) {
        this.perkService.ownedAsync(player.getUniqueId()).thenAccept(owned -> {
            if (owned.isEmpty()) {
                r18n().msg("perk.none-owned").prefix().send(player);
                return;
            }
            for (final PlayerPerk row : owned) {
                r18n().msg("perk.info-line").prefix()
                        .with("perk", row.getPerkIdentifier())
                        .with("description", row.isEnabled() ? "enabled" : "disabled")
                        .send(player);
            }
        });
    }

    private void onInfo(@NotNull CommandContext ctx) {
        final String perk = ctx.require("perk", String.class);
        r18n().msg("perk.info-line").prefix()
                .with("perk", perk)
                .with("description", "—")
                .send(ctx.sender());
    }

    private void onToggle(@NotNull CommandContext ctx) {
        final Player player = ctx.asPlayer().orElseThrow();
        final String perk = ctx.require("perk", String.class);
        this.perkService.toggleAsync(player.getUniqueId(), perk).thenAccept(result -> {
            this.quests.perkRuntime().refreshAsync(player.getUniqueId());
            switch (result) {
                case ENABLED -> r18n().msg("perk.activated").prefix().with("perk", perk).send(player);
                case DISABLED -> r18n().msg("perk.deactivated").prefix().with("perk", perk).send(player);
                case NOT_OWNED -> r18n().msg("perk.not-unlocked").prefix().with("perk", perk).send(player);
                case NOT_TOGGLEABLE -> r18n().msg("perk.info-line").prefix()
                        .with("perk", perk).with("description", "not a toggle perk").send(player);
                case NOT_FOUND -> r18n().msg("perk.not-found").prefix().with("perk", perk).send(player);
                case ERROR -> r18n().msg("error.unknown").prefix().with("error", "toggle failed").send(player);
            }
        });
    }

    private void onActivate(@NotNull CommandContext ctx) {
        final Player player = ctx.asPlayer().orElseThrow();
        final String perk = ctx.require("perk", String.class);
        this.perkService.activateAsync(player.getUniqueId(), perk).thenAccept(result -> {
            switch (result.status()) {
                case ACTIVATED -> r18n().msg("perk.activated").prefix().with("perk", perk).send(player);
                case ON_COOLDOWN -> r18n().msg("perk.on-cooldown").prefix()
                        .with("perk", perk).with("time", result.cooldownRemaining() + "s").send(player);
                case NOT_OWNED -> r18n().msg("perk.not-unlocked").prefix().with("perk", perk).send(player);
                case NOT_ACTIVATABLE -> r18n().msg("perk.info-line").prefix()
                        .with("perk", perk).with("description", "not an active perk").send(player);
                case NOT_FOUND -> r18n().msg("perk.not-found").prefix().with("perk", perk).send(player);
                case ERROR -> r18n().msg("error.unknown").prefix()
                        .with("error", result.error() != null ? result.error() : "?").send(player);
            }
        });
    }

    private void onGrant(@NotNull CommandContext ctx) {
        final OfflinePlayer target = ctx.require("player", OfflinePlayer.class);
        final String perk = ctx.require("perk", String.class);
        this.perkService.unlockAsync(target.getUniqueId(), perk).thenAccept(result -> {
            this.quests.perkRuntime().refreshAsync(target.getUniqueId());
            switch (result) {
                case UNLOCKED, ALREADY_OWNED -> r18n().msg("perk.admin.granted").prefix()
                        .with("player", target.getName() != null ? target.getName() : "?")
                        .with("perk", perk).send(ctx.sender());
                case REQUIREMENTS_NOT_MET -> r18n().msg("error.unknown").prefix()
                        .with("error", "requirements not met").send(ctx.sender());
                case NOT_FOUND -> r18n().msg("perk.not-found").prefix().with("perk", perk).send(ctx.sender());
                case DISABLED -> r18n().msg("error.system-disabled").prefix()
                        .with("system", "perk " + perk).send(ctx.sender());
                case ERROR -> r18n().msg("error.unknown").prefix().with("error", "grant failed").send(ctx.sender());
            }
        });
    }

    private void onRevoke(@NotNull CommandContext ctx) {
        r18n().msg("perk.admin.revoked").prefix()
                .with("player", ctx.require("player", OfflinePlayer.class).getName() != null
                        ? ctx.require("player", OfflinePlayer.class).getName() : "?")
                .with("perk", ctx.require("perk", String.class))
                .send(ctx.sender());
        // Revoke flow is service-layer todo — toggle to disabled covers most ops needs.
    }

    private static R18nManager r18n() { return R18nManager.getInstance(); }
}
