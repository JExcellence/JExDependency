/*
package com.raindropcentral.rdq2.perk;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Objects;

public sealed interface PerkRequirement {

    record RankRequired(@NotNull String rankId) implements PerkRequirement {
        public RankRequired {
            Objects.requireNonNull(rankId, "rankId");
        }
    }

    record PermissionRequired(@NotNull String permission) implements PerkRequirement {
        public PermissionRequired {
            Objects.requireNonNull(permission, "permission");
        }
    }

    record CurrencyRequired(
        @NotNull String currency,
        @NotNull BigDecimal amount
    ) implements PerkRequirement {
        public CurrencyRequired {
            Objects.requireNonNull(currency, "currency");
            Objects.requireNonNull(amount, "amount");
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("amount must be non-negative");
            }
        }
    }

    record LevelRequired(int level) implements PerkRequirement {
        public LevelRequired {
            if (level < 0) throw new IllegalArgumentException("level must be non-negative");
        }
    }
}
*/
