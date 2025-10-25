package de.jexcellence.economy.event;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import de.jexcellence.economy.database.entity.Currency;
import org.bukkit.Material;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrencyCreatedEventTest {

    private ServerMock server;
    private PlayerMock creator;
    private Currency currency;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.creator = this.server.addPlayer("EconomyCreator");
        this.currency = new Currency(
            "Coins: ",
            " credits",
            "test_currency",
            "¤",
            Material.DIAMOND
        );
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void gettersReflectPersistedCurrencyDetails() {
        final CurrencyCreatedEvent event = new CurrencyCreatedEvent(this.currency, this.creator);

        assertAll(
            () -> assertSame(this.currency, event.getCurrency(), "The currency reference should be preserved"),
            () -> assertEquals("Coins: ", event.getCurrency().getPrefix(), "Prefix should match persisted value"),
            () -> assertEquals(" credits", event.getCurrency().getSuffix(), "Suffix should match persisted value"),
            () -> assertEquals("test_currency", event.getCurrency().getIdentifier(), "Identifier should match persisted value"),
            () -> assertEquals("¤", event.getCurrency().getSymbol(), "Symbol should match persisted value"),
            () -> assertEquals(Material.DIAMOND, event.getCurrency().getIcon(), "Icon should match persisted value"),
            () -> assertSame(this.creator, event.getPlayer(), "Player reference should be preserved"),
            () -> assertTrue(event.hasPlayer(), "Event should report a present player")
        );
    }

    @Test
    void handlerListRegistrationIsConsistent() {
        final CurrencyCreatedEvent event = new CurrencyCreatedEvent(this.currency, this.creator);

        final HandlerList handlerList = CurrencyCreatedEvent.getHandlerList();
        assertAll(
            () -> assertSame(handlerList, event.getHandlers(), "Instance handlers must use the static handler list"),
            () -> assertTrue(
                Arrays.stream(HandlerList.getHandlerLists()).anyMatch(list -> list == handlerList),
                "Handler list should be registered with Bukkit"
            )
        );
    }

    @Test
    void creationTimeIsCapturedOnceAndRemainsStable() {
        final long beforeCreation = System.currentTimeMillis();
        final CurrencyCreatedEvent event = new CurrencyCreatedEvent(this.currency, this.creator);
        final long afterCreation = System.currentTimeMillis();

        final long creationTime = event.getCreationTime();
        assertAll(
            () -> assertTrue(
                creationTime >= beforeCreation && creationTime <= afterCreation,
                "Creation time should be captured during event instantiation"
            ),
            () -> assertEquals(creationTime, event.getCreationTime(), "Creation time should remain immutable across calls")
        );
    }
}
