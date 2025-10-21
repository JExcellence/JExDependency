package com.raindropcentral.rplatform.utility.itembuilder.skull;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.OfflinePlayerMock;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.raindropcentral.rplatform.version.ServerEnvironment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyHeadBuilderTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        resetServerEnvironment();
        installServerEnvironment(false);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        resetServerEnvironment();
    }

    @Test
    void legacyPlayerHeadAppliesOwnerAndMetadata() {
        PlayerMock player = this.server.addPlayer("LegacyOwner");

        Component name = Component.text("Relic", NamedTextColor.GOLD);
        List<Component> lore = List.of(
                Component.text("Ancient", NamedTextColor.DARK_PURPLE),
                Component.text("Artifact", NamedTextColor.GRAY)
        );

        ItemStack result = new LegacyHeadBuilder()
                .setPlayerHead(player)
                .setName(name)
                .setLore(lore)
                .build();

        SkullMeta meta = (SkullMeta) assertNotNull(result.getItemMeta());
        assertEquals("LegacyOwner", meta.getOwner());

        String expectedName = LegacyComponentSerializer.legacySection().serialize(name);
        assertEquals(expectedName, meta.getDisplayName());

        List<String> expectedLore = lore.stream()
                .map(component -> LegacyComponentSerializer.legacySection().serialize(component))
                .toList();
        assertEquals(expectedLore, meta.getLore());
    }

    @Test
    void legacyOfflinePlayerHeadTracksOwningPlayer() {
        OfflinePlayerMock offlinePlayer = new OfflinePlayerMock(this.server, "LegacyOffline");

        ItemStack result = new LegacyHeadBuilder()
                .setPlayerHead(offlinePlayer)
                .build();

        SkullMeta meta = (SkullMeta) assertNotNull(result.getItemMeta());
        assertEquals("LegacyOffline", meta.getOwner());
        assertEquals(offlinePlayer, meta.getOwningPlayer());
    }

    @Test
    void customTextureBuildsGameProfileAndLegacyMetadata() throws Exception {
        UUID profileId = UUID.fromString("f5f2a741-7c79-42cf-8e15-892b4d50a6db");
        String texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly9leGFtcGxlLmNvbS90ZXh0dXJlLnBuZyJ9fX0=";

        Component name = Component.text("Custom", NamedTextColor.AQUA);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Line One", NamedTextColor.BLUE));
        lore.add(Component.text("Line Two", NamedTextColor.WHITE));

        ItemStack result = new LegacyHeadBuilder()
                .setName(name)
                .setLore(lore)
                .setCustomTexture(profileId, texture)
                .build();

        SkullMeta meta = (SkullMeta) assertNotNull(result.getItemMeta());

        String expectedName = LegacyComponentSerializer.legacySection().serialize(name);
        assertEquals(expectedName, meta.getDisplayName());

        List<String> expectedLore = lore.stream()
                .map(component -> LegacyComponentSerializer.legacySection().serialize(component))
                .toList();
        assertEquals(expectedLore, meta.getLore());

        Object profile = extractProfile(meta);
        assertNotNull(profile);

        Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
        assertTrue(gameProfileClass.isInstance(profile));

        Method getId = gameProfileClass.getMethod("getId");
        Method getName = gameProfileClass.getMethod("getName");
        Method getProperties = gameProfileClass.getMethod("getProperties");

        assertEquals(profileId, getId.invoke(profile));
        assertEquals("CustomHead", getName.invoke(profile));

        Object propertyMap = getProperties.invoke(profile);
        Method getMethod = propertyMap.getClass().getMethod("get", Object.class);
        @SuppressWarnings("unchecked")
        Collection<Object> textures = (Collection<Object>) getMethod.invoke(propertyMap, "textures");
        assertNotNull(textures);
        assertFalse(textures.isEmpty());

        Object property = textures.iterator().next();
        Method getValue = property.getClass().getMethod("getValue");
        assertEquals(texture, getValue.invoke(property));
    }

    @Test
    void customTextureRejectsNullUuid() {
        LegacyHeadBuilder builder = new LegacyHeadBuilder();
        assertThrows(RuntimeException.class, () -> builder.setCustomTexture(null, "value"));
    }

    @Test
    void customTextureRejectsNullTexture() {
        LegacyHeadBuilder builder = new LegacyHeadBuilder();
        assertThrows(RuntimeException.class, () -> builder.setCustomTexture(UUID.randomUUID(), null));
    }

    private Object extractProfile(SkullMeta meta) throws Exception {
        try {
            Method getProfile = meta.getClass().getDeclaredMethod("getProfile");
            getProfile.setAccessible(true);
            Object profile = getProfile.invoke(meta);
            return unwrapProfile(profile);
        } catch (NoSuchMethodException ignored) {
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            Object profile = profileField.get(meta);
            return unwrapProfile(profile);
        }
    }

    private Object unwrapProfile(Object profile) throws Exception {
        if (profile == null) {
            return null;
        }

        if ("ResolvableProfile".equals(profile.getClass().getSimpleName())) {
            try {
                Method gameProfileMethod = profile.getClass().getMethod("gameProfile");
                Object optional = gameProfileMethod.invoke(profile);
                if (optional instanceof Optional<?> optionalProfile) {
                    return optionalProfile.orElse(null);
                }
                return optional;
            } catch (NoSuchMethodException first) {
                Method profileMethod = profile.getClass().getMethod("profile");
                Object optional = profileMethod.invoke(profile);
                if (optional instanceof Optional<?> optionalProfile) {
                    return optionalProfile.orElse(null);
                }
                return optional;
            }
        }

        return profile;
    }

    private void installServerEnvironment(boolean paper) {
        try {
            ServerEnvironment environment = Mockito.mock(ServerEnvironment.class);
            Mockito.when(environment.isPaper()).thenReturn(paper);

            Field instance = ServerEnvironment.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, environment);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to install test server environment", exception);
        }
    }

    private void resetServerEnvironment() {
        try {
            Field instance = ServerEnvironment.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to reset server environment", exception);
        }
    }
}
