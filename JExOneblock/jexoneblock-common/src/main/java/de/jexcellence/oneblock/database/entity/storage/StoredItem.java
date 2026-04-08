package de.jexcellence.oneblock.database.entity.storage;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

@Entity
@Table(name = "stored_items", indexes = {
    @Index(name = "idx_stored_items_island", columnList = "island_id"),
    @Index(name = "idx_stored_items_material", columnList = "material"),
    @Index(name = "idx_stored_items_category", columnList = "category"),
    @Index(name = "idx_stored_items_composite", columnList = "island_id, category, material")
})
public class StoredItem extends BaseEntity {

    @Column(name = "island_id", nullable = false)
    private Long islandId;

    @Enumerated(EnumType.STRING)
    @Column(name = "material", nullable = false)
    private Material material;

    @Column(name = "quantity", nullable = false)
    private Long quantity = 0L;

    @Column(name = "item_data", columnDefinition = "TEXT")
    private String itemData;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private StorageCategory category;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private StorageSource source = StorageSource.UNKNOWN;

    @Column(name = "rarity_tier")
    private Integer rarityTier = 0;

    public StoredItem() {}

    public StoredItem(@NotNull Long islandId, @NotNull Material material, @NotNull Long quantity, 
                     @NotNull StorageCategory category, @NotNull StorageSource source) {
        this.islandId = islandId;
        this.material = material;
        this.quantity = quantity;
        this.category = category;
        this.source = source;
        this.lastUpdated = LocalDateTime.now();
    }

    @NotNull
    public Long getIslandId() {
        return islandId;
    }

    public void setIslandId(@NotNull Long islandId) {
        this.islandId = islandId;
    }

    @NotNull
    public Material getMaterial() {
        return material;
    }

    public void setMaterial(@NotNull Material material) {
        this.material = material;
    }

    @NotNull
    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(@NotNull Long quantity) {
        this.quantity = Math.max(0L, quantity);
        this.lastUpdated = LocalDateTime.now();
    }

    public void addQuantity(@NotNull Long amount) {
        this.quantity += amount;
        this.lastUpdated = LocalDateTime.now();
    }

    public void removeQuantity(@NotNull Long amount) {
        this.quantity = Math.max(0L, this.quantity - amount);
        this.lastUpdated = LocalDateTime.now();
    }

    @Nullable
    public String getItemData() {
        return itemData;
    }

    public void setItemData(@Nullable String itemData) {
        this.itemData = itemData;
        this.lastUpdated = LocalDateTime.now();
    }

    @NotNull
    public StorageCategory getCategory() {
        return category;
    }

    public void setCategory(@NotNull StorageCategory category) {
        this.category = category;
    }

    @NotNull
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(@NotNull LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @NotNull
    public StorageSource getSource() {
        return source;
    }

    public void setSource(@NotNull StorageSource source) {
        this.source = source;
    }

    @NotNull
    public Integer getRarityTier() {
        return rarityTier;
    }

    public void setRarityTier(@NotNull Integer rarityTier) {
        this.rarityTier = rarityTier;
    }

    public boolean isEmpty() {
        return quantity <= 0;
    }

    public boolean canStack(@NotNull StoredItem other) {
        return this.material == other.material && 
               this.category == other.category &&
               java.util.Objects.equals(this.itemData, other.itemData);
    }

    @Override
    public String toString() {
        return String.format("StoredItem{island=%s, material=%s, quantity=%d, category=%s}", 
                           islandId, material, quantity, category);
    }
}