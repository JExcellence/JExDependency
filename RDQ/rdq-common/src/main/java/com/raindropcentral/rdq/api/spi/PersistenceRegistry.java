package com.raindropcentral.rdq.api.spi;

import java.util.Optional;

public class PersistenceRegistry {
    private final BountyPersistence bountyPersistence;
    private final PlayerPersistence playerPersistence;

    protected PersistenceRegistry(Builder builder) {
        this.bountyPersistence = builder.bountyPersistence;
        this.playerPersistence = builder.playerPersistence;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<BountyPersistence> getBountyPersistence() {
        return Optional.ofNullable(bountyPersistence);
    }

    public Optional<PlayerPersistence> getPlayerPersistence() {
        return Optional.ofNullable(playerPersistence);
    }

    public static final class Builder {
        private BountyPersistence bountyPersistence;
        private PlayerPersistence playerPersistence;

        public Builder bountyPersistence(BountyPersistence bountyPersistence) {
            this.bountyPersistence = bountyPersistence;
            return this;
        }

        public Builder playerPersistence(PlayerPersistence playerPersistence) {
            this.playerPersistence = playerPersistence;
            return this;
        }

        public PersistenceRegistry build() {
            return new PersistenceRegistry(this);
        }
    }
}