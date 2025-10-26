package de.jexcellence.economy.event;

import de.jexcellence.economy.database.entity.Currency;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrencyDeleteEventTest {

    @Test
    void shouldToggleCancellationAndRetainMetadata() {
        Currency currency = new Currency(
                "Gold: ",
                " coins",
                "gold",
                "★",
                Material.GOLD_INGOT
        );
        Player initiator = Mockito.mock(Player.class);

        CurrencyDeleteEvent event = new CurrencyDeleteEvent(currency, initiator, 42, 1337.5);

        assertSame(currency, event.getCurrency());
        assertSame(initiator, event.getPlayer());
        assertTrue(event.hasPlayer());
        assertEquals(42, event.getAffectedPlayers());
        assertEquals(1337.5, event.getTotalBalance(), 0.0001);
        assertFalse(event.isCancelled());

        event.setCancelReason("policy");
        event.setCancelled(true);

        assertTrue(event.isCancelled());
        assertEquals("policy", event.getCancelReason());
        assertSame(currency, event.getCurrency());
        assertSame(initiator, event.getPlayer());

        event.setCancelled(false);

        assertFalse(event.isCancelled());
        assertEquals("policy", event.getCancelReason());
    }

    @Test
    void shouldRegisterHandlersThroughStaticList() {
        Currency currency = new Currency(
                "Silver: ",
                " bars",
                "silver",
                "☆",
                Material.IRON_INGOT
        );
        CurrencyDeleteEvent event = new CurrencyDeleteEvent(currency, null, 5, 250.0);

        HandlerList handlerList = CurrencyDeleteEvent.getHandlerList();
        assertNotNull(handlerList);
        assertSame(handlerList, event.getHandlers());
    }

    @Test
    void shouldCancelWithReasonUsingOverloadedSetter() {
        Currency currency = new Currency(
                "Copper: ",
                " nuggets",
                "copper",
                "◇",
                Material.COPPER_INGOT
        );
        CurrencyDeleteEvent event = new CurrencyDeleteEvent(currency, null, 1, 0.5);

        event.setCancelled("regulation");

        assertTrue(event.isCancelled());
        assertEquals("regulation", event.getCancelReason());
    }
}
