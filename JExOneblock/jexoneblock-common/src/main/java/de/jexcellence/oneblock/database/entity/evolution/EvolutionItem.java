package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.hibernate.entity.BaseEntity;
import de.jexcellence.oneblock.database.converter.ItemStackListConverter;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import jakarta.persistence.*;
import lombok.*;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing items that can drop in an evolution
 * Replaces StageItem with evolution-based naming and Lombok optimization
 */
@Entity
@Table(name = "evolution_items",
    indexes = {
        @Index(name = "idx_evolution_item_evolution_id", columnList = "evolution_id"),
        @Index(name = "idx_evolution_item_rarity", columnList = "rarity"),
        @Index(name = "idx_evolution_item_enabled", columnList = "is_enabled"),
        @Index(name = "idx_evolution_item_drop_chance", columnList = "drop_chance"),
        @Index(name = "idx_evolution_item_silk_touch", columnList = "requires_silk_touch"),
        @Index(name = "idx_evolution_item_composite", columnList = "evolution_id, rarity, is_enabled")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_evolution_item_evolution_rarity", 
                         columnNames = {"evolution_id", "rarity"})
    }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"evolution", "rarity"}, callSuper = false)
public class EvolutionItem extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evolution_id", nullable = false)
    private OneblockEvolution evolution;
    
    @Column(name = "rarity", nullable = false)
    @Enumerated(EnumType.STRING)
    private EEvolutionRarityType rarity;
    
    @Column(name = "item_stacks", columnDefinition = "TEXT")
    @Convert(converter = ItemStackListConverter.class)
    @Builder.Default
    private List<ItemStack> itemStacks = new ArrayList<>();
    
    @Column(name = "weight", nullable = false)
    @Builder.Default
    private double weight = 1.0;
    
    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private boolean isEnabled = true;
    
    @Column(name = "drop_chance", nullable = false)
    @Builder.Default
    private double dropChance = 0.05; // 5% default drop chance
    
    @Column(name = "max_drops_per_break", nullable = false)
    @Builder.Default
    private int maxDropsPerBreak = 1;
    
    @Column(name = "requires_silk_touch", nullable = false)
    @Builder.Default
    private boolean requiresSilkTouch = false;
    
    /**
     * Constructor for creating evolution item with basic parameters
     * @param evolution the parent evolution
     * @param rarity the rarity level
     * @param itemStacks the list of item stacks
     */
    public EvolutionItem(@NotNull OneblockEvolution evolution, @NotNull EEvolutionRarityType rarity, @NotNull List<ItemStack> itemStacks) {
        this.evolution = evolution;
        this.rarity = rarity;
        this.itemStacks = new ArrayList<>(itemStacks);
        this.weight = 1.0;
        this.isEnabled = true;
        this.dropChance = 0.05;
        this.maxDropsPerBreak = 1;
        this.requiresSilkTouch = false;
    }
    
    /**
     * Adds an item stack to this item configuration
     * @param itemStack the item stack to add
     */
    public void addItemStack(@NotNull ItemStack itemStack) {
        if (this.itemStacks == null) {
            this.itemStacks = new ArrayList<>();
        }
        this.itemStacks.add(itemStack.clone());
    }
    
    /**
     * Removes an item stack from this item configuration
     * @param itemStack the item stack to remove
     * @return true if the item stack was removed
     */
    public boolean removeItemStack(@NotNull ItemStack itemStack) {
        return this.itemStacks != null && this.itemStacks.remove(itemStack);
    }
    
    /**
     * Checks if this item configuration contains a specific item stack
     * @param itemStack the item stack to check
     * @return true if the item stack is present
     */
    public boolean containsItemStack(@NotNull ItemStack itemStack) {
        return this.itemStacks != null && this.itemStacks.contains(itemStack);
    }
    
    /**
     * Gets the number of item stacks in this configuration
     * @return the count of item stacks
     */
    public int getItemStackCount() {
        return this.itemStacks != null ? this.itemStacks.size() : 0;
    }
    
    /**
     * Checks if this item configuration is valid
     * @return true if enabled and has at least one item stack
     */
    public boolean isValid() {
        return isEnabled && itemStacks != null && !itemStacks.isEmpty() && dropChance > 0;
    }
    
    /**
     * Calculates if an item should drop based on drop chance
     * @return true if item should drop
     */
    public boolean shouldDrop() {
        return isValid() && Math.random() < dropChance;
    }
    
    /**
     * Gets a random item stack from this configuration
     * @return a random item stack, or null if none available
     */
    public ItemStack getRandomItemStack() {
        if (itemStacks == null || itemStacks.isEmpty()) {
            return null;
        }
        ItemStack original = itemStacks.get((int) (Math.random() * itemStacks.size()));
        return original != null ? original.clone() : null;
    }
    
    /**
     * Gets multiple random item stacks based on max drops per break
     * @return list of random item stacks
     */
    public @NotNull List<ItemStack> getRandomItemStacks() {
        List<ItemStack> result = new ArrayList<>();
        if (!isValid()) {
            return result;
        }
        
        int dropCount = Math.min(maxDropsPerBreak, (int) (Math.random() * maxDropsPerBreak) + 1);
        for (int i = 0; i < dropCount; i++) {
            ItemStack randomItem = getRandomItemStack();
            if (randomItem != null) {
                result.add(randomItem);
            }
        }
        
        return result;
    }
    
    /**
     * Checks if this item configuration can drop with the given tool
     * @param hasSilkTouch whether the tool has silk touch
     * @return true if item can drop
     */
    public boolean canDropWith(boolean hasSilkTouch) {
        return isValid() && (!requiresSilkTouch || hasSilkTouch);
    }
    
    /**
     * Sets the parent evolution
     * @param evolution the parent evolution
     */
    public void setEvolution(@NotNull OneblockEvolution evolution) {
        this.evolution = evolution;
    }
    
    @Override
    public String toString() {
        return "EvolutionItem{" +
                "rarity=" + rarity +
                ", itemStackCount=" + getItemStackCount() +
                ", weight=" + weight +
                ", dropChance=" + dropChance +
                ", requiresSilkTouch=" + requiresSilkTouch +
                ", isEnabled=" + isEnabled +
                '}';
    }
}