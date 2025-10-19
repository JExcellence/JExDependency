package com.raindropcentral.commands;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.command.ConsoleCommandSenderMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.raindropcentral.commands.testplugin.command.TestCommandSection;
import de.jexcellence.evaluable.error.CommandError;
import de.jexcellence.evaluable.error.EErrorType;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerCommandTest {

    private ServerMock server;
    private ConsoleCommandSenderMock consoleSender;
    private PlayerMock playerSender;
    private RecordingCommandSection commandSection;
    private TestServerCommand command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        MockBukkit.createMockPlugin();
        this.consoleSender = this.server.getConsoleSender();
        this.playerSender = this.server.addPlayer("TestPlayer");
        this.commandSection = new RecordingCommandSection();
        this.command = new TestServerCommand(this.commandSection);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onInvocationRejectsNonConsoleSenders() {
        String[] args = {"arg"};

        CommandError error = assertThrows(CommandError.class, () -> this.command.onInvocation(this.playerSender, "alias", args));

        assertEquals(EErrorType.NOT_A_CONSOLE, error.errorType);
        assertEquals(0, this.command.invocations.size());
    }

    @Test
    void onInvocationDelegatesToConsoleSubclass() {
        String[] args = {"one", "two"};

        this.command.onInvocation(this.consoleSender, "alias", args);

        assertEquals(1, this.command.invocations.size());
        TestServerCommand.Invocation invocation = this.command.invocations.get(0);
        assertSame(this.consoleSender, invocation.console());
        assertEquals("alias", invocation.alias());
        assertEquals(List.of("one", "two"), invocation.arguments());
    }

    @Test
    void onTabCompletionAlwaysReturnsEmptyList() {
        List<String> consoleResult = this.command.onTabCompletion(this.consoleSender, "alias", new String[]{"value"});
        List<String> playerResult = this.command.onTabCompletion(this.playerSender, "alias", new String[]{"value"});

        assertEquals(List.of(), consoleResult);
        assertEquals(List.of(), playerResult);
    }

    private static final class TestServerCommand extends ServerCommand {

        private final CopyOnWriteArrayList<Invocation> invocations = new CopyOnWriteArrayList<>();

        private TestServerCommand(RecordingCommandSection section) {
            super(section);
        }

        @Override
        protected void onPlayerInvocation(
                final @NotNull ConsoleCommandSender console,
                final @NotNull String alias,
                final @NotNull String[] args
        ) {
            this.invocations.add(new Invocation(console, alias, List.of(args)));
        }

        private record Invocation(ConsoleCommandSender console, String alias, List<String> arguments) {
        }
    }

    private static final class RecordingCommandSection extends TestCommandSection {

        private RecordingCommandSection() {
            super("contextawarecommand");
        }
    }
}
