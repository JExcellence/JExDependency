package com.raindropcentral.rdq.bounty.announcement;

import com.raindropcentral.rdq.bounty.Bounty;
import com.raindropcentral.rdq.bounty.ClaimResult;
import com.raindropcentral.rdq.bounty.config.BountyConfig;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.logging.Logger;

public final class BountyAnnouncementService {

    private static final Logger LOGGER = Logger.getLogger(BountyAnnouncementService.class.getName());
    private static final int NEARBY_RADIUS = 100;

    private final BountyConfig config;

    public BountyAnnouncementService(@NotNull BountyConfig config) {
        this.config = config;
    }

    public void announceCreation(@NotNull Player placer, @NotNull Player target, @NotNull Bounty bounty) {
        if (!config.announceOnCreate()) {
            return;
        }

        var message = "<gold>[Bounty] <yellow>" + placer.getName() + " <gray>placed a bounty of <yellow>" + bounty.amount() + " " + bounty.currency() + " <gray>on <red>" + target.getName();

        getRecipients(placer, target).forEach(player -> {
            player.sendMessage(message);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        });

        target.sendMessage("<red>[Bounty] <gray>A bounty of <yellow>" + bounty.amount() + " " + bounty.currency() + " <gray>has been placed on you by <yellow>" + placer.getName());
        target.playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.0f);

        LOGGER.info("Bounty created: " + placer.getName() + " -> " + target.getName() + " for " + bounty.amount());
    }

    public void announceClaim(@NotNull Player hunter, @NotNull Player target, @NotNull ClaimResult result) {
        if (!config.announceOnClaim()) {
            return;
        }

        var message = "<gold>[Bounty] <green>" + hunter.getName() + " <gray>claimed the bounty on <red>" + target.getName() + " <gray>for <yellow>" + result.reward() + " " + result.bounty().currency();

        getRecipients(hunter, target).forEach(player -> {
            player.sendMessage(message);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        });

        hunter.sendMessage("<green>[Bounty] <gray>You claimed <yellow>" + result.reward() + " " + result.bounty().currency() + " <gray>for killing <red>" + target.getName());
        hunter.playSound(hunter.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        LOGGER.info("Bounty claimed: " + hunter.getName() + " killed " + target.getName() + " for " + result.reward());
    }


    public void announceExpiration(@NotNull Bounty bounty) {
        var placer = Bukkit.getPlayer(bounty.placerId());
        if (placer != null && placer.isOnline()) {
            placer.sendMessage("<yellow>[Bounty] <gray>Your bounty on <red>" + getPlayerName(bounty.targetId()) + " <gray>has expired. <green>" + bounty.amount() + " " + bounty.currency() + " <gray>refunded.");
            placer.playSound(placer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }

        var target = Bukkit.getPlayer(bounty.targetId());
        if (target != null && target.isOnline()) {
            target.sendMessage("<green>[Bounty] <gray>The bounty on you from <yellow>" + getPlayerName(bounty.placerId()) + " <gray>has expired.");
        }
    }

    public void announceCancellation(@NotNull Bounty bounty) {
        var target = Bukkit.getPlayer(bounty.targetId());
        if (target != null && target.isOnline()) {
            target.sendMessage("<green>[Bounty] <gray>The bounty on you from <yellow>" + getPlayerName(bounty.placerId()) + " <gray>has been cancelled.");
        }
    }

    @NotNull
    private Collection<? extends Player> getRecipients(@NotNull Player placer, @NotNull Player target) {
        if (config.isAnnouncementScopeServer()) {
            return Bukkit.getOnlinePlayers();
        }

        if (config.isAnnouncementScopeNearby()) {
            return placer.getWorld().getNearbyPlayers(placer.getLocation(), NEARBY_RADIUS);
        }

        return java.util.List.of();
    }

    @NotNull
    private String getPlayerName(@NotNull java.util.UUID playerId) {
        var player = Bukkit.getPlayer(playerId);
        if (player != null) {
            return player.getName();
        }
        var offline = Bukkit.getOfflinePlayer(playerId);
        return offline.getName() != null ? offline.getName() : "Unknown";
    }
}
