package com.raindropcentral.rdq.reward;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
// import de.jexcellence.economy.adapter.CurrencyAdapter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class CurrencyReward extends AbstractReward {

    @JsonProperty("currencyId")
    private final String currencyId;
    
    @JsonProperty("amount")
    private final double amount;

    @JsonCreator
    public CurrencyReward(@JsonProperty("currencyId") @NotNull String currencyId, 
                          @JsonProperty("amount") double amount) {
        super(Type.CURRENCY, "reward.currency");
        this.currencyId = currencyId;
        this.amount = amount;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        // TODO: Implement when economy system is available
        // var adapter = Bukkit.getServicesManager().load(CurrencyAdapter.class);
        // if (adapter == null) return CompletableFuture.completedFuture(false);
        // return adapter.hasGivenCurrency(currencyId)
        //     .thenCompose(exists -> exists 
        //         ? adapter.deposit(player, currencyId, amount).thenApply(r -> r.isTransactionSuccessful())
        //         : CompletableFuture.completedFuture(false));
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public double getEstimatedValue() { return amount; }

    public String getCurrencyId() { return currencyId; }
    public double getAmount() { return amount; }
}
