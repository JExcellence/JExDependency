package com.raindropcentral.rdq.reward;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.MockPlugin;
import org.mockbukkit.mockbukkit.scheduler.BukkitSchedulerMock;
import org.mockbukkit.mockbukkit.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;

class CommandRewardTest {

    private ServerMock server;
    private MockPlugin plugin;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.server.addSimpleWorld("world");
        this.plugin = MockBukkit.createMockPlugin("RaindropQuests");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void constructorRejectsBlankCommand() {
        assertThrows(IllegalArgumentException.class, () -> new CommandReward(""));
    }

    @Test
    void applyExecutesCommandAsPlayerWithResolvedPlaceholders() {
        PlayerMock player = Mockito.spy(this.server.addPlayer("TestPlayer"));
        Mockito.doReturn(true).when(player).performCommand(anyString());

        CommandReward reward = new CommandReward("/give Test %player%", true, 0L);

        reward.apply(player);

        Mockito.verify(player).performCommand("/give Test TestPlayer");
    }

    @Test
    void applyExecutesCommandAsConsoleWithResolvedPlaceholders() {
        PlayerMock player = this.server.addPlayer("ConsolePlayer");
        CommandReward reward = new CommandReward("/give Test %player%", false, 0L);

        ConsoleCommandSender consoleSender = this.server.getConsoleSender();

        try (MockedStatic<Bukkit> mockedBukkit = Mockito.mockStatic(Bukkit.class, Answers.CALLS_REAL_METHODS)) {
            mockedBukkit.when(Bukkit::getConsoleSender).thenReturn(consoleSender);
            mockedBukkit.when(() -> Bukkit.dispatchCommand(consoleSender, "/give Test ConsolePlayer")).thenReturn(true);

            reward.apply(player);

            mockedBukkit.verify(() -> Bukkit.dispatchCommand(consoleSender, "/give Test ConsolePlayer"));
        }
    }

    @Test
    void applySchedulesDelayedCommandExecution() {
        PlayerMock player = this.server.addPlayer("DelayedPlayer");
        CommandReward reward = new CommandReward("/give Test %player%", false, 40L);

        BukkitSchedulerMock scheduler = this.server.getScheduler();
        ConsoleCommandSender consoleSender = this.server.getConsoleSender();
        AtomicReference<String> dispatchedCommand = new AtomicReference<>();

        try (MockedStatic<Bukkit> mockedBukkit = Mockito.mockStatic(Bukkit.class, Answers.CALLS_REAL_METHODS)) {
            mockedBukkit.when(Bukkit::getConsoleSender).thenReturn(consoleSender);
            mockedBukkit.when(() -> Bukkit.dispatchCommand(consoleSender, Mockito.anyString())).thenAnswer(invocation -> {
                dispatchedCommand.set(invocation.getArgument(1, String.class));
                return true;
            });

            reward.apply(player);

            List<BukkitTask> pendingTasks = scheduler.getPendingTasks();
            assertEquals(1, pendingTasks.size(), "CommandReward should schedule a delayed task");

            ScheduledTask task = (ScheduledTask) pendingTasks.get(0);
            assertSame(this.plugin, task.getOwner(), "Scheduled task should be owned by the RaindropQuests plugin");
            assertEquals(scheduler.getCurrentTick() + 40L, task.getScheduledTick(), "Task should be scheduled for the configured delay");

            task.run();

            mockedBukkit.verify(() -> Bukkit.dispatchCommand(consoleSender, "/give Test DelayedPlayer"), times(1));
            assertEquals("/give Test DelayedPlayer", dispatchedCommand.get());
        }
    }
}
