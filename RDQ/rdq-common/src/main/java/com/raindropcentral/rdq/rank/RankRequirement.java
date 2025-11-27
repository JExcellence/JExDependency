package com.raindropcentral.rdq.rank;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Objects;

public sealed interface RankRequirement {

    record StatisticRequirement(
        @NotNull String statisticType,
        int amount
    ) implements RankRequirement {
        public StatisticRequirement {
            Objects.requireNonNull(statisticType, "statisticType");
            if (amount < 0) throw new IllegalArgumentException("amount must be non-negative");
        }
    }

    record PermissionRequirement(
        @NotNull String permission
    ) implements RankRequirement {
        public PermissionRequirement {
            Objects.requireNonNull(permission, "permission");
        }
    }

    record PreviousRankRequirement(
        @NotNull String requiredRankId
    ) implements RankRequirement {
        public PreviousRankRequirement {
            Objects.requireNonNull(requiredRankId, "requiredRankId");
        }
    }

    record CurrencyRequirement(
        @NotNull String currency,
        @NotNull BigDecimal amount
    ) implements RankRequirement {
        public CurrencyRequirement {
            Objects.requireNonNull(currency, "currency");
            Objects.requireNonNull(amount, "amount");
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("amount must be non-negative");
            }
        }
    }

    record ItemRequirement(
        @NotNull String material,
        int amount
    ) implements RankRequirement {
        public ItemRequirement {
            Objects.requireNonNull(material, "material");
            if (amount < 1) throw new IllegalArgumentException("amount must be at least 1");
        }
    }

    record LevelRequirement(
        int level
    ) implements RankRequirement {
        public LevelRequirement {
            if (level < 0) throw new IllegalArgumentException("level must be non-negative");
        }
    }

    record PlaytimeRequirement(
        long seconds
    ) implements RankRequirement {
        public PlaytimeRequirement {
            if (seconds < 0) throw new IllegalArgumentException("seconds must be non-negative");
        }
    }
}
