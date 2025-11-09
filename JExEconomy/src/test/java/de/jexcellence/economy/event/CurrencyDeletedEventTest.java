package de.jexcellence.economy.event;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import de.jexcellence.economy.database.entity.Currency;
import org.bukkit.Material;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyDeletedEventTest {

    private static ServerMock server;

    @BeforeAll
    static void setUpServer() {

        server = MockBukkit.mock();
    }

    @AfterAll
    static void tearDownServer() {

        MockBukkit.unmock();
    }

    @Test
    void constructorShouldStoreCurrencyDetailsAndProvideImmutableState() {

        final Currency currency = new Currency(
            "prefix",
            "suffix",
            "test-currency",
            "$",
            Material.DIAMOND
        );
        final PlayerMock player = server.addPlayer("Tester");
        final long beforeCreation = System.currentTimeMillis();

        final CurrencyDeletedEvent event = new CurrencyDeletedEvent(
            currency,
            player,
            7,
            42.5D
        );
        final long afterCreation = System.currentTimeMillis();

        assertSame(currency, event.getCurrency());
        assertSame(player, event.getPlayer());
        assertTrue(event.hasPlayer());
        assertEquals(7, event.getAffectedPlayers());
        assertEquals(42.5D, event.getTotalBalanceLost());

        final long deletionTime = event.getDeletionTime();
        assertTrue(deletionTime >= beforeCreation);
        assertTrue(deletionTime <= afterCreation);
        assertEquals(deletionTime, event.getDeletionTime());
    }

    @Test
    void eventShouldBeNonCancellableAndExposeSharedHandlerList() {

        final Currency currency = new Currency("test-currency");
        final CurrencyDeletedEvent event = new CurrencyDeletedEvent(
            currency,
            null,
            0,
            0.0D
        );

        assertFalse(event instanceof Cancellable);

        final HandlerList handlers = event.getHandlers();
        assertSame(CurrencyDeletedEvent.getHandlerList(), handlers);
        assertSame(handlers, event.getHandlers());
    }
}
