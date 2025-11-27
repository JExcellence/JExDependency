package com.raindropcentral.rdq.reward;

import de.jexcellence.economy.adapter.CurrencyAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class CurrencyReward extends AbstractReward {

    private final String currencyId;
    private final double amount;

    public CurrencyReward(@NotNull String currencyId, double amount) {
        super(Type.CURRENCY, "reward.currency");
        this.currencyId = currencyId;
        this.amount = amount;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        var adapter = Bukkit.getServicesManager().load(CurrencyAdapter.class);
        if (adapter == null) return CompletableFuture.completedFuture(false);
        
        return adapter.hasGivenCurrency(currencyId)
            .thenCompose(exists -> exists 
                ? adapter.deposit(player, currencyId, amount).thenApply(r -> r.isTransactionSuccessful())
                : CompletableFuture.completedFuture(false));
    }

    @Override
    public double getEstimatedValue() { return amount; }

    public String getCurrencyId() { return currencyId; }
    public double getAmount() { return amount; }
}
