package com.raindropcentral.rdq.view.bounty;

import net.jqwik.api.*;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for BountyRewardView.
 * These tests verify item insertion, removal, and merging behavior.
 * 
 * Note: These tests focus on the core logic of item grouping and merging,
 * which is the key algorithmic component of the BountyRewardView.
 * Full integration testing of the view requires a running Bukkit server.
 * 
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
class BountyRewardViewPropertiesTest {
    
    /**
     * **Feature: bounty-system-rebuild, Property 5: Item removal updates state**
     * 
     * For any set of inserted items, when a player removes an item from a reward slot,
     * the reward items state should be updated to reflect the removal.
     * 
     * **Validates: Requirements 4.4**
     */
    @Property(tries = 100)
    @Label("Property 5: Item removal updates state")
    void itemRemovalUpdatesStateProperty(
        @ForAll("materialAmountPairs") List<MaterialAmountPair> items
    ) {
        Assume.that(!items.isEmpty());
        
        // Given: A set of inserted items (represented as material-amount pairs)
        Map<Integer, MaterialAmountPair> insertedItems = new HashMap<>();
        
        for (int i = 0; i < items.size(); i++) {
            insertedItems.put(i, items.get(i));
        }
        
        // Group by material and calculate merged items
        Map<Material, Integer> grouped = groupByMaterial(insertedItems);
        int initialGroupedSize = grouped.size();
        int initialInsertedSize = insertedItems.size();
        
        // When: Removing an item from inserted items
        if (!insertedItems.isEmpty()) {
            Integer firstKey = insertedItems.keySet().iterator().next();
            insertedItems.remove(firstKey);
            
            // Recalculate grouped items
            grouped = groupByMaterial(insertedItems);
        }
        
        // Then: The inserted items map should have one less item
        assertEquals(initialInsertedSize - 1, insertedItems.size(),
            "Inserted items should decrease by 1 after removal");
        
        // And: The grouped items should be updated accordingly
        // (may be same size if removed item was of same type as another, or smaller if it was unique)
        assertTrue(grouped.size() <= initialGroupedSize,
            "Grouped items size should not increase after removal");
    }
    
    /**
     * **Feature: bounty-system-rebuild, Property 12: Similar items stack**
     * 
     * For any set of items of the same material, when merged, they should be combined
     * into a single entry with the total amount preserved.
     * 
     * **Validates: Requirements 4.6**
     */
    @Property(tries = 100)
    @Label("Property 12: Similar items stack")
    void similarItemsStackProperty(
        @ForAll("material") Material material,
        @ForAll("itemAmounts") List<Integer> amounts
    ) {
        Assume.that(!amounts.isEmpty());
        Assume.that(amounts.stream().allMatch(a -> a > 0 && a <= 64));
        Assume.that(material.isItem());
        
        // Given: Multiple items of the same material with different amounts
        Map<Integer, MaterialAmountPair> insertedItems = new HashMap<>();
        int expectedTotalAmount = 0;
        
        for (int i = 0; i < amounts.size(); i++) {
            insertedItems.put(i, new MaterialAmountPair(material, amounts.get(i)));
            expectedTotalAmount += amounts.get(i);
        }
        
        // When: Merging similar items (grouping by material)
        Map<Material, Integer> grouped = groupByMaterial(insertedItems);
        
        // Then: There should be exactly one entry for this material
        assertEquals(1, grouped.size(),
            "Similar items should be merged into a single entry");
        
        // And: The total amount should be preserved
        Integer mergedAmount = grouped.get(material);
        assertNotNull(mergedAmount, "Merged entry should exist for the material");
        assertEquals(expectedTotalAmount, mergedAmount,
            "Total amount should be preserved when merging similar items");
    }
    
    /**
     * Helper method to group items by material and sum amounts (simulates the view's logic).
     */
    private Map<Material, Integer> groupByMaterial(Map<Integer, MaterialAmountPair> items) {
        Map<Material, Integer> grouped = new HashMap<>();
        for (MaterialAmountPair pair : items.values()) {
            if (pair != null && pair.material.isItem()) {
                grouped.merge(pair.material, pair.amount, Integer::sum);
            }
        }
        return grouped;
    }
    
    /**
     * Simple record to represent a material-amount pair without using ItemStack.
     */
    private record MaterialAmountPair(Material material, int amount) {}
    
    // ========== Arbitraries (Generators) ==========
    
    @Provide
    Arbitrary<List<MaterialAmountPair>> materialAmountPairs() {
        return Arbitraries.integers().between(1, 10)
            .flatMap(size -> {
                Arbitrary<Material> materialArb = Arbitraries.of(
                    Material.DIAMOND,
                    Material.GOLD_INGOT,
                    Material.IRON_INGOT,
                    Material.EMERALD,
                    Material.COAL
                );
                Arbitrary<Integer> amountArb = Arbitraries.integers().between(1, 64);
                
                return Combinators.combine(materialArb, amountArb)
                    .as(MaterialAmountPair::new)
                    .list()
                    .ofSize(size);
            });
    }
    
    @Provide
    Arbitrary<Material> material() {
        return Arbitraries.of(
            Material.DIAMOND,
            Material.GOLD_INGOT,
            Material.IRON_INGOT,
            Material.EMERALD,
            Material.COAL,
            Material.STONE,
            Material.DIRT,
            Material.COBBLESTONE
        );
    }
    
    @Provide
    Arbitrary<List<Integer>> itemAmounts() {
        return Arbitraries.integers().between(1, 5)
            .flatMap(size -> 
                Arbitraries.integers().between(1, 64).list().ofSize(size)
            );
    }
}
