package de.jexcellence.economy.adapter;

import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.UserCurrency;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ICurrencyAdapterContractTest {

    @Test
    void depositUpdatesBalanceAndReportsSuccess() {
        FakeCurrencyAdapter fakeAdapter = new FakeCurrencyAdapter();
        ICurrencyAdapter adapter = fakeAdapter.adapter();

        Currency primaryCurrency = createCurrency("primary");
        fakeAdapter.registerCurrency(primaryCurrency);

        OfflinePlayer player = createOfflinePlayer();

        CurrencyResponse response = adapter.deposit(player, primaryCurrency, 150.0).join();

        assertEquals(CurrencyResponse.ResponseType.SUCCESS, response.operationStatus(),
                "Deposits should report SUCCESS when currency exists and amount is positive");
        assertTrue(response.isTransactionSuccessful(), "Successful operations must advertise positive result");
        assertEquals(150.0, response.resultingBalance(), 0.0001,
                "Resulting balance should reflect the deposited amount");

        Double balance = adapter.getBalance(player, primaryCurrency).join();
        assertEquals(150.0, balance, 0.0001, "Balance lookup should include previous deposits");
    }

    @Test
    void withdrawFailsWhenBalanceIsInsufficient() {
        FakeCurrencyAdapter fakeAdapter = new FakeCurrencyAdapter();
        ICurrencyAdapter adapter = fakeAdapter.adapter();

        Currency primaryCurrency = createCurrency("primary");
        fakeAdapter.registerCurrency(primaryCurrency);

        OfflinePlayer player = createOfflinePlayer();
        adapter.deposit(player, primaryCurrency, 50.0).join();

        CurrencyResponse response = adapter.withdraw(player, primaryCurrency, 100.0).join();

        assertEquals(CurrencyResponse.ResponseType.FAILURE, response.operationStatus(),
                "Withdrawals without enough funds should fail");
        assertTrue(response.isTransactionFailed(), "Failure responses must expose a failure state");
        assertTrue(response.failureMessage().toLowerCase().contains("insufficient"),
                "Failure message should explain insufficient funds");

        Double balance = adapter.getBalance(player, primaryCurrency).join();
        assertEquals(50.0, balance, 0.0001, "Failed withdrawals must not change the stored balance");
    }

    @Test
    void depositFailsWhenCurrencyIsMissing() {
        FakeCurrencyAdapter fakeAdapter = new FakeCurrencyAdapter();
        ICurrencyAdapter adapter = fakeAdapter.adapter();

        Currency registeredCurrency = createCurrency("registered");
        fakeAdapter.registerCurrency(registeredCurrency);

        OfflinePlayer player = createOfflinePlayer();
        Currency unknownCurrency = createCurrency("unknown");

        CurrencyResponse response = adapter.deposit(player, unknownCurrency, 25.0).join();

        assertEquals(CurrencyResponse.ResponseType.FAILURE, response.operationStatus(),
                "Deposits for currencies not tracked by the adapter should fail");
        assertTrue(response.isTransactionFailed(), "Missing currency must result in failed transaction");
        assertTrue(response.failureMessage().toLowerCase().contains("not registered"),
                "Failure message should indicate missing currency registration");
    }

    private static OfflinePlayer createOfflinePlayer() {
        OfflinePlayer player = Mockito.mock(OfflinePlayer.class);
        UUID playerId = UUID.randomUUID();
        Mockito.when(player.getUniqueId()).thenReturn(playerId);
        Mockito.when(player.getName()).thenReturn("TestPlayer" + playerId);
        return player;
    }

    private static Currency createCurrency(final String identifier) {
        Currency currency = Mockito.mock(Currency.class);
        Mockito.when(currency.getIdentifier()).thenReturn(identifier);
        return currency;
    }

    private static final class FakeCurrencyAdapter implements InvocationHandler {

        private final Map<String, Currency> currencies = new HashMap<>();
        private final Map<UUID, Map<String, Double>> balances = new HashMap<>();
        private final ICurrencyAdapter adapterProxy;

        private FakeCurrencyAdapter() {
            this.adapterProxy = (ICurrencyAdapter) Proxy.newProxyInstance(
                    ICurrencyAdapter.class.getClassLoader(),
                    new Class<?>[]{ICurrencyAdapter.class},
                    this
            );
        }

        private ICurrencyAdapter adapter() {
            return this.adapterProxy;
        }

        private void registerCurrency(final Currency currency) {
            Objects.requireNonNull(currency, "Currency must not be null");
            String identifier = currency.getIdentifier();
            if (identifier == null || identifier.trim().isEmpty()) {
                throw new IllegalArgumentException("Currency identifier must not be empty");
            }
            this.currencies.put(identifier, currency);
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }

            final String methodName = method.getName();
            if ("getBalance".equals(methodName)) {
                if (args != null && args.length == 2 && args[0] instanceof OfflinePlayer player &&
                        args[1] instanceof Currency currency) {
                    return this.handleGetBalance(player, currency);
                } else if (args != null && args.length == 1 && args[0] instanceof UserCurrency userCurrency) {
                    return CompletableFuture.completedFuture(userCurrency.getBalance());
                }
            } else if ("deposit".equals(methodName)) {
                if (args != null && args.length == 3 && args[0] instanceof OfflinePlayer player &&
                        args[1] instanceof Currency currency) {
                    final double amount = (double) args[2];
                    return this.handleDeposit(player, currency, amount);
                } else if (args != null && args.length == 2 && args[0] instanceof UserCurrency userCurrency) {
                    final double amount = (double) args[1];
                    return this.handleUserCurrencyDeposit(userCurrency, amount);
                }
            } else if ("withdraw".equals(methodName)) {
                if (args != null && args.length == 3 && args[0] instanceof OfflinePlayer player &&
                        args[1] instanceof Currency currency) {
                    final double amount = (double) args[2];
                    return this.handleWithdraw(player, currency, amount);
                } else if (args != null && args.length == 2 && args[0] instanceof UserCurrency userCurrency) {
                    final double amount = (double) args[1];
                    return this.handleUserCurrencyWithdraw(userCurrency, amount);
                }
            } else if ("hasGivenCurrency".equals(methodName)) {
                final String identifier = args != null ? (String) args[0] : null;
                return CompletableFuture.completedFuture(
                        identifier != null && this.currencies.containsKey(identifier)
                );
            } else if ("createCurrency".equals(methodName)) {
                if (args != null && args.length == 1 && args[0] instanceof Currency currency) {
                    this.registerCurrency(currency);
                    return CompletableFuture.completedFuture(Boolean.TRUE);
                }
            }

            if (CompletableFuture.class.isAssignableFrom(method.getReturnType())) {
                return CompletableFuture.failedFuture(new UnsupportedOperationException(
                        "Method " + method.getName() + " is not supported by FakeCurrencyAdapter"
                ));
            }

            throw new UnsupportedOperationException(
                    "Method " + method.getName() + " is not supported by FakeCurrencyAdapter"
            );
        }

        private CompletableFuture<Double> handleGetBalance(
                final OfflinePlayer player,
                final Currency currency
        ) {
            requirePlayer(player);
            final String identifier = resolveIdentifier(currency);

            final Map<String, Double> playerBalances = this.balances.get(player.getUniqueId());
            final double balance = playerBalances != null ? playerBalances.getOrDefault(identifier, 0.0) : 0.0;
            return CompletableFuture.completedFuture(balance);
        }

        private CompletableFuture<CurrencyResponse> handleDeposit(
                final OfflinePlayer player,
                final Currency currency,
                final double amount
        ) {
            requirePlayer(player);
            final String identifier = resolveIdentifier(currency);

            if (!this.currencies.containsKey(identifier)) {
                final double currentBalance = this.balances
                        .getOrDefault(player.getUniqueId(), Map.of())
                        .getOrDefault(identifier, 0.0);
                return CompletableFuture.completedFuture(
                        CurrencyResponse.createFailureResponse(
                                amount,
                                currentBalance,
                                String.format("Currency '%s' is not registered", identifier)
                        )
                );
            }

            if (amount <= 0.0) {
                final double currentBalance = this.balances
                        .getOrDefault(player.getUniqueId(), Map.of())
                        .getOrDefault(identifier, 0.0);
                return CompletableFuture.completedFuture(
                        CurrencyResponse.createFailureResponse(
                                amount,
                                currentBalance,
                                "Deposit amount must be positive"
                        )
                );
            }

            final Map<String, Double> playerBalances = this.balances.computeIfAbsent(
                    player.getUniqueId(),
                    ignored -> new HashMap<>()
            );
            final double updatedBalance = playerBalances.getOrDefault(identifier, 0.0) + amount;
            playerBalances.put(identifier, updatedBalance);

            return CompletableFuture.completedFuture(
                    CurrencyResponse.createSuccessfulResponse(amount, updatedBalance)
            );
        }

        private CompletableFuture<CurrencyResponse> handleWithdraw(
                final OfflinePlayer player,
                final Currency currency,
                final double amount
        ) {
            requirePlayer(player);
            final String identifier = resolveIdentifier(currency);

            final double currentBalance = this.balances
                    .getOrDefault(player.getUniqueId(), Map.of())
                    .getOrDefault(identifier, 0.0);

            if (!this.currencies.containsKey(identifier)) {
                return CompletableFuture.completedFuture(
                        CurrencyResponse.createFailureResponse(
                                amount,
                                currentBalance,
                                String.format("Currency '%s' is not registered", identifier)
                        )
                );
            }

            if (amount <= 0.0) {
                return CompletableFuture.completedFuture(
                        CurrencyResponse.createFailureResponse(
                                amount,
                                currentBalance,
                                "Withdrawal amount must be positive"
                        )
                );
            }

            if (currentBalance < amount) {
                return CompletableFuture.completedFuture(
                        CurrencyResponse.createFailureResponse(
                                amount,
                                currentBalance,
                                "Insufficient funds for withdrawal"
                        )
                );
            }

            final Map<String, Double> playerBalances = this.balances.get(player.getUniqueId());
            final double updatedBalance = currentBalance - amount;
            Objects.requireNonNull(playerBalances, "Balance map should exist when withdrawing");
            playerBalances.put(identifier, updatedBalance);

            return CompletableFuture.completedFuture(
                    CurrencyResponse.createSuccessfulResponse(-amount, updatedBalance)
            );
        }

        private CompletableFuture<CurrencyResponse> handleUserCurrencyDeposit(
                final UserCurrency userCurrency,
                final double amount
        ) {
            if (amount <= 0.0) {
                return CompletableFuture.completedFuture(
                        CurrencyResponse.createFailureResponse(
                                amount,
                                userCurrency.getBalance(),
                                "Deposit amount must be positive"
                        )
                );
            }

            final double updatedBalance = userCurrency.getBalance() + amount;
            userCurrency.setBalance(updatedBalance);
            return CompletableFuture.completedFuture(
                    CurrencyResponse.createSuccessfulResponse(amount, updatedBalance)
            );
        }

        private CompletableFuture<CurrencyResponse> handleUserCurrencyWithdraw(
                final UserCurrency userCurrency,
                final double amount
        ) {
            final double currentBalance = userCurrency.getBalance();
            if (amount <= 0.0) {
                return CompletableFuture.completedFuture(
                        CurrencyResponse.createFailureResponse(
                                amount,
                                currentBalance,
                                "Withdrawal amount must be positive"
                        )
                );
            }

            if (currentBalance < amount) {
                return CompletableFuture.completedFuture(
                        CurrencyResponse.createFailureResponse(
                                amount,
                                currentBalance,
                                "Insufficient funds for withdrawal"
                        )
                );
            }

            final double updatedBalance = currentBalance - amount;
            userCurrency.setBalance(updatedBalance);
            return CompletableFuture.completedFuture(
                    CurrencyResponse.createSuccessfulResponse(-amount, updatedBalance)
            );
        }

        private static void requirePlayer(final OfflinePlayer player) {
            if (player == null) {
                throw new IllegalArgumentException("Player must not be null");
            }

            if (player.getUniqueId() == null) {
                throw new IllegalArgumentException("Player must provide a unique identifier");
            }
        }

        private static String resolveIdentifier(final Currency currency) {
            if (currency == null) {
                throw new IllegalArgumentException("Currency must not be null");
            }

            final String identifier = currency.getIdentifier();
            if (identifier == null || identifier.trim().isEmpty()) {
                throw new IllegalArgumentException("Currency identifier must not be empty");
            }

            return identifier;
        }
    }
}
