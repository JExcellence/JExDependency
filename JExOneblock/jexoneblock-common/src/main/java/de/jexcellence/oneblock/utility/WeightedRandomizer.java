package de.jexcellence.oneblock.utility;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class WeightedRandomizer<T> {
    
    private final List<WeightedEntry<T>> entries = new ArrayList<>();
    private double totalWeight = 0.0;
    private boolean needsSort = false;
    
    public void addEntry(@NotNull T item, double weight) {
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight must be positive, got: " + weight);
        }
        
        totalWeight += weight;
        entries.add(new WeightedEntry<>(item, weight, totalWeight));
        needsSort = true;
    }
    
    public @Nullable T getRandom() {
        if (entries.isEmpty()) {
            return null;
        }
        
        if (needsSort) {
            sortEntries();
            needsSort = false;
        }
        
        double randomValue = ThreadLocalRandom.current().nextDouble(totalWeight);
        
        int left = 0;
        int right = entries.size() - 1;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            WeightedEntry<T> entry = entries.get(mid);
            
            if (randomValue <= entry.cumulativeWeight) {
                if (mid == 0 || randomValue > entries.get(mid - 1).cumulativeWeight) {
                    return entry.item;
                }
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }
        
        return entries.get(entries.size() - 1).item;
    }
    
    public void clear() {
        entries.clear();
        totalWeight = 0.0;
        needsSort = false;
    }
    
    public int size() {
        return entries.size();
    }
    
    public boolean isEmpty() {
        return entries.isEmpty();
    }
    
    public double getTotalWeight() {
        return totalWeight;
    }
    
    private void sortEntries() {
        double cumulative = 0.0;
        for (WeightedEntry<T> entry : entries) {
            cumulative += entry.weight;
            entry.cumulativeWeight = cumulative;
        }
        totalWeight = cumulative;
    }
    
    private static final class WeightedEntry<T> {
        final T item;
        final double weight;
        double cumulativeWeight;
        
        WeightedEntry(T item, double weight, double cumulativeWeight) {
            this.item = item;
            this.weight = weight;
            this.cumulativeWeight = cumulativeWeight;
        }
    }
}