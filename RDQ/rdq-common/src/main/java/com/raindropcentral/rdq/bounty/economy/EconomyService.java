package com.raindropcentral.rdq.bounty.economy;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EconomyService {

    CompletableFuture<Boolean> withdraw(@NotNull UUID playerId, @NotNull BigDecimal amount, @NotNull String currency);

    CompletableFuture<Boolean> deposit(@NotNull UUID playerId, @NotNull BigDecimal amount, @NotNull String currency);

    CompletableFuture<BigDecimal> getBalance(@NotNull UUID playerId, @NotNull String currency);

    CompletableFuture<Boolean> hasBalance(@NotNull UUID playerId, @NotNull BigDecimal amount, @NotNull String currency);

    boolean isAvailable();

    @NotNull
    String getName();
}
