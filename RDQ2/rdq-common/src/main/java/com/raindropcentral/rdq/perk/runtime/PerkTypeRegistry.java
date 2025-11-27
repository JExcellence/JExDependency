package com.raindropcentral.rdq.perk.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PerkTypeRegistry {

    private final Map<String, PerkType> types;

    public PerkTypeRegistry() {
        this.types = new ConcurrentHashMap<>();
    }

    public void register(@NotNull PerkType perkType) {
        types.put(perkType.getTypeId(), perkType);
    }

    public void unregister(@NotNull String typeId) {
        types.remove(typeId);
    }

    public @Nullable PerkType get(@NotNull String typeId) {
        return types.get(typeId);
    }

    public boolean isRegistered(@NotNull String typeId) {
        return types.containsKey(typeId);
    }

    public void clear() {
        types.clear();
    }
}
