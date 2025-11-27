package com.raindropcentral.rdq.database.entity.reward;

import com.raindropcentral.rplatform.database.converter.ItemStackConverter;
import jakarta.persistence.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@Embeddable
public class RewardItem {

    @Column(name = "unique_id", nullable = false, unique = true)
    private UUID uniqueId;

    @Column(name = "item", columnDefinition = "LONGTEXT", nullable = false)
    @Convert(converter = ItemStackConverter.class)
    private ItemStack item;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "contributor_unique_id")
    private UUID contributorUniqueId;

    @Column(name = "contributed_at")
    private LocalDateTime contributedAt;

    protected RewardItem() {}

    public RewardItem(@NotNull ItemStack item) {
        this.item = item;
        this.amount = item.getAmount();
    }

    public RewardItem(@NotNull ItemStack item, @NotNull Player contributor) {
        this.uniqueId = UUID.randomUUID();
        this.item = item;
        this.amount = item.getAmount();
        this.contributorUniqueId = contributor.getUniqueId();
        this.contributedAt = LocalDateTime.now();
    }

    public ItemStack getItem() { return item; }
    public int getAmount() { return amount; }
    public @Nullable UUID getContributorUniqueId() { return contributorUniqueId; }
    public @Nullable LocalDateTime getContributedAt() { return contributedAt; }
    
    public void setAmount(int amount) { this.amount = amount; }
}
