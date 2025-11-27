package com.raindropcentral.rdq.perk.repository;

import com.raindropcentral.rdq.perk.Perk;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class PerkRepository {

    private final Map<String, Perk> perks = new ConcurrentHashMap<>();

    public void register(@NotNull Perk perk) {
        perks.put(perk.id(), perk);
    }

    public void registerAll(@NotNull List<Perk> perksToRegister) {
        perksToRegister.forEach(this::register);
    }

    @NotNull
    public Optional<Perk> findById(@NotNull String id) {
        return Optional.ofNullable(perks.get(id));
    }

    @NotNull
    public List<Perk> findAll() {
        return List.copyOf(perks.values());
    }

    @NotNull
    public List<Perk> findEnabled() {
        return perks.values().stream()
            .filter(Perk::enabled)
            .toList();
    }

    @NotNull
    public List<Perk> findByCategory(@NotNull String category) {
        return perks.values().stream()
            .filter(p -> category.equals(p.category()))
            .toList();
    }

    @NotNull
    public List<Perk> findEnabledByCategory(@NotNull String category) {
        return perks.values().stream()
            .filter(p -> p.enabled() && category.equals(p.category()))
            .toList();
    }

    @NotNull
    public List<String> getCategories() {
        return perks.values().stream()
            .map(Perk::category)
            .filter(c -> c != null)
            .distinct()
            .sorted()
            .toList();
    }

    @NotNull
    public List<Perk> findToggleable() {
        return perks.values().stream()
            .filter(p -> p.enabled() && p.isToggleable())
            .toList();
    }

    @NotNull
    public List<Perk> findEventBased() {
        return perks.values().stream()
            .filter(p -> p.enabled() && p.isEventBased())
            .toList();
    }

    @NotNull
    public List<Perk> findPassive() {
        return perks.values().stream()
            .filter(p -> p.enabled() && p.isPassive())
            .toList();
    }

    public boolean exists(@NotNull String id) {
        return perks.containsKey(id);
    }

    public int count() {
        return perks.size();
    }

    public void clear() {
        perks.clear();
    }

    public void reload(@NotNull List<Perk> newPerks) {
        perks.clear();
        registerAll(newPerks);
    }
}
