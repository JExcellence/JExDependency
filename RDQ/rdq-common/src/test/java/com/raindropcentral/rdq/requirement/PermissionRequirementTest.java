package com.raindropcentral.rdq.requirement;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import be.seeseemelk.mockbukkit.plugin.MockPlugin;
import org.bukkit.permissions.PermissionAttachment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PermissionRequirementTest {

    private static final double DELTA = 1.0E-6;

    private ServerMock server;
    private PlayerMock player;
    private MockPlugin plugin;
    private PermissionAttachment attachment;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin();
        this.player = this.server.addPlayer("PermissionRequirementTest");
        this.attachment = this.player.addAttachment(this.plugin);
    }

    @AfterEach
    void tearDown() {
        if (this.player != null && this.attachment != null) {
            this.player.removeAttachment(this.attachment);
        }
        MockBukkit.unmock();
    }

    @Test
    void isMetAndProgressRequireAllPermissions() {
        List<String> permissions = List.of("rdq.permission.first", "rdq.permission.second");
        PermissionRequirement requirement = new PermissionRequirement(permissions, PermissionRequirement.PermissionMode.ALL);

        assertFalse(requirement.isMet(this.player));
        assertEquals(0.0D, requirement.calculateProgress(this.player), DELTA);

        this.attachment.setPermission("rdq.permission.first", true);

        assertFalse(requirement.isMet(this.player));
        assertEquals(0.5D, requirement.calculateProgress(this.player), DELTA);

        this.attachment.setPermission("rdq.permission.second", true);

        assertTrue(requirement.isMet(this.player));
        assertEquals(1.0D, requirement.calculateProgress(this.player), DELTA);

        this.attachment.setPermission("rdq.permission.second", false);

        assertFalse(requirement.isMet(this.player));
        assertEquals(0.5D, requirement.calculateProgress(this.player), DELTA);
    }

    @Test
    void isMetAndProgressAcceptAnyPermission() {
        List<String> permissions = List.of("rdq.permission.alpha", "rdq.permission.beta");
        PermissionRequirement requirement = new PermissionRequirement(permissions, PermissionRequirement.PermissionMode.ANY);

        assertFalse(requirement.isMet(this.player));
        assertEquals(0.0D, requirement.calculateProgress(this.player), DELTA);

        this.attachment.setPermission("rdq.permission.alpha", true);

        assertTrue(requirement.isMet(this.player));
        assertEquals(1.0D, requirement.calculateProgress(this.player), DELTA);

        this.attachment.setPermission("rdq.permission.alpha", false);
        this.attachment.setPermission("rdq.permission.beta", true);

        assertTrue(requirement.isMet(this.player));
        assertEquals(1.0D, requirement.calculateProgress(this.player), DELTA);

        this.attachment.setPermission("rdq.permission.beta", false);

        assertFalse(requirement.isMet(this.player));
        assertEquals(0.0D, requirement.calculateProgress(this.player), DELTA);
    }

    @Test
    void isMetAndProgressRespectMinimumRequirement() {
        List<String> permissions = List.of(
                "rdq.permission.one",
                "rdq.permission.two",
                "rdq.permission.three"
        );
        PermissionRequirement requirement = new PermissionRequirement(
                permissions,
                PermissionRequirement.PermissionMode.MINIMUM,
                2,
                null,
                false
        );

        assertFalse(requirement.isMet(this.player));
        assertEquals(0.0D, requirement.calculateProgress(this.player), DELTA);

        this.attachment.setPermission("rdq.permission.one", true);

        assertFalse(requirement.isMet(this.player));
        assertEquals(0.5D, requirement.calculateProgress(this.player), DELTA);

        this.attachment.setPermission("rdq.permission.two", true);

        assertTrue(requirement.isMet(this.player));
        assertEquals(1.0D, requirement.calculateProgress(this.player), DELTA);

        this.attachment.setPermission("rdq.permission.three", true);

        assertTrue(requirement.isMet(this.player));
        assertEquals(1.0D, requirement.calculateProgress(this.player), DELTA);

        this.attachment.setPermission("rdq.permission.one", false);
        this.attachment.setPermission("rdq.permission.two", false);
        this.attachment.setPermission("rdq.permission.three", false);

        assertFalse(requirement.isMet(this.player));
        assertEquals(0.0D, requirement.calculateProgress(this.player), DELTA);
    }

    @Test
    void negatedChecksInvertPermissionPresence() {
        List<String> permissions = List.of("rdq.permission.negated.one", "rdq.permission.negated.two");
        PermissionRequirement requirement = new PermissionRequirement(
                permissions,
                PermissionRequirement.PermissionMode.ALL,
                permissions.size(),
                "Negated permissions must be absent",
                true
        );

        assertTrue(requirement.isMet(this.player));
        assertEquals(1.0D, requirement.calculateProgress(this.player), DELTA);
        assertTrue(requirement.isCheckNegated());

        this.attachment.setPermission("rdq.permission.negated.one", true);

        assertFalse(requirement.isMet(this.player));
        assertEquals(0.0D, requirement.calculateProgress(this.player), DELTA);

        this.attachment.setPermission("rdq.permission.negated.one", false);
        this.attachment.setPermission("rdq.permission.negated.two", true);

        assertFalse(requirement.isMet(this.player));
        assertEquals(0.5D, requirement.calculateProgress(this.player), DELTA);
    }

    @Test
    void helperAccessorsReflectConfiguration() {
        List<String> permissions = new ArrayList<>(List.of(
                "rdq.permission.accessor.one",
                "rdq.permission.accessor.two",
                "rdq.permission.accessor.three"
        ));
        PermissionRequirement requirement = new PermissionRequirement(
                permissions,
                PermissionRequirement.PermissionMode.MINIMUM,
                2,
                "Two permissions needed",
                false
        );

        assertFalse(requirement.isCheckNegated());
        assertFalse(requirement.isAllMode());
        assertFalse(requirement.isAnyMode());
        assertTrue(requirement.isMinimumMode());
        assertEquals("Two permissions needed", requirement.getDescription());
        assertEquals("requirement.permission", requirement.getDescriptionKey());

        this.attachment.setPermission("rdq.permission.accessor.one", true);
        this.attachment.setPermission("rdq.permission.accessor.two", true);

        List<String> held = requirement.getHeldPermissions(this.player);
        List<String> missing = requirement.getMissingPermissions(this.player);

        assertEquals(List.of(
                "rdq.permission.accessor.one",
                "rdq.permission.accessor.two"
        ), held);
        assertEquals(List.of("rdq.permission.accessor.three"), missing);

        List<String> returned = requirement.getRequiredPermissions();
        assertNotSame(permissions, returned);
        returned.add("rdq.permission.extra");
        assertEquals(3, requirement.getRequiredPermissions().size());
        assertEquals(PermissionRequirement.PermissionMode.MINIMUM, requirement.getPermissionMode());
        assertEquals(2, requirement.getMinimumRequired());
    }

    @Test
    void validateAcceptsValidConfiguration() {
        PermissionRequirement requirement = new PermissionRequirement(
                List.of("rdq.permission.valid.one", "rdq.permission.valid.two"),
                PermissionRequirement.PermissionMode.ALL,
                2,
                null,
                false
        );

        assertDoesNotThrow(requirement::validate);
    }

    @Test
    void validateRejectsEmptyPermissions() throws Exception {
        PermissionRequirement requirement = new PermissionRequirement(
                List.of("rdq.permission.valid"),
                PermissionRequirement.PermissionMode.ALL,
                1,
                null,
                false
        );

        Field permissionsField = PermissionRequirement.class.getDeclaredField("requiredPermissions");
        permissionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> internal = (List<String>) permissionsField.get(requirement);
        internal.clear();

        assertThrows(IllegalStateException.class, requirement::validate);
    }

    @Test
    void validateRejectsMinimumLessThanOne() throws Exception {
        PermissionRequirement requirement = new PermissionRequirement(
                List.of("rdq.permission.valid.one", "rdq.permission.valid.two"),
                PermissionRequirement.PermissionMode.MINIMUM,
                1,
                null,
                false
        );

        Field minimumField = PermissionRequirement.class.getDeclaredField("minimumRequired");
        minimumField.setAccessible(true);
        minimumField.setInt(requirement, 0);

        assertThrows(IllegalStateException.class, requirement::validate);
    }

    @Test
    void validateRejectsMinimumGreaterThanPermissions() throws Exception {
        PermissionRequirement requirement = new PermissionRequirement(
                List.of("rdq.permission.valid.one", "rdq.permission.valid.two"),
                PermissionRequirement.PermissionMode.MINIMUM,
                1,
                null,
                false
        );

        Field minimumField = PermissionRequirement.class.getDeclaredField("minimumRequired");
        minimumField.setAccessible(true);
        minimumField.setInt(requirement, 3);

        assertThrows(IllegalStateException.class, requirement::validate);
    }

    @Test
    void validateRejectsBlankPermissionEntries() throws Exception {
        PermissionRequirement requirement = new PermissionRequirement(
                List.of("rdq.permission.valid.one", "rdq.permission.valid.two"),
                PermissionRequirement.PermissionMode.ALL,
                2,
                null,
                false
        );

        Field permissionsField = PermissionRequirement.class.getDeclaredField("requiredPermissions");
        permissionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> internal = (List<String>) permissionsField.get(requirement);
        internal.set(0, "   ");

        assertThrows(IllegalStateException.class, requirement::validate);
    }

    @Test
    void fromStringRejectsInvalidMode() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                PermissionRequirement.fromString(
                        List.of("rdq.permission.valid"),
                        "invalid",
                        1
                )
        );

        assertTrue(exception.getMessage().contains("Invalid permission mode"));
    }
}
