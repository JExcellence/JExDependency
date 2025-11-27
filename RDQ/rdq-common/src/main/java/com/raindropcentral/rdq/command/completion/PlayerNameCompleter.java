package com.raindropcentral.rdq.command.completion;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class PlayerNameCompleter {

    public List<String> complete(final @NotNull String input) {
        var lowerInput = input.toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(lowerInput))
                .sorted()
                .toList();
    }

    public List<String> completeExcluding(final @NotNull String input, final @NotNull Player exclude) {
        var lowerInput = input.toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.getUniqueId().equals(exclude.getUniqueId()))
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(lowerInput))
                .sorted()
                .toList();
    }
}
