package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.perk.config.PerkConfig;
import com.raindropcentral.rdq.type.EPerkCategory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PerkRegistry {

    private final Map<String, LoadedPerk> perksById;
    private final Map<EPerkCategory, List<LoadedPerk>> perksByCategory;
    private final PerkTypeRegistry typeRegistry;
    private final List<Runnable> reloadListeners;

    public PerkRegistry(@NotNull PerkTypeRegistry typeRegistry) {
        this.perksById = new ConcurrentHashMap<>();
        this.perksByCategory = new ConcurrentHashMap<>();
        this.typeRegistry = typeRegistry;
        this.reloadListeners = new ArrayList<>();
    }

    public void register(@NotNull PerkConfig config) {
        PerkType type = typeRegistry.get(config.perkType().name());
        if (type == null) {
            throw new IllegalArgumentException("No PerkType registered for: " + config.perkType());
        }

        LoadedPerk loaded = type.createLoadedPerk(config);
        perksById.put(config.id(), loaded);

        perksByCategory.computeIfAbsent(config.category(), k -> new ArrayList<>()).add(loaded);
    }

    public void unregister(@NotNull String perkId) {
        LoadedPerk loaded = perksById.remove(perkId);
        if (loaded != null) {
            perksByCategory.values().forEach(list -> list.remove(loaded));
        }
    }

    public @Nullable LoadedPerk get(@NotNull String perkId) {
        return perksById.get(perkId);
    }

    public @NotNull List<LoadedPerk> getByCategory(@NotNull EPerkCategory category) {
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
        for (Runnable listener : reloadListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
