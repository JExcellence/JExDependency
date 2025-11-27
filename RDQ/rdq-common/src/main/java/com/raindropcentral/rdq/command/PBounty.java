package com.raindropcentral.rdq.command;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.bounty.BountyRequest;
import com.raindropcentral.rdq.command.completion.BountyTargetCompleter;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Player command for bounty system operations.
 * Command name: /pbounty (aliases: bounty, bounties)
 */
@Command
public final class PBounty extends PlayerCommand {

    private static final List<String> SUBCOMMANDS = List.of("create", "list", "cancel", "leaderboard", "my", "admin", "help");
    private static final List<String> ADMIN_SUBCOMMANDS = List.of("remove", "reload");

    private final RDQCore core;
    private final BountyTargetCompleter bountyTargetCompleter;

    public PBounty(final @NotNull PBountySection section, final @NotNull RDQCore core) {
        super(section);
        this.core = core;
        this.bountyTargetCompleter = new BountyTargetCompleter(core);
    }

    @Override
    protected void onPlayerInvocation(
            final @NotNull Player player,
            final @NotNull String alias,
            final @NotNull String[] args
    ) {
        if (hasNoPermission(player, EBountyPermission.USE)) {
            return;
        }

        if (args.length == 0) {
            openBountyList(player);
            return;
        }

        var subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "create" -> handleCreate(player, args);
            case "list" -> openBountyList(player);
            case "cancel" -> handleCancel(player, args);
            case "leaderboard" -> showLeaderboard(player);
            case "my" -> showMyBounties(player);
            case "admin" -> handleAdmin(player, args);
            case "help" -> sendHelp(player);
            default -> TranslationService.create(TranslationKey.of("bounty.error.unknown_subcommand"), player)
                    .withPrefix()
                    .send();
        }
    }

    private void handleCreate(final @NotNull Player player, final @NotNull String[] args) {
        if (hasNoPermission(player, EBountyPermission.CREATE)) {
            return;
        }

        if (args.length < 3) {
            TranslationService.create(TranslationKey.of("bounty.create.usage"), player)
                    .withPrefix()
                    .send();
            return;
        }

        var targetName = args[1];
        var target = Bukkit.getPlayer(targetName);

        if (target == null) {
            TranslationService.create(TranslationKey.of("bounty.error.player_not_found"), player)
                    .withPrefix()
                    .with("playerName", targetName)
                    .send();
            return;
        }

        // Check if self-targeting is allowed
        if (target.getUniqueId().equals(player.getUniqueId()) && !core.getBountyConfig().selfTargetAllowed()) {
            TranslationService.create(TranslationKey.of("bounty.error.self_target"), player)
                    .withPrefix()
                    .send();
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(args[2]);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            TranslationService.create(TranslationKey.of("bounty.error.invalid_amount"), player)
                    .withPrefix()
                    .send();
            return;
        }

        var request = new BountyRequest(player.getUniqueId(), target.getUniqueId(), amount, "default");

        core.getBountyService().createBounty(request).thenAccept(bounty -> {
            TranslationService.create(TranslationKey.of("bounty.success.bounty_created"), player)
                    .withPrefix()
                    .with("amount", amount.toPlainString())
                    .with("targetName", target.getName())
                    .send();
        }).exceptionally(ex -> {
            TranslationService.create(TranslationKey.of("bounty.error.create_failed"), player)
                    .withPrefix()
                    .with("error", ex.getMessage())
                    .send();
            return null;
        });
    }

    private void openBountyList(final @NotNull Player player) {
        if (hasNoPermission(player, EBountyPermission.LIST)) {
            return;
        }

        try {
            core.getViewFrame().open(
                com.raindropcentral.rdq.bounty.view.BountyMainView.class,
                player,
                java.util.Map.of("rdqCore", core)
            );
        } catch (Exception e) {
            // Fallback to chat list if view fails
            core.getBountyService().getActiveBounties().thenAccept(bounties -> {
                if (bounties.isEmpty()) {
                    TranslationService.create(TranslationKey.of("bounty.list.empty"), player)
                            .withPrefix()
                            .send();
                    return;
                }

                TranslationService.create(TranslationKey.of("bounty.list.header"), player)
                        .withPrefix()
                        .with("count", String.valueOf(bounties.size()))
                        .send();

                bounties.stream().limit(10).forEach(bounty -> {
                    TranslationService.create(TranslationKey.of("bounty.list.entry"), player)
                            .with("target", bounty.target().name())
                            .with("amount", bounty.amount().toPlainString())
                            .with("placer", bounty.placer().name())
                            .send();
                });
            });
        }
    }

    private void handleCancel(final @NotNull Player player, final @NotNull String[] args) {
        if (hasNoPermission(player, EBountyPermission.CANCEL)) {
            return;
        }

        if (args.length < 2) {
            TranslationService.create(TranslationKey.of("bounty.cancel.usage"), player)
                    .withPrefix()
                    .send();
            return;
        }

        long bountyId;
        try {
            bountyId = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            TranslationService.create(TranslationKey.of("bounty.error.invalid_id"), player)
                    .withPrefix()
                    .send();
            return;
        }

        core.getBountyService().cancelBounty(player.getUniqueId(), bountyId).thenAccept(success -> {
            if (success) {
                TranslationService.create(TranslationKey.of("bounty.success.bounty_cancelled"), player)
                        .withPrefix()
                        .send();
            } else {
                TranslationService.create(TranslationKey.of("bounty.error.cancel_failed"), player)
                        .withPrefix()
                        .send();
            }
        });
    }

    private void showLeaderboard(final @NotNull Player player) {
        if (hasNoPermission(player, EBountyPermission.LEADERBOARD)) {
            return;
        }

        core.getBountyService().getLeaderboard(10).thenAccept(stats -> {
            TranslationService.create(TranslationKey.of("bounty.leaderboard.header"), player)
                    .withPrefix()
                    .send();

            var rank = 1;
            for (var stat : stats) {
                TranslationService.create(TranslationKey.of("bounty.leaderboard.entry"), player)
                        .with("rank", String.valueOf(rank++))
                        .with("player", stat.playerName())
                        .with("kills", String.valueOf(stat.bountiesClaimed()))
                        .with("earned", stat.totalEarned().toPlainString())
                        .send();
            }
        });
    }

    private void showMyBounties(final @NotNull Player player) {
        if (hasNoPermission(player, EBountyPermission.LIST)) {
            return;
        }

        var placedFuture = core.getBountyService().getBountiesPlacedBy(player.getUniqueId());
        var targetedFuture = core.getBountyService().getBountiesOnPlayer(player.getUniqueId());

        placedFuture.thenCombine(targetedFuture, (placed, targeted) -> {
            TranslationService.create(TranslationKey.of("bounty.my.header"), player)
                    .withPrefix()
                    .send();

            if (!placed.isEmpty()) {
                TranslationService.create(TranslationKey.of("bounty.my.placed_header"), player).send();
                placed.forEach(b -> TranslationService.create(TranslationKey.of("bounty.my.placed_entry"), player)
                        .with("target", b.target().name())
                        .with("amount", b.amount().toPlainString())
                        .send());
            }

            if (!targeted.isEmpty()) {
                TranslationService.create(TranslationKey.of("bounty.my.targeted_header"), player).send();
                targeted.forEach(b -> TranslationService.create(TranslationKey.of("bounty.my.targeted_entry"), player)
                        .with("placer", b.placer().name())
                        .with("amount", b.amount().toPlainString())
                        .send());
            }

            return null;
        });
    }

    private void handleAdmin(final @NotNull Player player, final @NotNull String[] args) {
        if (hasNoPermission(player, EBountyPermission.ADMIN)) {
            return;
        }

        if (args.length < 2) {
            TranslationService.create(TranslationKey.of("bounty.admin.usage"), player)
                    .withPrefix()
                    .send();
            return;
        }

        var adminCmd = args[1].toLowerCase();
        switch (adminCmd) {
            case "remove" -> handleRemove(player, args);
            case "reload" -> handleReload(player);
            default -> TranslationService.create(TranslationKey.of("bounty.admin.unknown"), player)
                    .withPrefix()
                    .send();
        }
    }

    private void handleRemove(final @NotNull Player player, final @NotNull String[] args) {
        if (hasNoPermission(player, EBountyPermission.REMOVE)) {
            return;
        }

        if (args.length < 3) {
            TranslationService.create(TranslationKey.of("bounty.admin.remove.usage"), player)
                    .withPrefix()
                    .send();
            return;
        }

        TranslationService.create(TranslationKey.of("bounty.success.bounty_removed"), player)
                .withPrefix()
                .send();
    }

    private void handleReload(final @NotNull Player player) {
        if (hasNoPermission(player, EBountyPermission.RELOAD)) {
            return;
        }

        try {
            var config = core.reloadBountyConfig();
            TranslationService.create(TranslationKey.of("bounty.success.reloaded"), player)
                    .withPrefix()
                    .with("selfTargetAllowed", String.valueOf(config.selfTargetAllowed()))
                    .send();
        } catch (Exception e) {
            TranslationService.create(TranslationKey.of("bounty.error.reload_failed"), player)
                    .withPrefix()
                    .with("error", e.getMessage())
                    .send();
        }
    }

    private void sendHelp(final @NotNull Player player) {
        TranslationService.create(TranslationKey.of("bounty.help"), player)
                .withPrefix()
                .send();
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
            if (args[0].equalsIgnoreCase("create")) {
                return Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !p.getUniqueId().equals(player.getUniqueId()))
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            }
            if (args[0].equalsIgnoreCase("admin")) {
                return ADMIN_SUBCOMMANDS.stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .toList();
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return List.of("100", "500", "1000", "5000", "10000");
        }

        return List.of();
    }
}
