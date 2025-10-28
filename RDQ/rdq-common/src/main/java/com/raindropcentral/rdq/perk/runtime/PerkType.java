package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.perk.config.PerkConfig;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface PerkType {

    @NotNull String getTypeId();

    boolean canHandle(@NotNull PerkConfig config);

    @NotNull LoadedPerk createLoadedPerk(@NotNull PerkConfig config);

    boolean activate(@NotNull Player player, @NotNull LoadedPerk perk);

    boolean deactivate(@NotNull Player player, @NotNull LoadedPerk perk);

    void trigger(@NotNull Player player, @NotNull LoadedPerk perk);
}
