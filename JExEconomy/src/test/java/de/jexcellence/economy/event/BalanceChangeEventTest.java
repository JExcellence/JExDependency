package de.jexcellence.economy.event;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.type.EChangeType;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalanceChangeEventTest {

    private ServerMock server;
    private PlayerMock initiator;
    private User user;
    private Currency currency;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.initiator = this.server.addPlayer("Initiator");
        this.user = new User(this.initiator);
        this.currency = new Currency("test-coins");
        this.currency.setSymbol("¤");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void cancellationTogglesShouldUpdateStateAndReason() {
        BalanceChangeEvent event = new BalanceChangeEvent(
                this.user,
                this.currency,
                100.0,
                75.0,
                EChangeType.WITHDRAW,
                "purchase",
                this.initiator
        );

        assertFalse(event.isCancelled());
        assertNull(event.getCancelReason());

        event.setCancelled(true);
        assertTrue(event.isCancelled());
        assertNull(event.getCancelReason());

        String manualReason = "Insufficient funds";
        event.setCancelReason(manualReason);
        assertEquals(manualReason, event.getCancelReason());

        event.setCancelled(false);
        assertFalse(event.isCancelled());
        assertEquals(manualReason, event.getCancelReason());

        String combinedReason = "Denied by admin";
        event.setCancelled(combinedReason);
        assertTrue(event.isCancelled());
        assertEquals(combinedReason, event.getCancelReason());
    }

    @Test
    void metadataShouldPropagateToBalanceChangedEventAndHandlers() {
        double oldBalance = 150.0;
        double newBalance = 180.0;
        String reason = "Quest reward";

        BalanceChangeEvent changeEvent = new BalanceChangeEvent(
                this.user,
                this.currency,
                oldBalance,
                newBalance,
                EChangeType.DEPOSIT,
                reason,
                this.initiator
        );

        assertEquals(oldBalance, changeEvent.getOldBalance());
        assertEquals(newBalance, changeEvent.getNewBalance());
        assertEquals(newBalance - oldBalance, changeEvent.getChangeAmount());
        assertEquals(this.currency, changeEvent.getCurrency());
        assertEquals(this.initiator, changeEvent.getInitiator());
        assertEquals(reason, changeEvent.getReason());

        BalanceChangedEvent changedEvent = new BalanceChangedEvent(
                changeEvent.getUser(),
                changeEvent.getCurrency(),
                changeEvent.getOldBalance(),
                changeEvent.getNewBalance(),
                changeEvent.getChangeType(),
                changeEvent.getReason(),
                changeEvent.getInitiator()
        );

        assertEquals(changeEvent.getChangeAmount(), changedEvent.getChangeAmount());
        assertEquals(changeEvent.getAbsoluteChangeAmount(), changedEvent.getAbsoluteChangeAmount());
        assertEquals(changeEvent.getCurrency(), changedEvent.getCurrency());
        assertEquals(changeEvent.getInitiator(), changedEvent.getInitiator());
        assertEquals(changeEvent.getReason(), changedEvent.getReason());
        assertEquals(changeEvent.getChangeType(), changedEvent.getChangeType());
        assertTrue(changedEvent.getChangeTime() > 0);

        HandlerList changeHandlers = changeEvent.getHandlers();
        HandlerList changeStaticHandlers = BalanceChangeEvent.getHandlerList();
        HandlerList changedHandlers = changedEvent.getHandlers();
        HandlerList changedStaticHandlers = BalanceChangedEvent.getHandlerList();

        assertSame(changeStaticHandlers, changeHandlers);
        assertSame(changedStaticHandlers, changedHandlers);
    }
}
