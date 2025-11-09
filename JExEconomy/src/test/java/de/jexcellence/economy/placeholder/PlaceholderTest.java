package de.jexcellence.economy.placeholder;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.JExEconomyImpl;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.PluginMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PlaceholderTest {

    private ServerMock server;
    private Placeholder placeholder;
    private CurrencyPlaceholderUtil currencyPlaceholderUtil;

    @BeforeEach
    void setUp() throws Exception {
        this.server = MockBukkit.mock();

        JExEconomy plugin = Mockito.mock(JExEconomy.class);
        when(plugin.getServer()).thenReturn(this.server);

        PluginMeta pluginMeta = Mockito.mock(PluginMeta.class);
        when(pluginMeta.getAuthors()).thenReturn(List.of("JExcellence", "RaindropCentral"));
        when(pluginMeta.getVersion()).thenReturn("2.5.0");

        when(plugin.getPluginMeta()).thenReturn(pluginMeta);
        when(plugin.getName()).thenReturn("JExEconomy");

        JExEconomyImpl delegate = new JExEconomyImpl(plugin);
        this.placeholder = new Placeholder(delegate);

        this.currencyPlaceholderUtil = Mockito.mock(CurrencyPlaceholderUtil.class);
        Field field = Placeholder.class.getDeclaredField("currencyPlaceholderUtil");
        field.setAccessible(true);
        field.set(this.placeholder, this.currencyPlaceholderUtil);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void metadataDerivedFromPluginMeta() {
        assertEquals("jexeconomy", this.placeholder.getIdentifier(), "Identifier should use lower-case plugin name");
        assertEquals("JExcellence, RaindropCentral", this.placeholder.getAuthor(),
                "Author listing should be built from plugin meta");
        assertEquals("2.5.0", this.placeholder.getVersion(), "Version should originate from plugin meta");

        assertTrue(this.placeholder.getPlaceholders().contains("%jexeconomy_currency_<currency>_name%"),
                "Placeholder definitions should be expanded into fully qualified keys");
    }

    @Test
    void onRequestDelegatesToProcessPlaceholderForOnlinePlayers() {
        PlayerMock player = this.server.addPlayer("CurrencyFan");
        when(this.currencyPlaceholderUtil.processPlaceholder(player, "currency_gold_name")).thenReturn("Gold");

        String result = this.placeholder.onRequest(player, "currency_gold_name");

        assertEquals("Gold", result, "Resolved placeholder should come from the utility");
        verify(this.currencyPlaceholderUtil).processPlaceholder(player, "currency_gold_name");
    }

    @Test
    void onRequestReturnsNullForOfflinePlayers() {
        OfflinePlayer offlinePlayer = this.server.getOfflinePlayer("OfflineFan");

        assertNull(this.placeholder.onRequest(offlinePlayer, "currency_gold_name"),
                "Offline requests without an online player should be ignored");
        verifyNoInteractions(this.currencyPlaceholderUtil);
    }

    @Test
    void onPlaceholderRequestReturnsEmptyWhenPlayerMissing() {
        String result = this.placeholder.onPlaceholderRequest(null, "currency_gold_name");

        assertEquals("", result, "Missing player contexts should produce an empty placeholder");
        verifyNoInteractions(this.currencyPlaceholderUtil);
    }

    @Test
    void onPlaceholderRequestReturnsEmptyForUnsupportedParameters() {
        PlayerMock player = this.server.addPlayer("UnsupportedFan");

        String result = this.placeholder.onPlaceholderRequest(player, "invalid_placeholder");

        assertEquals("", result, "Unsupported placeholder prefixes should be ignored");
        verify(this.currencyPlaceholderUtil, never()).processPlaceholder(any(), any());
    }

    @Test
    void onPlaceholderRequestUsesFallbackWhenUtilityReturnsNull() {
        PlayerMock player = this.server.addPlayer("FallbackFan");
        when(this.currencyPlaceholderUtil.processPlaceholder(player, "player_currency_gold_amount")).thenReturn(null);

        String result = this.placeholder.onPlaceholderRequest(player, "player_currency_gold_amount");

        assertEquals("", result, "Null utility responses should fall back to an empty string");
        verify(this.currencyPlaceholderUtil).processPlaceholder(player, "player_currency_gold_amount");
    }
}
