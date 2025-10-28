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
 * Default implementation of PerkStateService.
 *
 * @author qodo
 * @version 1.0.1
 * @since TBD
 */
public class DefaultPerkStateService implements PerkStateService {

    private final RDQ rdq;

    public DefaultPerkStateService(@NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    @Override
    public boolean playerOwnsPerk(@NotNull RDQPlayer player, @NotNull RPerk perk) {
        return rdq.getPlayerPerkRepository().findByAttributes(Map.of(
                "player", player,
                "perk", perk
        )) != null;
    }

    @Override
    public void grantPerk(@NotNull RDQPlayer player, @NotNull RPerk perk, boolean enabled) {
        if (!playerOwnsPerk(player, perk)) {
            RPlayerPerk playerPerk = new RPlayerPerk(player, perk, enabled);
            rdq.getPlayerPerkRepository().create(playerPerk);
        }
    }

    @Override
    public void revokePerk(@NotNull RDQPlayer player, @NotNull RPerk perk) {
        RPlayerPerk playerPerk = rdq.getPlayerPerkRepository().findByAttributes(Map.of(
                "player", player,
                "perk", perk
        ));
        if (playerPerk != null) {
            rdq.getPlayerPerkRepository().delete(playerPerk.getId());
        }
    }

    @Override
    public boolean isPerkEnabled(@NotNull RDQPlayer player, @NotNull RPerk perk) {
        RPlayerPerk playerPerk = rdq.getPlayerPerkRepository().findByAttributes(Map.of(
                "player", player,
                "perk", perk
        ));
        return playerPerk != null && playerPerk.isEnabled();
    }

    @Override
    public boolean enablePerk(@NotNull RDQPlayer player, @NotNull RPerk perk, int maxEnabledPerks) {
        RPlayerPerk playerPerk = rdq.getPlayerPerkRepository().findByAttributes(Map.of(
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
        rdq.getPlayerPerkRepository().update(playerPerk);
        return true;
    }

    @Override
    public boolean disablePerk(@NotNull RDQPlayer player, @NotNull RPerk perk) {
        RPlayerPerk playerPerk = rdq.getPlayerPerkRepository().findByAttributes(Map.of(
                "player", player,
                "perk", perk
        ));
        if (playerPerk == null || !playerPerk.isEnabled()) {
            return false;
        }
        playerPerk.setEnabled(false);
        rdq.getPlayerPerkRepository().update(playerPerk);
        return true;
    }

    @Override
    @NotNull
    public List<RPerk> getOwnedPerks(@NotNull RDQPlayer player) {
        List<RPlayerPerk> playerPerks = rdq.getPlayerPerkRepository().findListByAttributes(Map.of(
                "player", player
        ));
        return playerPerks.stream()
                .map(RPlayerPerk::getPerk)
                .collect(Collectors.toList());
    }

    @Override
    @NotNull
    public List<RPerk> getEnabledPerks(@NotNull RDQPlayer player) {
        List<RPlayerPerk> playerPerks = rdq.getPlayerPerkRepository().findListByAttributes(Map.of(
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
        return rdq.getPlayerRepository().findByAttributes(Map.of("uniqueId", player.getUniqueId()));
    }

    @Override
    public void cleanupPlayerState(@NotNull UUID playerId) {
        final var initializationManager = rdq.getPerkInitializationManager();
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