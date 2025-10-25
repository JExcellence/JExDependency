package de.jexcellence.economy.migrate;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.migrate.VaultMigrationManager.MigrationResult;
import de.jexcellence.economy.migrate.VaultMigrationManager.MigrationStats;
import org.bukkit.command.Command;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationCommandTest {

    private ServerMock server;
    private ConsoleCommandSenderMock consoleSender;
    private JExEconomyImpl plugin;
    private VaultMigrationManager migrationManager;
    private MigrationCommand command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.consoleSender = this.server.getConsoleSender();
        this.consoleSender.setOp(true);

        this.plugin = Mockito.mock(JExEconomyImpl.class);
        this.migrationManager = Mockito.mock(VaultMigrationManager.class);
        Mockito.when(this.plugin.getVaultMigrationManager()).thenReturn(this.migrationManager);
        Mockito.when(this.plugin.getLogger()).thenReturn(Logger.getLogger("MigrationCommandTest"));

        this.command = new MigrationCommand(this.plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void startCommandTriggersMigrationAndReportsSuccess() {
        Mockito.when(this.migrationManager.isMigrationInProgress()).thenReturn(false);
        CompletableFuture<MigrationResult> future = new CompletableFuture<>();
        Mockito.when(this.migrationManager.startMigration(true, false)).thenReturn(future);

        Command commandHandle = Mockito.mock(Command.class);

        boolean handled = this.command.onCommand(
                this.consoleSender,
                commandHandle,
                "jexeconomy",
                new String[]{"migrate", "start"}
        );

        assertTrue(handled, "Command should indicate it handled valid input");
        Mockito.verify(this.migrationManager).startMigration(true, false);

        assertMessageContains("Starting Vault to JExEconomyImpl migration...");
        assertNotNull(nextConsoleMessage(), "Expected backup summary message");
        assertNotNull(nextConsoleMessage(), "Expected replace summary message");
        assertNotNull(nextConsoleMessage(), "Expected time warning message");
        assertMessageContains("Migration started in background");

        MigrationStats stats = new MigrationStats();
        stats.setSuccess(true);
        stats.incrementProcessed();
        stats.incrementSuccessful();

        future.complete(new MigrationResult(true, "Essentials", stats, null));

        assertMessageContains("Migration completed successfully");
        assertMessageContains("Migration Summary");
        assertMessageContains("Players Processed");
    }

    @Test
    void startCommandRejectsActiveMigration() {
        Mockito.when(this.migrationManager.isMigrationInProgress()).thenReturn(true);

        Command commandHandle = Mockito.mock(Command.class);
        this.command.onCommand(
                this.consoleSender,
                commandHandle,
                "jexeconomy",
                new String[]{"migrate", "start"}
        );

        Mockito.verify(this.migrationManager, Mockito.never()).startMigration(Mockito.anyBoolean(), Mockito.anyBoolean());
        assertMessageContains("Migration is already in progress");
    }

    @Test
    void commandRejectsInvalidArguments() {
        Command commandHandle = Mockito.mock(Command.class);

        this.command.onCommand(
                this.consoleSender,
                commandHandle,
                "jexeconomy",
                new String[]{"status"}
        );

        Mockito.verify(this.migrationManager, Mockito.never()).startMigration(Mockito.anyBoolean(), Mockito.anyBoolean());
        assertMessageContains("Migration Commands");
    }

    @Test
    void startCommandPropagatesFailureMessages() {
        Mockito.when(this.migrationManager.isMigrationInProgress()).thenReturn(false);
        CompletableFuture<MigrationResult> future = new CompletableFuture<>();
        Mockito.when(this.migrationManager.startMigration(true, false)).thenReturn(future);

        Command commandHandle = Mockito.mock(Command.class);
        this.command.onCommand(
                this.consoleSender,
                commandHandle,
                "jexeconomy",
                new String[]{"migrate", "start"}
        );

        drainMessages();

        future.complete(MigrationResult.error("Test failure"));

        assertMessageContains("Migration failed: Test failure");
    }

    @Test
    void startCommandParsesOptionsForMigration() {
        Mockito.when(this.migrationManager.isMigrationInProgress()).thenReturn(false);
        CompletableFuture<MigrationResult> future = new CompletableFuture<>();
        Mockito.when(this.migrationManager.startMigration(false, true)).thenReturn(future);

        Command commandHandle = Mockito.mock(Command.class);
        this.command.onCommand(
                this.consoleSender,
                commandHandle,
                "jexeconomy",
                new String[]{"migrate", "start", "--no-backup", "--replace-vault"}
        );

        Mockito.verify(this.migrationManager).startMigration(false, true);
    }

    private void assertMessageContains(String expectedContent) {
        String message = nextConsoleMessage();
        assertNotNull(message, "Expected console output containing '" + expectedContent + "'");
        assertTrue(
                message.contains(expectedContent),
                () -> "Expected message to contain '" + expectedContent + "' but was '" + message + "'"
        );
    }

    private String nextConsoleMessage() {
        try {
            return this.consoleSender.nextMessage();
        } catch (IndexOutOfBoundsException ignored) {
            return null;
        }
    }

    private void drainMessages() {
        while (true) {
            String message = nextConsoleMessage();
            if (message == null) {
                return;
            }
        }
    }
}
