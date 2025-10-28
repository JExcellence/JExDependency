package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.config.perk.PerkSection; import org.jetbrains.annotations.NotNull; import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class PerkRegistry {

    private final Map<String, PerkSection> byId = new LinkedHashMap<>();

    public void register(final @NotNull String identifier, final @NotNull PerkSection section) {
        byId.put(Objects.requireNonNull(identifier), Objects.requireNonNull(section));
    }

    public @Nullable PerkSection get(final @NotNull String identifier) {
        return byId.get(identifier);
    }

    public @NotNull Collection<PerkSection> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public @NotNull Set<String> identifiers() {
        return Collections.unmodifiableSet(byId.keySet());
    }

    public int size() {
        return byId.size();
    }
}