package com.raindropcentral.rdq.command;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.command.completion.PerkNameCompleter;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Player command for perk system operations.
 * Command name: /pperk (aliases: perk, perks)
 */
@Command
public final class PPerk extends PlayerCommand {

    private static final List<String> SUBCOMMANDS = List.of("list", "activate", "deactivate", "info", "admin", "help");
    private static final List<String> ADMIN_SUBCOMMANDS = List.of("grant", "revoke", "reload");

    private final RDQCore core;
    private final PerkNameCompleter perkCompleter;

    public PPerk(final @NotNull PPerkSection section, final @NotNull RDQCore core) {
        super(section);
        this.core = core;
        this.perkCompleter = new PerkNameCompleter(core);
    }

    @Override
    protected void onPlayerInvocation(
            final @NotNull Player player,
            final @NotNull String alias,
            final @NotNull String[] args
    ) {
        if (hasNoPermission(player, EPerkPermission.USE)) {
            return;
        }

        if (args.length == 0) {
            listPerks(player);
            return;
        }

        var subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "list" -> listPerks(player);
            case "activate" -> handleActivate(player, args);
            case "deactivate" -> handleDeactivate(player, args);
            case "info" -> handleInfo(player, args);
            case "admin" -> handleAdmin(player, args);
            case "help" -> sendHelp(player);
            default -> TranslationService.create(TranslationKey.of("perk.error.unknown_subcommand"), player)
                    .withPrefix()
                    .send();
        }
    }

    private void listPerks(final @NotNull Player player) {
        if (hasNoPermission(player, EPerkPermission.LIST)) {
            return;
        }

        try {
            core.getViewFrame().open(com.raindropcentral.rdq.perk.view.PerkMainView.class, player);
        } catch (Exception e) {
            // Fallback to chat list if view fails
            core.getPerkService().getAvailablePerks(player.getUniqueId()).thenAccept(perks -> {
                if (perks.isEmpty()) {
                    TranslationService.create(TranslationKey.of("perk.list.empty"), player)
                            .withPrefix()
                            .send();
                    return;
                }

                TranslationService.create(TranslationKey.of("perk.list.header"), player)
                        .withPrefix()
                        .with("count", String.valueOf(perks.size()))
                        .send();

                perks.forEach(perk -> {
                    TranslationService.create(TranslationKey.of("perk.list.entry"), player)
                            .with("id", perk.id())
                            .with("category", perk.category())
                            .send();
                });
            });
        }
    }

    private void handleActivate(final @NotNull Player player, final @NotNull String[] args) {
        if (hasNoPermission(player, EPerkPermission.ACTIVATE)) {
            return;
        }

        if (args.length < 2) {
            TranslationService.create(TranslationKey.of("perk.activate.usage"), player)
                    .withPrefix()
                    .send();
            return;
        }

        var perkId = args[1].toLowerCase();

        core.getPerkService().getCooldownRemaining(player.getUniqueId(), perkId).thenCompose(cooldown -> {
            if (cooldown.isPresent()) {
                var remaining = cooldown.get();
                TranslationService.create(TranslationKey.of("perk.error.perk_on_cooldown"), player)
                        .withPrefix()
                        .with("remaining", formatDuration(remaining))
                        .send();
                return java.util.concurrent.CompletableFuture.completedFuture(false);
            }
            return core.getPerkService().activatePerk(player.getUniqueId(), perkId);
        }).thenAccept(success -> {
            if (success) {
                TranslationService.create(TranslationKey.of("perk.success.perk_activated"), player)
                        .withPrefix()
                        .with("perkName", perkId)
                        .send();
            }
        }).exceptionally(ex -> {
            TranslationService.create(TranslationKey.of("perk.error.activate_failed"), player)
                    .withPrefix()
                    .with("error", ex.getMessage())
                    .send();
            return null;
        });
    }

    private void handleDeactivate(final @NotNull Player player, final @NotNull String[] args) {
        if (hasNoPermission(player, EPerkPermission.DEACTIVATE)) {
            return;
        }

        if (args.length < 2) {
            TranslationService.create(TranslationKey.of("perk.deactivate.usage"), player)
                    .withPrefix()
                    .send();
            return;
        }

        var perkId = args[1].toLowerCase();

        core.getPerkService().deactivatePerk(player.getUniqueId(), perkId).thenAccept(success -> {
            if (success) {
                TranslationService.create(TranslationKey.of("perk.success.perk_deactivated"), player)
                        .withPrefix()
                        .with("perkName", perkId)
                        .send();
            } else {
                TranslationService.create(TranslationKey.of("perk.error.perk_not_active"), player)
                        .withPrefix()
                        .send();
            }
        });
    }

    private void handleInfo(final @NotNull Player player, final @NotNull String[] args) {
        if (hasNoPermission(player, EPerkPermission.INFO)) {
            return;
        }

        if (args.length < 2) {
            TranslationService.create(TranslationKey.of("perk.info.usage"), player)
                    .withPrefix()
                    .send();
            return;
        }

        var perkId = args[1].toLowerCase();

        core.getPerkService().getPerk(perkId).thenAccept(optPerk -> {
            if (optPerk.isEmpty()) {
                TranslationService.create(TranslationKey.of("perk.error.perk_not_found"), player)
                        .withPrefix()
                        .with("perkId", perkId)
                        .send();
                return;
            }

            var perk = optPerk.get();
            TranslationService.create(TranslationKey.of("perk.info.header"), player)
                    .withPrefix()
                    .with("id", perk.id())
                    .send();

            TranslationService.create(TranslationKey.of("perk.info.details"), player)
                    .with("category", perk.category())
                    .with("cooldown", String.valueOf(perk.cooldownSeconds()))
                    .with("duration", String.valueOf(perk.durationSeconds()))
                    .send();
        });
    }

    private void handleAdmin(final @NotNull Player player, final @NotNull String[] args) {
        if (hasNoPermission(player, EPerkPermission.ADMIN)) {
            return;
        }

        if (args.length < 2) {
            TranslationService.create(TranslationKey.of("perk.admin.usage"), player)
                    .withPrefix()
                    .send();
            return;
        }

        var adminCmd = args[1].toLowerCase();
        switch (adminCmd) {
            case "grant" -> handleGrant(player, args);
            case "revoke" -> handleRevoke(player, args);
            case "reload" -> handleReload(player);
            default -> TranslationService.create(TranslationKey.of("perk.admin.unknown"), player)
                    .withPrefix()
                    .send();
        }
    }

    private void handleGrant(final @NotNull Player player, final @NotNull String[] args) {
        if (hasNoPermission(player, EPerkPermission.GRANT)) {
            return;
        }

        if (args.length < 4) {
            TranslationService.create(TranslationKey.of("perk.admin.grant.usage"), player)
                    .withPrefix()
                    .send();
            return;
        }

        var targetName = args[2];
        var perkId = args[3];
        var target = Bukkit.getPlayer(targetName);

        if (target == null) {
            TranslationService.create(TranslationKey.of("perk.error.player_not_found"), player)
                    .withPrefix()
                    .with("playerName", targetName)
                    .send();
            return;
        }

        core.getPerkService().unlockPerk(target.getUniqueId(), perkId).thenAccept(success -> {
            if (success) {
                TranslationService.create(TranslationKey.of("perk.success.perk_granted"), player)
                        .withPrefix()
                        .with("perkName", perkId)
                        .with("playerName", target.getName())
                        .send();
            } else {
                TranslationService.create(TranslationKey.of("perk.error.grant_failed"), player)
                        .withPrefix()
                        .send();
            }
        });
    }

    private void handleRevoke(final @NotNull Player player, final @NotNull String[] args) {
        if (hasNoPermission(player, EPerkPermission.REVOKE)) {
            return;
        }

        if (args.length < 4) {
            TranslationService.create(TranslationKey.of("perk.admin.revoke.usage"), player)
                    .withPrefix()
                    .send();
            return;
        }

        var targetName = args[2];
        var perkId = args[3];

        TranslationService.create(TranslationKey.of("perk.success.perk_revoked"), player)
                .withPrefix()
                .with("perkName", perkId)
                .with("playerName", targetName)
                .send();
    }

    private void handleReload(final @NotNull Player player) {
        if (hasNoPermission(player, EPerkPermission.RELOAD)) {
            return;
        }

        core.getPerkService().reload().thenRun(() -> {
            TranslationService.create(TranslationKey.of("perk.success.reloaded"), player)
                    .withPrefix()
                    .send();
        }).exceptionally(ex -> {
            TranslationService.create(TranslationKey.of("perk.error.reload_failed"), player)
                    .withPrefix()
                    .with("error", ex.getMessage())
                    .send();
            return null;
        });
    }

    private void sendHelp(final @NotNull Player player) {
        TranslationService.create(TranslationKey.of("perk.help"), player)
                .withPrefix()
                .send();
    }

    private String formatDuration(java.time.Duration duration) {
        var seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        }
        var minutes = seconds / 60;
        var remainingSeconds = seconds % 60;
        if (minutes < 60) {
            return minutes + "m " + remainingSeconds + "s";
        }
        var hours = minutes / 60;
        var remainingMinutes = minutes % 60;
        return hours + "h " + remainingMinutes + "m";
    }

    @Override
    protected List<String> onPlayerTabCompletion(
            final @NotNull Player player,
            final @NotNull String alias,
            final @NotNull String[] args
    ) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2) {
            var sub = args[0].toLowerCase();
            if (sub.equals("activate") || sub.equals("deactivate") || sub.equals("info")) {
                return perkCompleter.complete(args[1]);
            }
            if (sub.equals("admin")) {
                return ADMIN_SUBCOMMANDS.stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .toList();
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("admin") &&
                (args[1].equalsIgnoreCase("grant") || args[1].equalsIgnoreCase("revoke"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("admin") &&
                (args[1].equalsIgnoreCase("grant") || args[1].equalsIgnoreCase("revoke"))) {
            return perkCompleter.complete(args[3]);
        }

        return List.of();
    }
}
