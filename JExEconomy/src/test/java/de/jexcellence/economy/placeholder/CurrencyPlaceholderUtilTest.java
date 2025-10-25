package de.jexcellence.economy.placeholder;

import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.database.entity.UserCurrency;
import de.jexcellence.economy.database.repository.UserCurrencyRepository;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyPlaceholderUtilTest {

    @Mock
    private JExEconomyImpl jexEconomyImpl;

    @Mock
    private UserCurrencyRepository userCurrencyRepository;

    @Mock
    private Player player;

    private CurrencyPlaceholderUtil util;

    private Map<Long, Currency> currencyMap;

    private Currency coinsCurrency;

    private Currency creditsCurrency;

    private Locale originalLocale;

    @BeforeEach
    void setUp() {
        this.originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);

        this.coinsCurrency = createCurrency("coins", "$", "₡ ", " coins");
        this.creditsCurrency = createCurrency("credits", "¤", "Cr ", " credits");

        this.currencyMap = new HashMap<>();
        this.currencyMap.put(1L, this.coinsCurrency);
        this.currencyMap.put(2L, this.creditsCurrency);

        when(this.jexEconomyImpl.getCurrencies()).thenReturn(this.currencyMap);
        when(this.jexEconomyImpl.getUserCurrencyRepository()).thenReturn(this.userCurrencyRepository);

        this.util = new CurrencyPlaceholderUtil(this.jexEconomyImpl);
    }

    @AfterEach
    void tearDown() {
        Locale.setDefault(this.originalLocale);
    }

    @Test
    void formatAmountUsesLocaleSpecificFormatters() {
        final String standardAmount = this.util.formatAmount(1234.567, "amount");
        final String roundedAmount = this.util.formatAmount(1234.567, "amount-rounded");
        final String germanDotsAmount = this.util.formatAmount(1234.567, "amount-rounded-dots");
        final String fallbackAmount = this.util.formatAmount(1234.567, "unknown-format");

        assertEquals("1,234.57", standardAmount);
        assertEquals("1235", roundedAmount);
        assertEquals("1.235", germanDotsAmount);
        assertEquals("1234.57", fallbackAmount);
    }

    @Test
    void getCurrencyInfoReturnsFormattingElements() {
        assertEquals("$", this.util.getCurrencyInfo("coins", "symbol"));
        assertEquals("₡ ", this.util.getCurrencyInfo("coins", "prefix"));
        assertEquals(" coins", this.util.getCurrencyInfo("coins", "suffix"));
        assertEquals("₡ coins coins", this.util.getCurrencyInfo("coins", "currencies"));
    }

    @Test
    void getCurrencyInfoReturnsEmptyStringForMissingCurrencyOrUnsupportedType() {
        assertEquals("", this.util.getCurrencyInfo("unknown", "symbol"));
        assertEquals("", this.util.getCurrencyInfo("coins", "unsupported"));
    }

    @Test
    void getPlayerBalanceReturnsFormattedAmountsForAllFormats() {
        final UUID playerId = UUID.randomUUID();
        final UserCurrency balance = new UserCurrency(new User(playerId, "Tester"), this.coinsCurrency, 1234.567);
        when(this.userCurrencyRepository.findByAttributes(anyMap())).thenReturn(balance);

        assertEquals("1,234.57", this.util.getPlayerBalance(playerId, "coins", "amount"));
        assertEquals("1235", this.util.getPlayerBalance(playerId, "coins", "amount-rounded"));
        assertEquals("1.235", this.util.getPlayerBalance(playerId, "coins", "amount-rounded-dots"));
        assertEquals("1234.57", this.util.getPlayerBalance(playerId, "coins", "anything-else"));
    }

    @Test
    void getPlayerBalanceReturnsFallbackWhenDataIsMissing() {
        final UUID playerId = UUID.randomUUID();
        when(this.userCurrencyRepository.findByAttributes(anyMap())).thenReturn(null);

        assertEquals("N/A", this.util.getPlayerBalance(playerId, "coins", "amount"));
        assertEquals("N/A", this.util.getPlayerBalance(playerId, "missing", "amount"));
    }

    @Test
    void getFormattedPlayerBalanceCombinesCurrencyElements() {
        final UUID playerId = UUID.randomUUID();
        final UserCurrency balance = new UserCurrency(new User(playerId, "Tester"), this.coinsCurrency, 9876.5);
        when(this.userCurrencyRepository.findByAttributes(anyMap())).thenReturn(balance);

        assertEquals("₡ 9,876.50$ coins", this.util.getFormattedPlayerBalance(playerId, "coins", "amount"));
        assertEquals("₡ 9,877$ coins", this.util.getFormattedPlayerBalance(playerId, "coins", "amount-rounded"));
    }

    @Test
    void getFormattedPlayerBalanceReturnsFallbackWhenBalanceUnavailable() {
        final UUID playerId = UUID.randomUUID();
        when(this.userCurrencyRepository.findByAttributes(anyMap())).thenReturn(null);

        assertEquals("N/A", this.util.getFormattedPlayerBalance(playerId, "coins", "amount"));
    }

    @Test
    void processPlaceholderHandlesCurrencyAndBalancePatterns() {
        final UUID playerId = UUID.randomUUID();
        when(this.player.getUniqueId()).thenReturn(playerId);
        final UserCurrency balance = new UserCurrency(new User(playerId, "Tester"), this.coinsCurrency, 1234.567);
        when(this.userCurrencyRepository.findByAttributes(anyMap())).thenReturn(balance);

        assertEquals("$", this.util.processPlaceholder(this.player, "currency_coins_symbol"));
        assertEquals("1,234.57", this.util.processPlaceholder(this.player, "player_currency_coins_amount"));
        assertEquals("₡ 1,235$ coins", this.util.processPlaceholder(this.player, "player_formatted_currency_coins_amount-rounded"));
        assertEquals("₡ 1,234.57$ coins", this.util.processPlaceholder(this.player, "player_formatted_currency_coins"));
    }

    @Test
    void processPlaceholderGracefullyHandlesNullOrInvalidInputs() {
        assertNull(this.util.processPlaceholder(null, "currency_coins_symbol"));

        final UUID playerId = UUID.randomUUID();
        when(this.player.getUniqueId()).thenReturn(playerId);
        when(this.userCurrencyRepository.findByAttributes(anyMap())).thenReturn(null);

        assertNull(this.util.processPlaceholder(this.player, "currency_coins"));
        assertNull(this.util.processPlaceholder(this.player, "player_currency_coins"));
        assertEquals("N/A", this.util.processPlaceholder(this.player, "player_currency_coins_amount"));
        assertEquals("N/A", this.util.processPlaceholder(this.player, "player_formatted_currency_coins_amount"));
        assertNull(this.util.processPlaceholder(this.player, "unknown_placeholder"));
    }

    private static Currency createCurrency(
            final String identifier,
            final String symbol,
            final String prefix,
            final String suffix
    ) {
        final Currency currency = new Currency(identifier);
        currency.setSymbol(symbol);
        currency.setPrefix(prefix);
        currency.setSuffix(suffix);
        return currency;
    }
}
