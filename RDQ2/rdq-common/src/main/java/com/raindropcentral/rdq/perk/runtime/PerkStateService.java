package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.database.entity.perk.RPerk;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface PerkStateService {

    boolean playerOwnsPerk(@NotNull RDQPlayer player, @NotNull RPerk perk);

    void grantPerk(@NotNull RDQPlayer player, @NotNull RPerk perk, boolean enabled);

    void revokePerk(@NotNull RDQPlayer player, @NotNull RPerk perk);

    boolean isPerkEnabled(@NotNull RDQPlayer player, @NotNull RPerk perk);

    boolean enablePerk(@NotNull RDQPlayer player, @NotNull RPerk perk, int maxEnabledPerks);

    boolean disablePerk(@NotNull RDQPlayer player, @NotNull RPerk perk);

    @NotNull List<RPerk> getOwnedPerks(@NotNull RDQPlayer player);

    @NotNull List<RPerk> getEnabledPerks(@NotNull RDQPlayer player);

    @Nullable RDQPlayer getRDQPlayer(@NotNull Player player);

    void cleanupPlayerState(@NotNull UUID playerId);
}
