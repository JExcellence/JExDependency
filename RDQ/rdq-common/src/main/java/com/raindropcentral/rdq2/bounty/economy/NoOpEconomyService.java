package com.raindropcentral.rdq2.bounty.economy;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class NoOpEconomyService implements EconomyService {

    private static final Logger LOGGER = Logger.getLogger(NoOpEconomyService.class.getName());

    public NoOpEconomyService() {
        LOGGER.warning("Using NoOp economy service - bounty transactions will always fail");
    }

    @Override
    public CompletableFuture<Boolean> withdraw(@NotNull UUID playerId, @NotNull BigDecimal amount, @NotNull String currency) {
        LOGGER.warning("Attempted withdraw without economy provider");
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> deposit(@NotNull UUID playerId, @NotNull BigDecimal amount, @NotNull String currency) {
        LOGGER.warning("Attempted deposit without economy provider");
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<BigDecimal> getBalance(@NotNull UUID playerId, @NotNull String currency) {
        return CompletableFuture.completedFuture(BigDecimal.ZERO);
    }

    @Override
    public CompletableFuture<Boolean> hasBalance(@NotNull UUID playerId, @NotNull BigDecimal amount, @NotNull String currency) {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    @NotNull
    public String getName() {
        return "None";
    }
}
