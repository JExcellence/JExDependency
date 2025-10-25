package de.jexcellence.economy.migrate;

import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.adapter.CurrencyAdapter;
import de.jexcellence.economy.adapter.CurrencyResponse;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.database.repository.UserRepository;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JExEconomyVaultProviderTest {

    @Mock
    private JExEconomyImpl jexEconomyImpl;

    @Mock
    private CurrencyAdapter currencyAdapter;

    @Mock
    private JExEconomy plugin;

    @Mock
    private Currency currency;

    @Mock
    private OfflinePlayer offlinePlayer;

    @Mock
    private UserRepository userRepository;

    private JExEconomyVaultProvider vaultProvider;

    @BeforeEach
    void setUp() {
        when(jexEconomyImpl.getCurrencyAdapter()).thenReturn(currencyAdapter);
        when(jexEconomyImpl.getPlugin()).thenReturn(plugin);
        when(jexEconomyImpl.getUserRepository()).thenReturn(userRepository);
        when(plugin.isEnabled()).thenReturn(true);

        Map<Long, Currency> currencies = new HashMap<>();
        currencies.put(1L, currency);
        when(jexEconomyImpl.getCurrencies()).thenReturn(currencies);

        when(currency.getIdentifier()).thenReturn("coins");
        when(currency.getSymbol()).thenReturn("$");

        when(offlinePlayer.getName()).thenReturn("PlayerOne");

        vaultProvider = new JExEconomyVaultProvider(jexEconomyImpl);
    }

    @Test
    void depositPlayerDelegatesToCurrencyAdapterAndMapsSuccessResponse() {
        double depositAmount = 25.0;
        double currentBalance = 50.0;
        double resultingBalance = 75.0;

        when(currencyAdapter.getBalance(offlinePlayer, currency)).thenReturn(CompletableFuture.completedFuture(currentBalance));
        CurrencyResponse successResponse = new CurrencyResponse(
                depositAmount,
                resultingBalance,
                CurrencyResponse.ResponseType.SUCCESS,
                null
        );
        when(currencyAdapter.deposit(offlinePlayer, currency, depositAmount))
                .thenReturn(CompletableFuture.completedFuture(successResponse));

        EconomyResponse response = vaultProvider.depositPlayer(offlinePlayer, depositAmount);

        assertEquals(EconomyResponse.ResponseType.SUCCESS, response.type);
        assertEquals(depositAmount, response.amount);
        assertEquals(resultingBalance, response.balance);
        assertNull(response.errorMessage);

        verify(currencyAdapter).deposit(offlinePlayer, currency, depositAmount);
    }

    @Test
    void depositPlayerPropagatesFailureFromCurrencyAdapter() {
        double depositAmount = 30.0;
        double currentBalance = 15.0;
        String failureMessage = "Player account not found";

        when(currencyAdapter.getBalance(offlinePlayer, currency)).thenReturn(CompletableFuture.completedFuture(currentBalance));
        CurrencyResponse failureResponse = new CurrencyResponse(
                0.0,
                currentBalance,
                CurrencyResponse.ResponseType.FAILURE,
                failureMessage
        );
        when(currencyAdapter.deposit(offlinePlayer, currency, depositAmount))
                .thenReturn(CompletableFuture.completedFuture(failureResponse));

        EconomyResponse response = vaultProvider.depositPlayer(offlinePlayer, depositAmount);

        assertEquals(EconomyResponse.ResponseType.FAILURE, response.type);
        assertEquals(0.0, response.amount);
        assertEquals(currentBalance, response.balance);
        assertEquals(failureMessage, response.errorMessage);

        verify(currencyAdapter).deposit(offlinePlayer, currency, depositAmount);
    }

    @Test
    void depositPlayerByNameUsesBukkitLookup() {
        double depositAmount = 10.0;
        double currentBalance = 20.0;
        double resultingBalance = 30.0;

        when(currencyAdapter.getBalance(offlinePlayer, currency)).thenReturn(CompletableFuture.completedFuture(currentBalance));
        CurrencyResponse successResponse = new CurrencyResponse(
                depositAmount,
                resultingBalance,
                CurrencyResponse.ResponseType.SUCCESS,
                null
        );
        when(currencyAdapter.deposit(offlinePlayer, currency, depositAmount))
                .thenReturn(CompletableFuture.completedFuture(successResponse));

        try (MockedStatic<Bukkit> mockedBukkit = Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer("PlayerOne")).thenReturn(null);
            mockedBukkit.when(() -> Bukkit.getOfflinePlayer("PlayerOne")).thenReturn(offlinePlayer);

            EconomyResponse response = vaultProvider.depositPlayer("PlayerOne", depositAmount);

            assertEquals(EconomyResponse.ResponseType.SUCCESS, response.type);
            assertEquals(resultingBalance, response.balance);
        }

        verify(currencyAdapter).deposit(offlinePlayer, currency, depositAmount);
    }

    @Test
    void withdrawPlayerDelegatesToCurrencyAdapterAndMapsSuccessResponse() {
        double withdrawAmount = 12.5;
        double currentBalance = 40.0;
        double resultingBalance = 27.5;

        when(currencyAdapter.getBalance(offlinePlayer, currency)).thenReturn(CompletableFuture.completedFuture(currentBalance));
        CurrencyResponse successResponse = new CurrencyResponse(
                -withdrawAmount,
                resultingBalance,
                CurrencyResponse.ResponseType.SUCCESS,
                null
        );
        when(currencyAdapter.withdraw(offlinePlayer, currency, withdrawAmount))
                .thenReturn(CompletableFuture.completedFuture(successResponse));

        EconomyResponse response = vaultProvider.withdrawPlayer(offlinePlayer, withdrawAmount);

        assertEquals(EconomyResponse.ResponseType.SUCCESS, response.type);
        assertEquals(withdrawAmount, response.amount);
        assertEquals(resultingBalance, response.balance);
        assertNull(response.errorMessage);

        verify(currencyAdapter).withdraw(offlinePlayer, currency, withdrawAmount);
    }

    @Test
    void withdrawPlayerFailsWhenFundsAreInsufficient() {
        double withdrawAmount = 80.0;
        double currentBalance = 25.0;

        when(currencyAdapter.getBalance(offlinePlayer, currency)).thenReturn(CompletableFuture.completedFuture(currentBalance));

        EconomyResponse response = vaultProvider.withdrawPlayer(offlinePlayer, withdrawAmount);

        assertEquals(EconomyResponse.ResponseType.FAILURE, response.type);
        assertEquals(withdrawAmount, response.amount);
        assertEquals(currentBalance, response.balance);
        assertEquals("Insufficient funds", response.errorMessage);

        verify(currencyAdapter, never()).withdraw(offlinePlayer, currency, withdrawAmount);
    }

    @Test
    void withdrawPlayerPropagatesFailureFromCurrencyAdapter() {
        double withdrawAmount = 15.0;
        double currentBalance = 50.0;
        String failureMessage = "Withdrawal blocked";

        when(currencyAdapter.getBalance(offlinePlayer, currency)).thenReturn(CompletableFuture.completedFuture(currentBalance));
        CurrencyResponse failureResponse = new CurrencyResponse(
                0.0,
                currentBalance,
                CurrencyResponse.ResponseType.FAILURE,
                failureMessage
        );
        when(currencyAdapter.withdraw(offlinePlayer, currency, withdrawAmount))
                .thenReturn(CompletableFuture.completedFuture(failureResponse));

        EconomyResponse response = vaultProvider.withdrawPlayer(offlinePlayer, withdrawAmount);

        assertEquals(EconomyResponse.ResponseType.FAILURE, response.type);
        assertEquals(0.0, response.amount);
        assertEquals(currentBalance, response.balance);
        assertEquals(failureMessage, response.errorMessage);

        verify(currencyAdapter).withdraw(offlinePlayer, currency, withdrawAmount);
    }

    @Test
    void getBalanceDelegatesToCurrencyAdapter() {
        double expectedBalance = 123.45;
        when(currencyAdapter.getBalance(offlinePlayer, currency)).thenReturn(CompletableFuture.completedFuture(expectedBalance));

        double balance = vaultProvider.getBalance(offlinePlayer);

        assertEquals(expectedBalance, balance);
        verify(currencyAdapter).getBalance(offlinePlayer, currency);
    }

    @Test
    void getBalanceByNameUsesBukkitLookup() {
        double expectedBalance = 64.0;
        when(currencyAdapter.getBalance(offlinePlayer, currency)).thenReturn(CompletableFuture.completedFuture(expectedBalance));

        try (MockedStatic<Bukkit> mockedBukkit = Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer("PlayerOne")).thenReturn(null);
            mockedBukkit.when(() -> Bukkit.getOfflinePlayer("PlayerOne")).thenReturn(offlinePlayer);

            double balance = vaultProvider.getBalance("PlayerOne");
            assertEquals(expectedBalance, balance);
        }

        verify(currencyAdapter).getBalance(offlinePlayer, currency);
    }

    @Test
    void createPlayerAccountCreatesMissingDependencies() {
        UUID playerUuid = UUID.randomUUID();
        when(offlinePlayer.getUniqueId()).thenReturn(playerUuid);
        when(currencyAdapter.getUserCurrency(eq(offlinePlayer), eq("coins")))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(currencyAdapter.createPlayer(offlinePlayer))
                .thenReturn(CompletableFuture.completedFuture(true));
        User user = mock(User.class);
        when(userRepository.findByAttributes(anyMap())).thenReturn(user);
        when(currencyAdapter.createPlayerCurrency(user, currency))
                .thenReturn(CompletableFuture.completedFuture(true));

        boolean created = vaultProvider.createPlayerAccount(offlinePlayer);

        assertTrue(created);
        verify(currencyAdapter).createPlayer(offlinePlayer);
        verify(currencyAdapter).createPlayerCurrency(user, currency);
        verify(userRepository).findByAttributes(argThat(map -> playerUuid.equals(map.get("uniqueId"))));
    }

    @Test
    void unsupportedBankOperationsReturnVaultNotImplementedResponses() {
        List<EconomyResponse> responses = List.of(
                vaultProvider.createBank("bank", "player"),
                vaultProvider.createBank("bank", offlinePlayer),
                vaultProvider.deleteBank("bank"),
                vaultProvider.bankBalance("bank"),
                vaultProvider.bankHas("bank", 10.0),
                vaultProvider.bankWithdraw("bank", 5.0),
                vaultProvider.bankDeposit("bank", 5.0),
                vaultProvider.isBankOwner("bank", "player"),
                vaultProvider.isBankOwner("bank", offlinePlayer),
                vaultProvider.isBankMember("bank", "player"),
                vaultProvider.isBankMember("bank", offlinePlayer)
        );

        for (EconomyResponse response : responses) {
            assertEquals(EconomyResponse.ResponseType.NOT_IMPLEMENTED, response.type);
            assertEquals(0.0, response.amount);
            assertEquals(0.0, response.balance);
            assertEquals("JExEconomyImpl does not support bank accounts", response.errorMessage);
        }

        assertTrue(vaultProvider.getBanks().isEmpty());
    }
}
