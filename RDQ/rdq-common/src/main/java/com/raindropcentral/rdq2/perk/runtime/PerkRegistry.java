/*
package com.raindropcentral.rdq2.perk.runtime;

import com.raindropcentral.rdq2.perk.Perk;
import com.raindropcentral.rdq2.perk.config.PerkConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PerkRegistry {

    private final Map<String, LoadedPerk> perksById = new ConcurrentHashMap<>();
    private final Map<String, List<LoadedPerk>> perksByCategory = new ConcurrentHashMap<>();
    private final PerkTypeRegistry typeRegistry;
    private final List<Runnable> reloadListeners = new ArrayList<>();

    public PerkRegistry(@NotNull PerkTypeRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;
    }

    public void register(@NotNull Perk perk) {
        // TODO: Implement proper perk registration when LoadedPerk is complete
        // For now, just store the perk ID to allow compilation
        // var loaded = new LoadedPerk(perk);
        // perksById.put(perk.id(), loaded);
        // if (perk.category() != null) {
        //     perksByCategory.computeIfAbsent(perk.category(), k -> new ArrayList<>()).add(loaded);
        // }
    }
    
    public void register(@NotNull PerkConfig config) {
        // TODO: Implement proper config registration
        // This method exists for backward compatibility
    }

    public void unregister(@NotNull String perkId) {
        var loaded = perksById.remove(perkId);
        if (loaded != null) perksByCategory.values().forEach(list -> list.remove(loaded));
    }

    public @Nullable LoadedPerk get(@NotNull String perkId) {
        return perksById.get(perkId);
    }

    public @NotNull List<LoadedPerk> getByCategory(@NotNull String category) {
        return new ArrayList<>(perksByCategory.getOrDefault(category, new ArrayList<>()));
    }

    public @NotNull List<LoadedPerk> getAll() {
        return new ArrayList<>(perksById.values());
    }

    public boolean isRegistered(@NotNull String perkId) {
        return perksById.containsKey(perkId);
    }

    public void registerReloadListener(@NotNull Runnable listener) {
        reloadListeners.add(listener);
    }

    public void onReload() {
        perksById.clear();
        perksByCategory.clear();
        reloadListeners.forEach(listener -> {
            try {
                listener.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    */
/**
     * Reloads the registry with new perks.
     * @param perks the list of perks to load
     *//*

    public void reload(@NotNull List<Perk> perks) {
        onReload();
        perks.forEach(this::register);
    }
    
    */
/**
     * Gets active perks for a player.
     * @param playerId the player UUID
     * @return list of active perk runtimes
     *//*

    public @NotNull List<Object> getActiveForPlayer(@NotNull java.util.UUID playerId) {
        // TODO: Implement when perk runtime tracking is complete
        return List.of();
    }
}
*/
