package com.raindropcentral.rdq.command;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rdq.RDQCore;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Player command for main RDQ operations.
 * Command name: /prdq (aliases: rdq, raindropquests)
 */
@Command
public final class PRdq extends PlayerCommand {

    private static final List<String> SUBCOMMANDS = List.of("rank", "bounty", "perk", "reload", "help");

    private final RDQCore core;

    public PRdq(final @NotNull PRdqSection section, final @NotNull RDQCore core) {
        super(section);
        this.core = core;
    }

    @Override
    protected void onPlayerInvocation(
            final @NotNull Player player,
            final @NotNull String alias,
            final @NotNull String[] args
    ) {
        if (hasNoPermission(player, ERdqPermission.USE)) {
            return;
        }

        if (!core.isInitialized()) {
            TranslationService.create(TranslationKey.of("rdq.error.not_initialized"), player)
                    .withPrefix()
                    .send();
            return;
        }

        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        var subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "rank" -> openRankGui(player);
            case "bounty" -> openBountyGui(player);
            case "perk" -> openPerkGui(player);
            case "reload" -> handleReload(player);
            case "help" -> sendHelp(player);
            default -> TranslationService.create(TranslationKey.of("rdq.error.unknown_subcommand"), player)
                    .withPrefix()
                    .send();
        }
    }

    private void openRankGui(final @NotNull Player player) {
        if (hasNoPermission(player, ERdqPermission.USE)) {
            return;
        }
        try {
            core.getViewFrame().open(com.raindropcentral.rdq.rank.view.RankMainView.class, player);
        } catch (Exception e) {
            core.getPlugin().getLogger().warning("Failed to open RankMainView: " + e.getMessage());
            e.printStackTrace();
            TranslationService.create(TranslationKey.of("rdq.error.view_failed"), player)
                    .withPrefix()
                    .send();
        }
    }

    private void openBountyGui(final @NotNull Player player) {
        if (hasNoPermission(player, ERdqPermission.USE)) {
            return;
        }
        try {
            core.getViewFrame().open(
                com.raindropcentral.rdq.bounty.view.BountyMainView.class,
                player,
                java.util.Map.of("rdqCore", core)
            );
        } catch (Exception e) {
            core.getPlugin().getLogger().warning("Failed to open BountyMainView: " + e.getMessage());
            e.printStackTrace();
            TranslationService.create(TranslationKey.of("rdq.error.view_failed"), player)
                    .withPrefix()
                    .send();
        }
    }

    private void openPerkGui(final @NotNull Player player) {
        if (hasNoPermission(player, ERdqPermission.USE)) {
            return;
        }
        try {
            core.getViewFrame().open(com.raindropcentral.rdq.perk.view.PerkMainView.class, player);
        } catch (Exception e) {
            core.getPlugin().getLogger().warning("Failed to open PerkMainView: " + e.getMessage());
            e.printStackTrace();
            TranslationService.create(TranslationKey.of("rdq.error.view_failed"), player)
                    .withPrefix()
                    .send();
        }
    }

    private void handleReload(final @NotNull Player player) {
        if (hasNoPermission(player, ERdqPermission.RELOAD)) {
            return;
        }

        var rankReload = core.getRankService().reload();
        var perkReload = core.getPerkService().reload();

        rankReload.thenCombine(perkReload, (v1, v2) -> null)
                .thenRun(() -> TranslationService.create(TranslationKey.of("rdq.success.reloaded"), player)
                        .withPrefix()
                        .send())
                .exceptionally(ex -> {
                    TranslationService.create(TranslationKey.of("rdq.error.reload_failed"), player)
                            .withPrefix()
                            .with("error", ex.getMessage())
                            .send();
                    return null;
                });
    }

    private void sendHelp(final @NotNull Player player) {
        TranslationService.create(TranslationKey.of("rdq.help"), player)
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
        return List.of();
    }
}
