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

    private final Map<String, LoadedPerk> perksById = new ConcurrentHashMap<>();
    private final Map<EPerkCategory, List<LoadedPerk>> perksByCategory = new ConcurrentHashMap<>();
    private final PerkTypeRegistry typeRegistry;
    private final List<Runnable> reloadListeners = new ArrayList<>();

    public PerkRegistry(@NotNull PerkTypeRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;
    }

    public void register(@NotNull PerkConfig config) {
        var type = typeRegistry.get(config.perkType().name());
        if (type == null) throw new IllegalArgumentException("No PerkType registered for: " + config.perkType());

        var loaded = type.createLoadedPerk(config);
        perksById.put(config.id(), loaded);
        perksByCategory.computeIfAbsent(config.category(), k -> new ArrayList<>()).add(loaded);
    }

    public void unregister(@NotNull String perkId) {
        var loaded = perksById.remove(perkId);
        if (loaded != null) perksByCategory.values().forEach(list -> list.remove(loaded));
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
        reloadListeners.forEach(listener -> {
            try {
                listener.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
