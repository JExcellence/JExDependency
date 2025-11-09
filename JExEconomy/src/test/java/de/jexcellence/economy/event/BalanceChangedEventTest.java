package de.jexcellence.economy.event;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import be.seeseemelk.mockbukkit.plugin.MockPlugin;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.type.EChangeType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalanceChangedEventTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        HandlerList.unregisterAll();
        this.server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        HandlerList.unregisterAll();
        MockBukkit.unmock();
    }

    @Test
    void shouldExposeSuppliedEventData() {
        PlayerMock initiator = this.server.addPlayer("Initiator");
        User user = new User(UUID.randomUUID(), "TargetPlayer");
        Currency currency = new Currency("coins");
        double oldBalance = 150.75D;
        double newBalance = 200.00D;
        String reason = "Quest reward";

        BalanceChangedEvent event = new BalanceChangedEvent(
                user,
                currency,
                oldBalance,
                newBalance,
                EChangeType.DEPOSIT,
                reason,
                initiator
        );

        assertAll(
                () -> assertSame(user, event.getUser(), "User should match supplied instance"),
                () -> assertSame(currency, event.getCurrency(), "Currency should match supplied instance"),
                () -> assertEquals(oldBalance, event.getOldBalance(), "Old balance should match"),
                () -> assertEquals(newBalance, event.getNewBalance(), "New balance should match"),
                () -> assertEquals(newBalance - oldBalance, event.getChangeAmount(), "Change amount should be derived"),
                () -> assertEquals(Math.abs(newBalance - oldBalance), event.getAbsoluteChangeAmount(), "Absolute change should be positive"),
                () -> assertSame(EChangeType.DEPOSIT, event.getChangeType(), "Change type should match"),
                () -> assertEquals(reason, event.getReason(), "Reason should be retained"),
                () -> assertSame(initiator, event.getInitiator(), "Initiator should match supplied player"),
                () -> assertTrue(event.getChangeTime() > 0, "Change time should capture creation timestamp"),
                () -> assertSame(BalanceChangedEvent.getHandlerList(), event.getHandlers(), "Handlers should use shared handler list")
        );
    }

    @Test
    void handlerListShouldRegisterAndDispatchEvents() {
        MockPlugin plugin = MockBukkit.createMockPlugin();
        PlayerMock initiator = this.server.addPlayer("Banker");
        User user = new User(initiator.getUniqueId(), initiator.getName());
        Currency currency = new Currency("credits");
        BalanceChangedEvent event = new BalanceChangedEvent(
                user,
                currency,
                120.0D,
                80.0D,
                EChangeType.WITHDRAW,
                "Purchase",
                initiator
        );
        HandlerList handlerList = BalanceChangedEvent.getHandlerList();
        assertEquals(0, handlerList.getRegisteredListeners().length, "Handler list should start empty");

        AtomicReference<BalanceChangedEvent> dispatchedEvent = new AtomicReference<>();
        Listener listener = new Listener() {
            @EventHandler
            public void onBalanceChanged(final BalanceChangedEvent balanceChangedEvent) {
                dispatchedEvent.set(balanceChangedEvent);
            }
        };

        this.server.getPluginManager().registerEvents(listener, plugin);

        RegisteredListener[] registeredListeners = handlerList.getRegisteredListeners();
        assertEquals(1, registeredListeners.length, "Listener should be registered");
        assertSame(listener, registeredListeners[0].getListener(), "Registered listener should match");
        assertSame(plugin, registeredListeners[0].getPlugin(), "Registered plugin should match");
        assertSame(handlerList, event.getHandlers(), "Instance handlers should reference shared list");

        this.server.getPluginManager().callEvent(event);

        assertSame(event, dispatchedEvent.get(), "Registered listener should receive dispatched event");
        HandlerList.unregisterAll(plugin);
        assertEquals(0, handlerList.getRegisteredListeners().length, "Handler list should be cleared after unregistering");
    }
}
