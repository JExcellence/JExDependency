package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.perk.RPerk;
import com.raindropcentral.rdq.database.entity.perk.RPlayerPerk;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing player perk state and ownership.
 *
 * Tracks which perks are owned by players, their enabled/disabled status,
 * and provides methods for granting and revoking perks.
 *
 * @author JExcellence
 * @version 1.0.1
 * @since TBD
 */
public class PlayerPerkStateService implements PerkStateService {

    private final RDQ plugin;

    public PlayerPerkStateService(@NotNull RDQ plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean playerOwnsPerk(@NotNull RDQPlayer player, @NotNull RPerk perk) {
        return plugin.getPlayerPerkRepository().findByAttributes(Map.of(
                "player", player,
                "perk", perk
        )) != null;
    }

    @Override
    public void grantPerk(@NotNull RDQPlayer player, @NotNull RPerk perk, boolean enabled) {
        if (!playerOwnsPerk(player, perk)) {
            RPlayerPerk playerPerk = new RPlayerPerk(player, perk, enabled);
            plugin.getPlayerPerkRepository().create(playerPerk);
        }
    }

    @Override
    public void revokePerk(@NotNull RDQPlayer player, @NotNull RPerk perk) {
        RPlayerPerk playerPerk = plugin.getPlayerPerkRepository().findByAttributes(Map.of(
                "player", player,
                "perk", perk
        ));
        if (playerPerk != null) {
            plugin.getPlayerPerkRepository().delete(playerPerk.getId());
        }
    }

    @Override
    public boolean isPerkEnabled(@NotNull RDQPlayer player, @NotNull RPerk perk) {
        RPlayerPerk playerPerk = plugin.getPlayerPerkRepository().findByAttributes(Map.of(
                "player", player,
                "perk", perk
        ));
        return playerPerk != null && playerPerk.isEnabled();
    }

    @Override
    public boolean enablePerk(@NotNull RDQPlayer player, @NotNull RPerk perk, int maxEnabledPerks) {
        RPlayerPerk playerPerk = plugin.getPlayerPerkRepository().findByAttributes(Map.of(
                "player", player,
                "perk", perk
        ));
        if (playerPerk == null || playerPerk.isEnabled()) {
            return false;
        }
        if (getEnabledPerks(player).size() >= maxEnabledPerks) {
            return false;
        }
        playerPerk.setEnabled(true);
        plugin.getPlayerPerkRepository().update(playerPerk);
        return true;
    }

    @Override
    public boolean disablePerk(@NotNull RDQPlayer player, @NotNull RPerk perk) {
        RPlayerPerk playerPerk = plugin.getPlayerPerkRepository().findByAttributes(Map.of(
                "player", player,
                "perk", perk
        ));
        if (playerPerk == null || !playerPerk.isEnabled()) {
            return false;
        }
        playerPerk.setEnabled(false);
        plugin.getPlayerPerkRepository().update(playerPerk);
        return true;
    }

    @Override
    @NotNull
    public List<RPerk> getOwnedPerks(@NotNull RDQPlayer player) {
        List<RPlayerPerk> playerPerks = plugin.getPlayerPerkRepository().findListByAttributes(Map.of(
                "player", player
        ));
        return playerPerks.stream()
                .map(RPlayerPerk::getPerk)
                .collect(Collectors.toList());
    }

    @Override
    @NotNull
    public List<RPerk> getEnabledPerks(@NotNull RDQPlayer player) {
        List<RPlayerPerk> playerPerks = plugin.getPlayerPerkRepository().findListByAttributes(Map.of(
                "player", player,
                "enabled", true
        ));
        return playerPerks.stream()
                .map(RPlayerPerk::getPerk)
                .collect(Collectors.toList());
    }

    @Override
    @Nullable
    public RDQPlayer getRDQPlayer(@NotNull Player player) {
        return plugin.getPlayerRepository().findByAttributes(Map.of("uniqueId", player.getUniqueId()));
    }

    @Override
    public void cleanupPlayerState(@NotNull UUID playerId) {
        final var initializationManager = plugin.getPerkInitializationManager();
        if (initializationManager == null || !initializationManager.isInitialized()) {
            return;
        }
        final var perkManager = initializationManager.getPerkManager();
        final Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            perkManager.clearPlayerState(player);
        } else {
            perkManager.clearPlayerState(playerId);
        }
    }
}
