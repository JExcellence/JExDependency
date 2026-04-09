/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rplatform.requirement.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reflection-backed {@code PLUGIN} requirement bridge for RDT town progression data.
 */
final class RdtPluginIntegrationBridge implements PluginIntegrationBridge {

    private static final Logger LOGGER = Logger.getLogger(RdtPluginIntegrationBridge.class.getName());

    private static final String INTEGRATION_ID = "rdt";
    private static final String PLUGIN_NAME = "RDT";
    private static final String CATEGORY = "TOWNS";

    private static final Map<String, String> TYPED_CHUNK_LEVEL_KEYS = Map.of(
        "security_chunk_level", "SECURITY",
        "bank_chunk_level", "BANK",
        "farm_chunk_level", "FARM",
        "outpost_chunk_level", "OUTPOST",
        "medic_chunk_level", "MEDIC",
        "armory_chunk_level", "ARMORY"
    );

    private @Nullable Object rdtRuntime;

    @Override
    public @NotNull String getIntegrationId() {
        return INTEGRATION_ID;
    }

    @Override
    public @NotNull String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public @NotNull String getCategory() {
        return CATEGORY;
    }

    @Override
    public boolean isAvailable() {
        final Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (plugin == null || !plugin.isEnabled()) {
            this.rdtRuntime = null;
            return false;
        }

        if (this.rdtRuntime != null) {
            return true;
        }

        this.rdtRuntime = this.resolveRuntime(plugin);
        return this.rdtRuntime != null;
    }

    @Override
    public double getValue(final @NotNull Player player, final @NotNull String key) {
        if (!this.isAvailable()) {
            return 0.0D;
        }

        try {
            return switch (this.normalizeKey(key)) {
                case "town_level" -> this.resolveTownMetric(player, "getTownLevel");
                case "nexus_level" -> this.resolveTownMetric(player, "getNexusLevel");
                case "chunk_level" -> this.resolveStandingChunkLevel(player);
                default -> this.resolveTypedChunkLevel(player, this.normalizeKey(key));
            };
        } catch (final Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve RDT requirement value for key " + key, exception);
            return 0.0D;
        }
    }

    private double resolveTownMetric(final @NotNull Player player, final @NotNull String accessorName) {
        final Object town = this.resolveTownForPlayer(player);
        return town == null ? 0.0D : Math.max(0.0D, this.asDouble(this.invokeOptional(town, accessorName)));
    }

    private double resolveStandingChunkLevel(final @NotNull Player player) {
        final Object town = this.resolveTownForPlayer(player);
        if (town == null) {
            return 0.0D;
        }

        final Object townChunk = this.resolveStandingOwnTownChunk(player, town);
        return townChunk == null ? 0.0D : Math.max(0.0D, this.asDouble(this.invokeOptional(townChunk, "getChunkLevel")));
    }

    private double resolveTypedChunkLevel(final @NotNull Player player, final @NotNull String key) {
        final String expectedChunkType = TYPED_CHUNK_LEVEL_KEYS.get(key);
        if (expectedChunkType == null) {
            return 0.0D;
        }

        final Object town = this.resolveTownForPlayer(player);
        if (town == null) {
            return 0.0D;
        }

        double highestLevel = 0.0D;
        for (final Object townChunk : this.iterableOf(this.invokeOptional(town, "getChunks"))) {
            if (!expectedChunkType.equals(this.resolveChunkTypeName(townChunk))) {
                continue;
            }
            highestLevel = Math.max(highestLevel, Math.max(0.0D, this.asDouble(this.invokeOptional(townChunk, "getChunkLevel"))));
        }
        return highestLevel;
    }

    private @Nullable Object resolveStandingOwnTownChunk(final @NotNull Player player, final @NotNull Object town) {
        final Location location = player.getLocation();
        if (location == null) {
            return null;
        }

        final World world = location.getWorld();
        final Chunk chunk = location.getChunk();
        if (world == null || chunk == null) {
            return null;
        }

        final String worldName = world.getName();
        final int chunkX = chunk.getX();
        final int chunkZ = chunk.getZ();

        final Object repositoryChunk = this.resolveChunkByLocation(worldName, chunkX, chunkZ);
        if (this.isChunkOwnedByTown(repositoryChunk, town)) {
            return repositoryChunk;
        }

        return this.invokeOptional(town, "findChunk", worldName, chunkX, chunkZ);
    }

    private @Nullable Object resolveChunkByLocation(
        final @NotNull String worldName,
        final int chunkX,
        final int chunkZ
    ) {
        if (this.rdtRuntime == null) {
            return null;
        }

        final Object townChunkRepository = this.invokeOptional(this.rdtRuntime, "getTownChunkRepository");
        if (townChunkRepository == null) {
            return null;
        }

        return this.invokeOptional(townChunkRepository, "findByChunk", worldName, chunkX, chunkZ);
    }

    private boolean isChunkOwnedByTown(final @Nullable Object townChunk, final @Nullable Object town) {
        if (townChunk == null || town == null) {
            return false;
        }

        final Object chunkTown = this.invokeOptional(townChunk, "getTown");
        return chunkTown != null && this.sameTown(chunkTown, town);
    }

    private @Nullable Object resolveTownForPlayer(final @NotNull Player player) {
        final Object playerRecord = this.resolvePlayerRecord(player);
        if (playerRecord == null) {
            return null;
        }

        final UUID townUuid = this.resolveTownUuid(playerRecord);
        return townUuid == null ? null : this.resolveTown(townUuid);
    }

    private @Nullable Object resolvePlayerRecord(final @NotNull Player player) {
        if (this.rdtRuntime == null) {
            return null;
        }

        final Object playerRepository = this.invokeOptional(this.rdtRuntime, "getPlayerRepository");
        if (playerRepository == null) {
            return null;
        }

        return this.firstNonNull(
            this.invokeOptional(playerRepository, "findByPlayer", player.getUniqueId()),
            this.invokeOptional(playerRepository, "findByIdentifier", player.getUniqueId())
        );
    }

    private @Nullable UUID resolveTownUuid(final @NotNull Object playerRecord) {
        return this.asUuid(this.firstNonNull(
            this.invokeOptional(playerRecord, "getTownUUID"),
            this.invokeOptional(playerRecord, "getTownUuid")
        ));
    }

    private @Nullable Object resolveTown(final @NotNull UUID townUuid) {
        if (this.rdtRuntime == null) {
            return null;
        }

        final Object townRepository = this.invokeOptional(this.rdtRuntime, "getTownRepository");
        if (townRepository == null) {
            return null;
        }

        return this.firstNonNull(
            this.invokeOptional(townRepository, "findByTownUUID", townUuid),
            this.invokeOptional(townRepository, "findByTownUuid", townUuid),
            this.invokeOptional(townRepository, "findByIdentifier", townUuid),
            this.invokeOptional(townRepository, "findById", townUuid)
        );
    }

    private @Nullable Object resolveRuntime(final @NotNull Plugin plugin) {
        if (this.isRuntimeObject(plugin)) {
            return plugin;
        }

        final Object directRuntime = this.firstNonNull(
            this.invokeOptional(plugin, "getRdt"),
            this.readFieldOptional(plugin, "rdt")
        );
        if (this.isRuntimeObject(directRuntime)) {
            return directRuntime;
        }

        final Object delegate = this.firstNonNull(
            this.readFieldOptional(plugin, "impl"),
            this.readFieldOptional(plugin, "delegate")
        );
        if (delegate == null) {
            return null;
        }

        final Object delegatedRuntime = this.firstNonNull(
            this.invokeOptional(delegate, "getRdt"),
            this.readFieldOptional(delegate, "rdt")
        );
        return this.isRuntimeObject(delegatedRuntime) ? delegatedRuntime : null;
    }

    private boolean isRuntimeObject(final @Nullable Object runtime) {
        if (runtime == null) {
            return false;
        }

        return this.hasMethod(runtime.getClass(), "getPlayerRepository", 0)
            && this.hasMethod(runtime.getClass(), "getTownRepository", 0)
            && this.hasMethod(runtime.getClass(), "getTownChunkRepository", 0);
    }

    private @NotNull Iterable<?> iterableOf(final @Nullable Object value) {
        return value instanceof Iterable<?> iterable ? iterable : List.of();
    }

    private @Nullable String resolveChunkTypeName(final @Nullable Object townChunk) {
        if (townChunk == null) {
            return null;
        }

        final Object chunkType = this.firstNonNull(
            this.invokeOptional(townChunk, "getChunkType"),
            this.invokeOptional(townChunk, "getType")
        );
        if (chunkType instanceof Enum<?> enumValue) {
            return enumValue.name();
        }

        final Object namedValue = chunkType == null ? null : this.invokeOptional(chunkType, "name");
        if (namedValue instanceof String name && !name.isBlank()) {
            return name.trim().toUpperCase(Locale.ROOT);
        }

        return chunkType == null ? null : chunkType.toString().trim().toUpperCase(Locale.ROOT);
    }

    private boolean sameTown(final @Nullable Object firstTown, final @Nullable Object secondTown) {
        if (firstTown == null || secondTown == null) {
            return false;
        }
        if (firstTown == secondTown || firstTown.equals(secondTown)) {
            return true;
        }

        final Object firstIdentifier = this.firstNonNull(
            this.invokeOptional(firstTown, "getTownUUID"),
            this.invokeOptional(firstTown, "getTownUuid"),
            this.invokeOptional(firstTown, "getIdentifier"),
            this.invokeOptional(firstTown, "getId")
        );
        final Object secondIdentifier = this.firstNonNull(
            this.invokeOptional(secondTown, "getTownUUID"),
            this.invokeOptional(secondTown, "getTownUuid"),
            this.invokeOptional(secondTown, "getIdentifier"),
            this.invokeOptional(secondTown, "getId")
        );

        final UUID firstUuid = this.asUuid(firstIdentifier);
        final UUID secondUuid = this.asUuid(secondIdentifier);
        if (firstUuid != null && secondUuid != null) {
            return firstUuid.equals(secondUuid);
        }
        if (firstIdentifier != null && secondIdentifier != null) {
            return firstIdentifier.equals(secondIdentifier);
        }

        final Object firstName = this.firstNonNull(
            this.invokeOptional(firstTown, "getTownName"),
            this.invokeOptional(firstTown, "getName")
        );
        final Object secondName = this.firstNonNull(
            this.invokeOptional(secondTown, "getTownName"),
            this.invokeOptional(secondTown, "getName")
        );
        return firstName instanceof String first
            && secondName instanceof String second
            && first.equalsIgnoreCase(second);
    }

    private @NotNull String normalizeKey(final @NotNull String key) {
        return key.trim()
            .toLowerCase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_');
    }

    private @Nullable Object invokeOptional(final @NotNull Object target, final @NotNull String methodName, final Object... arguments) {
        final Method method = this.findMethod(target.getClass(), methodName, arguments);
        if (method == null) {
            return null;
        }

        try {
            method.setAccessible(true);
            return this.unwrapOptional(method.invoke(target, arguments));
        } catch (final ReflectiveOperationException ignored) {
            return null;
        }
    }

    private @Nullable Object readFieldOptional(final @NotNull Object target, final @NotNull String fieldName) {
        final Field field = this.findField(target.getClass(), fieldName);
        if (field == null) {
            return null;
        }

        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (final ReflectiveOperationException ignored) {
            return null;
        }
    }

    private boolean hasMethod(final @NotNull Class<?> owner, final @NotNull String methodName, final int parameterCount) {
        for (final Method method : owner.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
                return true;
            }
        }

        Class<?> current = owner;
        while (current != null) {
            for (final Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
                    return true;
                }
            }
            current = current.getSuperclass();
        }

        return false;
    }

    private @Nullable Method findMethod(final @NotNull Class<?> owner, final @NotNull String methodName, final Object... arguments) {
        for (final Method method : owner.getMethods()) {
            if (method.getName().equals(methodName) && this.parametersMatch(method.getParameterTypes(), arguments)) {
                return method;
            }
        }

        Class<?> current = owner;
        while (current != null) {
            for (final Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && this.parametersMatch(method.getParameterTypes(), arguments)) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private boolean parametersMatch(final @NotNull Class<?>[] parameterTypes, final @Nullable Object[] arguments) {
        final int argumentCount = arguments == null ? 0 : arguments.length;
        if (parameterTypes.length != argumentCount) {
            return false;
        }

        for (int index = 0; index < parameterTypes.length; index++) {
            final Class<?> parameterType = this.wrapPrimitive(parameterTypes[index]);
            final Object argument = arguments[index];

            if (argument == null) {
                if (parameterTypes[index].isPrimitive()) {
                    return false;
                }
                continue;
            }

            if (!parameterType.isInstance(argument)) {
                return false;
            }
        }

        return true;
    }

    private @NotNull Class<?> wrapPrimitive(final @NotNull Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }

        return switch (type.getName()) {
            case "boolean" -> Boolean.class;
            case "byte" -> Byte.class;
            case "char" -> Character.class;
            case "short" -> Short.class;
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "float" -> Float.class;
            case "double" -> Double.class;
            default -> type;
        };
    }

    private @Nullable Field findField(final @NotNull Class<?> owner, final @NotNull String fieldName) {
        Class<?> current = owner;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (final NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private @Nullable Object unwrapOptional(final @Nullable Object value) {
        return value instanceof Optional<?> optional ? optional.orElse(null) : value;
    }

    private @Nullable Object firstNonNull(final @Nullable Object... values) {
        for (final Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private @Nullable UUID asUuid(final @Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String textValue) {
            try {
                return UUID.fromString(textValue);
            } catch (final IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private double asDouble(final @Nullable Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0D;
    }
}
