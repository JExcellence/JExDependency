package de.jexcellence.economy.command.player.currencies;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ECurrenciesPermissionTest {

    @Test
    void shouldExposeExpectedInternalIdentifiersAndFallbackNodes() {
        assertAll(
                () -> assertEquals("command", ECurrenciesPermission.CURRENCIES.getInternalName()),
                () -> assertEquals("commandCreate", ECurrenciesPermission.CREATE.getInternalName()),
                () -> assertEquals("commandDelete", ECurrenciesPermission.DELETE.getInternalName()),
                () -> assertEquals("commandEdit", ECurrenciesPermission.EDIT.getInternalName()),
                () -> assertEquals("commandOverview", ECurrenciesPermission.OVERVIEW.getInternalName())
        );

        assertAll(
                () -> assertEquals("currencies.command", ECurrenciesPermission.CURRENCIES.getFallbackNode()),
                () -> assertEquals("currencies.command.create", ECurrenciesPermission.CREATE.getFallbackNode()),
                () -> assertEquals("currencies.command.delete", ECurrenciesPermission.DELETE.getFallbackNode()),
                () -> assertEquals("currencies.command.update", ECurrenciesPermission.EDIT.getFallbackNode()),
                () -> assertEquals("currencies.command.overview", ECurrenciesPermission.OVERVIEW.getFallbackNode())
        );
    }

    @Test
    void shouldResolvePermissionsFromInternalNames() {
        for (final ECurrenciesPermission permission : ECurrenciesPermission.values()) {
            final Optional<ECurrenciesPermission> resolved = ECurrenciesPermission.fromInternalName(permission.getInternalName());
            assertTrue(resolved.isPresent());
            assertSame(permission, resolved.orElseThrow());
        }

        assertAll(
                () -> assertTrue(ECurrenciesPermission.fromInternalName(null).isEmpty()),
                () -> assertTrue(ECurrenciesPermission.fromInternalName(" ").isEmpty()),
                () -> assertTrue(ECurrenciesPermission.fromInternalName("COMMAND").isEmpty())
        );
    }

    @Test
    void shouldResolvePermissionsFromFallbackNodes() {
        for (final ECurrenciesPermission permission : ECurrenciesPermission.values()) {
            final Optional<ECurrenciesPermission> resolved = ECurrenciesPermission.fromFallbackNode(permission.getFallbackNode());
            assertTrue(resolved.isPresent());
            assertSame(permission, resolved.orElseThrow());
        }

        assertAll(
                () -> assertTrue(ECurrenciesPermission.fromFallbackNode(null).isEmpty()),
                () -> assertTrue(ECurrenciesPermission.fromFallbackNode("\t").isEmpty()),
                () -> assertTrue(ECurrenciesPermission.fromFallbackNode("currencies.command.CREATE").isEmpty())
        );
    }

    @Test
    void shouldIdentifyAdministrativePermissions() {
        final Set<ECurrenciesPermission> administrativePermissions = EnumSet.of(
                ECurrenciesPermission.CREATE,
                ECurrenciesPermission.DELETE,
                ECurrenciesPermission.EDIT
        );

        for (final ECurrenciesPermission permission : ECurrenciesPermission.values()) {
            final boolean expected = administrativePermissions.contains(permission);
            assertEquals(expected, permission.isAdministrativePermission(),
                    () -> "Unexpected administrative flag for " + permission.name());
        }
    }

    @Test
    void shouldIdentifyDestructivePermissions() {
        final Set<ECurrenciesPermission> destructivePermissions = EnumSet.of(ECurrenciesPermission.DELETE);

        for (final ECurrenciesPermission permission : ECurrenciesPermission.values()) {
            final boolean expected = destructivePermissions.contains(permission);
            assertEquals(expected, permission.isDestructivePermission(),
                    () -> "Unexpected destructive flag for " + permission.name());
        }
    }
}
