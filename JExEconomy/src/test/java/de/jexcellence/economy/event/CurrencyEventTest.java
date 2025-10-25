package de.jexcellence.economy.event;

import de.jexcellence.economy.database.entity.Currency;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrencyEventTest {

    @Test
    void gettersExposeConstructorValues() {
        Currency currency = new Currency("$", " coins", "gold", "G", Material.GOLD_INGOT);
        Player player = Mockito.mock(Player.class);

        TestCurrencyEvent event = new TestCurrencyEvent(currency, player);

        assertSame(currency, event.getCurrency(), "Currency getter should return the provided currency instance");
        assertSame(player, event.getPlayer(), "Player getter should return the provided player instance");
        assertTrue(event.hasPlayer(), "hasPlayer should report true when a player is provided");
    }

    @Test
    void handlerListIsSharedAcrossInstances() {
        Currency currency = new Currency("", "", "silver", "S", Material.DIAMOND);
        TestCurrencyEvent event = new TestCurrencyEvent(currency, null);

        HandlerList handlers = event.getHandlers();

        assertNotNull(handlers, "Handler list should never be null");
        assertSame(CurrencyEvent.getHandlerList(), handlers, "Instance handlers should match the static handler list");
    }

    @Test
    void playerAbsenceIsReflected() {
        Currency currency = new Currency("", "", "bronze", "B", Material.IRON_INGOT);
        TestCurrencyEvent event = new TestCurrencyEvent(currency, null);

        assertNull(event.getPlayer(), "Player getter should return null when no player is involved");
        assertFalse(event.hasPlayer(), "hasPlayer should report false when no player is provided");
    }

    @Test
    void eventsAreSynchronousByDefault() {
        Currency currency = new Currency("", "", "platinum", "P", Material.EMERALD);
        Player player = Mockito.mock(Player.class);
        TestCurrencyEvent event = new TestCurrencyEvent(currency, player);

        assertFalse(event.isAsynchronous(), "Currency events should default to synchronous execution");
    }

    private static final class TestCurrencyEvent extends CurrencyEvent {

        private TestCurrencyEvent(Currency currency, Player player) {
            super(currency, player);
        }
    }
}
