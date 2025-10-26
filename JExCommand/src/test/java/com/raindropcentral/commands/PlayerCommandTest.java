package com.raindropcentral.commands;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.raindropcentral.commands.testplugin.command.TestCommandSection;
import de.jexcellence.evaluable.error.CommandError;
import de.jexcellence.evaluable.error.EErrorType;
import de.jexcellence.evaluable.section.IPermissionNode;
import de.jexcellence.evaluable.section.PermissionsSection;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PlayerCommandTest {

    private ServerMock server;
    private ConsoleCommandSenderMock consoleSender;
    private PlayerMock player;
    private TestableCommandSection commandSection;
    private PermissionsSection permissionsSection;
    private IPermissionNode permissionNode;
    private TestPlayerCommand command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        MockBukkit.createMockPlugin();
        this.consoleSender = this.server.getConsoleSender();
        this.player = this.server.addPlayer("PlayerCommandTester");
        this.commandSection = new TestableCommandSection();
        this.permissionsSection = mock(PermissionsSection.class);
        this.permissionNode = mock(IPermissionNode.class);
        this.commandSection.setPermissions(this.permissionsSection);
        this.command = new TestPlayerCommand(this.commandSection);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onInvocationThrowsCommandErrorForNonPlayerSenders() {
        CommandError error = assertThrows(CommandError.class, () -> this.command.onInvocation(
                this.consoleSender,
                "alias",
                new String[]{"arg"}
        ));

        assertEquals(EErrorType.NOT_A_PLAYER, error.errorType);
        assertFalse(this.command.invocationCalled());
    }

    @Test
    void onInvocationDelegatesToPlayerHandler() {
        String[] args = {"first", "second"};

        this.command.onInvocation(this.player, "alias", args);

        assertTrue(this.command.invocationCalled());
        assertSame(this.player, this.command.lastInvocationPlayer());
        assertEquals("alias", this.command.lastInvocationAlias());
        assertEquals(List.of("first", "second"), this.command.lastInvocationArguments());
    }

    @Test
    void onTabCompletionReturnsEmptyListForNonPlayers() {
        List<String> suggestions = this.command.onTabCompletion(
                this.consoleSender,
                "alias",
                new String[]{"partial"}
        );

        assertEquals(List.of(), suggestions);
        assertFalse(this.command.tabCompletionCalled());
    }

    @Test
    void onTabCompletionDelegatesForPlayerSenders() {
        List<String> expected = List.of("one", "two");
        this.command.setTabCompletionResult(expected);

        List<String> suggestions = this.command.onTabCompletion(
                this.player,
                "alias",
                new String[]{"partial"}
        );

        assertEquals(expected, suggestions);
        assertTrue(this.command.tabCompletionCalled());
        assertSame(this.player, this.command.lastTabCompletionPlayer());
        assertEquals("alias", this.command.lastTabCompletionAlias());
        assertEquals(List.of("partial"), this.command.lastTabCompletionArguments());
    }

    @Test
    void hasNoPermissionReturnsFalseWhenSectionMissing() {
        this.commandSection.setPermissions(null);

        boolean denied = this.command.hasNoPermission(this.player, this.permissionNode);

        assertFalse(denied);
        verifyNoInteractions(this.permissionsSection);
    }

    @Test
    void hasNoPermissionReturnsFalseWhenPermissionGranted() {
        when(this.permissionsSection.hasPermission(this.player, this.permissionNode)).thenReturn(true);

        boolean denied = this.command.hasNoPermission(this.player, this.permissionNode);

        assertFalse(denied);
        verify(this.permissionsSection).hasPermission(this.player, this.permissionNode);
        verify(this.permissionsSection, never()).sendMissingMessage(this.player, this.permissionNode);
    }

    @Test
    void hasNoPermissionReturnsTrueAndSendsMessageWhenDenied() {
        when(this.permissionsSection.hasPermission(this.player, this.permissionNode)).thenReturn(false);

        boolean denied = this.command.hasNoPermission(this.player, this.permissionNode);

        assertTrue(denied);
        verify(this.permissionsSection).hasPermission(this.player, this.permissionNode);
        verify(this.permissionsSection).sendMissingMessage(this.player, this.permissionNode);
    }

    private static final class TestPlayerCommand extends PlayerCommand {

        private final List<InvocationRecord> invocations = new CopyOnWriteArrayList<>();
        private final List<TabRecord> tabCompletions = new CopyOnWriteArrayList<>();
        private volatile List<String> tabCompletionResult = List.of();

        private TestPlayerCommand(TestCommandSection section) {
            super(section);
        }

        @Override
        protected void onPlayerInvocation(Player player, String alias, String[] args) {
            this.invocations.add(new InvocationRecord(player, alias, List.copyOf(Arrays.asList(args.clone()))));
        }

        @Override
        protected List<String> onPlayerTabCompletion(Player player, String alias, String[] args) {
            this.tabCompletions.add(new TabRecord(player, alias, List.copyOf(Arrays.asList(args.clone()))));
            return this.tabCompletionResult;
        }

        void setTabCompletionResult(List<String> tabCompletionResult) {
            this.tabCompletionResult = tabCompletionResult;
        }

        boolean invocationCalled() {
            return !this.invocations.isEmpty();
        }

        Player lastInvocationPlayer() {
            return this.invocations.get(this.invocations.size() - 1).player();
        }

        String lastInvocationAlias() {
            return this.invocations.get(this.invocations.size() - 1).alias();
        }

        List<String> lastInvocationArguments() {
            return this.invocations.get(this.invocations.size() - 1).arguments();
        }

        boolean tabCompletionCalled() {
            return !this.tabCompletions.isEmpty();
        }

        Player lastTabCompletionPlayer() {
            return this.tabCompletions.get(this.tabCompletions.size() - 1).player();
        }

        String lastTabCompletionAlias() {
            return this.tabCompletions.get(this.tabCompletions.size() - 1).alias();
        }

        List<String> lastTabCompletionArguments() {
            return this.tabCompletions.get(this.tabCompletions.size() - 1).arguments();
        }

        private record InvocationRecord(Player player, String alias, List<String> arguments) {
        }

        private record TabRecord(Player player, String alias, List<String> arguments) {
        }
    }

    private static final class TestableCommandSection extends TestCommandSection {

        private PermissionsSection permissions;

        private TestableCommandSection() {
            super("contextawarecommand");
        }

        void setPermissions(PermissionsSection permissions) {
            this.permissions = permissions;
        }

        @Override
        public PermissionsSection getPermissions() {
            return this.permissions;
        }
    }
}
