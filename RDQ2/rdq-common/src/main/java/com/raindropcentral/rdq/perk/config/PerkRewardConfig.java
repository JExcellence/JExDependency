package com.raindropcentral.rdq.perk.config;

import org.jetbrains.annotations.NotNull;

public sealed interface PerkRewardConfig permits
        PerkRewardConfig.MessageRewardConfig,
        PerkRewardConfig.CommandRewardConfig,
        PerkRewardConfig.CurrencyRewardConfig {

    @NotNull String type();

    record MessageRewardConfig(
        @NotNull String type,
        @NotNull String message
    ) implements PerkRewardConfig {
        public MessageRewardConfig {
            if (type == null) throw new IllegalArgumentException("type cannot be null");
            if (message == null) throw new IllegalArgumentException("message cannot be null");
        }
    }

    record CommandRewardConfig(
        @NotNull String type,
        @NotNull String command
    ) implements PerkRewardConfig {
        public CommandRewardConfig {
            if (type == null) throw new IllegalArgumentException("type cannot be null");
            if (command == null) throw new IllegalArgumentException("command cannot be null");
        }
    }

    record CurrencyRewardConfig(
        @NotNull String type,
        @NotNull String currencyType,
        double amount
    ) implements PerkRewardConfig {
        public CurrencyRewardConfig {
            if (type == null) throw new IllegalArgumentException("type cannot be null");
            if (currencyType == null) throw new IllegalArgumentException("currencyType cannot be null");
        }
    }
}
