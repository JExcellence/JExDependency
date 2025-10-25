package de.jexcellence.economy.migrate;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.adapter.CurrencyAdapter;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.database.entity.UserCurrency;
import de.jexcellence.economy.database.repository.CurrencyRepository;
import de.jexcellence.economy.database.repository.UserCurrencyRepository;
import de.jexcellence.economy.database.repository.UserRepository;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultMigrationManagerTest {

    private final List<ExecutorService> executors = new ArrayList<>();
    private final List<Logger> loggers = new ArrayList<>();
    private final List<TestLogHandler> handlers = new ArrayList<>();

    @AfterEach
    void tearDown() throws InterruptedException {
        for (ExecutorService executor : executors) {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        executors.clear();

        for (int i = 0; i < loggers.size(); i++) {
            Logger logger = loggers.get(i);
            TestLogHandler handler = handlers.get(i);
            logger.removeHandler(handler);
        }
        loggers.clear();
        handlers.clear();
    }

    @Test
    void startMigrationMigratesBalancesAndLogsProgress() throws Exception {
        ExecutorService executor = registerExecutor();
        Logger logger = registerLogger();
        TestLogHandler handler = handlers.get(handlers.size() - 1);

        try (MockedStatic<CentralLogger> centralLogger = Mockito.mockStatic(CentralLogger.class);
             MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            centralLogger.when(() -> CentralLogger.getLogger(Mockito.anyString())).thenReturn(logger);

            ServicesManager servicesManager = Mockito.mock(ServicesManager.class);
            RegisteredServiceProvider<Economy> registration = Mockito.mock(RegisteredServiceProvider.class);
            Economy vaultEconomy = Mockito.mock(Economy.class, Mockito.withSettings().lenient());
            JavaPlugin vaultPlugin = Mockito.mock(JavaPlugin.class);

            bukkit.when(Bukkit::getServicesManager).thenReturn(servicesManager);
            bukkit.when(Bukkit::getOfflinePlayers).thenReturn(createOfflinePlayers());

            Mockito.when(servicesManager.getRegistration(Economy.class)).thenReturn(registration);
            Mockito.when(registration.getProvider()).thenReturn(vaultEconomy);
            Mockito.when(registration.getPlugin()).thenReturn(vaultPlugin);
            Mockito.when(vaultEconomy.getName()).thenReturn("Essentials");
            Mockito.when(vaultEconomy.currencyNamePlural()).thenReturn("Coins");
            Mockito.when(vaultEconomy.currencyNameSingular()).thenReturn("Coin");
            Mockito.when(vaultEconomy.getBalance("Alice")).thenReturn(50.0);
            Mockito.when(vaultEconomy.getBalance("Bob")).thenReturn(25.0);
            Mockito.when(vaultEconomy.hasAccount(Mockito.any(OfflinePlayer.class))).thenAnswer(invocation -> {
                OfflinePlayer player = invocation.getArgument(0);
                return player.getName() != null && (player.getName().equals("Alice") || player.getName().equals("Bob"));
            });

            JExEconomyImpl economy = Mockito.mock(JExEconomyImpl.class, Mockito.withSettings().lenient());
            CurrencyAdapter currencyAdapter = Mockito.mock(CurrencyAdapter.class, Mockito.withSettings().lenient());
            CurrencyRepository currencyRepository = Mockito.mock(CurrencyRepository.class);
            UserRepository userRepository = Mockito.mock(UserRepository.class, Mockito.withSettings().lenient());
            UserCurrencyRepository userCurrencyRepository = Mockito.mock(UserCurrencyRepository.class, Mockito.withSettings().lenient());

            Mockito.when(economy.getExecutor()).thenReturn(executor);
            Mockito.when(economy.getCurrencyAdapter()).thenReturn(currencyAdapter);
            Mockito.when(economy.getCurrencyRepository()).thenReturn(currencyRepository);
            Mockito.when(economy.getUserRepository()).thenReturn(userRepository);
            Mockito.when(economy.getUserCurrencyRepository()).thenReturn(userCurrencyRepository);
            Mockito.when(economy.getCurrencies()).thenReturn(new ConcurrentHashMap<>());

            Map<UUID, User> users = new ConcurrentHashMap<>();
            Map<UUID, UserCurrency> userCurrencies = new ConcurrentHashMap<>();
            String currencyIdentifier = "vault-coins";

            Mockito.when(currencyRepository.createAsync(Mockito.any(Currency.class))).thenAnswer(invocation -> {
                Currency created = invocation.getArgument(0);
                return CompletableFuture.completedFuture(created);
            });

            Mockito.when(currencyAdapter.createPlayer(Mockito.any(OfflinePlayer.class))).thenAnswer(invocation -> {
                OfflinePlayer player = invocation.getArgument(0);
                users.computeIfAbsent(player.getUniqueId(), uuid -> new User(uuid, player.getName()));
                return CompletableFuture.completedFuture(true);
            });

            Mockito.when(userRepository.findByAttributes(Mockito.anyMap())).thenAnswer(invocation -> {
                Map<String, Object> attributes = invocation.getArgument(0);
                UUID uniqueId = (UUID) attributes.get("uniqueId");
                return users.get(uniqueId);
            });

            Mockito.when(currencyAdapter.createPlayerCurrency(Mockito.any(User.class), Mockito.any(Currency.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                Currency currency = invocation.getArgument(1);
                userCurrencies.put(user.getUniqueId(), new UserCurrency(user, currency));
                return CompletableFuture.completedFuture(true);
            });

            Mockito.when(currencyAdapter.getUserCurrency(Mockito.any(OfflinePlayer.class), Mockito.eq(currencyIdentifier))).thenAnswer(invocation -> {
                OfflinePlayer player = invocation.getArgument(0);
                return CompletableFuture.completedFuture(userCurrencies.get(player.getUniqueId()));
            });

            Mockito.when(userCurrencyRepository.createAsync(Mockito.any(UserCurrency.class))).thenAnswer(invocation -> {
                UserCurrency userCurrency = invocation.getArgument(0);
                userCurrencies.put(userCurrency.getPlayer().getUniqueId(), userCurrency);
                return CompletableFuture.completedFuture(userCurrency);
            });

            VaultMigrationManager manager = new VaultMigrationManager(economy);
            VaultMigrationManager.MigrationResult result = manager
                    .startMigration(false, false, currencyIdentifier)
                    .get(3, TimeUnit.SECONDS);

            assertTrue(result.isSuccess(), "Migration should succeed when all collaborators respond positively");
            assertEquals("Essentials", result.getSourceProvider(), "Detected provider should be surfaced in the result");

            VaultMigrationManager.MigrationStats stats = result.getStats();
            assertNotNull(stats, "Successful migrations should capture statistics");
            assertEquals(2, stats.getSuccessfulPlayers(), "Both mock players should migrate successfully");
            assertEquals(2, stats.getPlayersProcessed(), "Processed count should match player count");
            assertEquals(0, stats.getFailedPlayers(), "No players should fail during the happy-path migration");
            assertEquals(75.0, stats.getTotalBalance().doubleValue(), 0.0001, "Total migrated balance should be aggregated");
            assertTrue(stats.getErrors().isEmpty(), "Successful migrations must not report errors");

            Mockito.verify(currencyRepository).createAsync(Mockito.any(Currency.class));
            Mockito.verify(userCurrencyRepository, Mockito.times(2)).createAsync(Mockito.any(UserCurrency.class));

            assertTrue(handler.messages(Level.INFO).stream().anyMatch(message -> message.contains("Migration completed successfully")),
                    "Logger should record a success message for completed migrations");
            assertTrue(handler.messages(Level.INFO).stream().anyMatch(message -> message.contains("Migrated 2 players")),
                    "Logger should describe the migrated player count");
        }
    }

    @Test
    void startMigrationReportsErrorWhenVaultEconomyMissing() throws Exception {
        ExecutorService executor = registerExecutor();
        Logger logger = registerLogger();

        try (MockedStatic<CentralLogger> centralLogger = Mockito.mockStatic(CentralLogger.class);
             MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            centralLogger.when(() -> CentralLogger.getLogger(Mockito.anyString())).thenReturn(logger);

            ServicesManager servicesManager = Mockito.mock(ServicesManager.class);
            bukkit.when(Bukkit::getServicesManager).thenReturn(servicesManager);
            bukkit.when(Bukkit::getOfflinePlayers).thenReturn(new OfflinePlayer[0]);

            Mockito.when(servicesManager.getRegistration(Economy.class)).thenReturn(null);

            JExEconomyImpl economy = Mockito.mock(JExEconomyImpl.class, Mockito.withSettings().lenient());
            Mockito.when(economy.getExecutor()).thenReturn(executor);
            Mockito.when(economy.getCurrencies()).thenReturn(new ConcurrentHashMap<>());

            VaultMigrationManager manager = new VaultMigrationManager(economy);
            VaultMigrationManager.MigrationResult result = manager
                    .startMigration(false, false, null)
                    .get(2, TimeUnit.SECONDS);

            assertFalse(result.isSuccess(), "Migration must fail when no Vault provider is registered");
            assertEquals("Failed to detect economy provider: No Vault economy provider found", result.getErrorMessage(),
                    "Error message should communicate missing Vault provider");
        }
    }

    @Test
    void startMigrationSurfacesCurrencyCreationConflicts() throws Exception {
        ExecutorService executor = registerExecutor();
        Logger logger = registerLogger();
        TestLogHandler handler = handlers.get(handlers.size() - 1);

        try (MockedStatic<CentralLogger> centralLogger = Mockito.mockStatic(CentralLogger.class);
             MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            centralLogger.when(() -> CentralLogger.getLogger(Mockito.anyString())).thenReturn(logger);

            ServicesManager servicesManager = Mockito.mock(ServicesManager.class);
            RegisteredServiceProvider<Economy> registration = Mockito.mock(RegisteredServiceProvider.class);
            Economy vaultEconomy = Mockito.mock(Economy.class, Mockito.withSettings().lenient());

            bukkit.when(Bukkit::getServicesManager).thenReturn(servicesManager);
            bukkit.when(Bukkit::getOfflinePlayers).thenReturn(new OfflinePlayer[0]);

            Mockito.when(servicesManager.getRegistration(Economy.class)).thenReturn(registration);
            Mockito.when(registration.getProvider()).thenReturn(vaultEconomy);
            Mockito.when(vaultEconomy.getName()).thenReturn("Essentials");
            Mockito.when(vaultEconomy.currencyNamePlural()).thenReturn("Coins");
            Mockito.when(vaultEconomy.currencyNameSingular()).thenReturn("Coin");

            JExEconomyImpl economy = Mockito.mock(JExEconomyImpl.class, Mockito.withSettings().lenient());
            CurrencyRepository currencyRepository = Mockito.mock(CurrencyRepository.class);

            Mockito.when(economy.getExecutor()).thenReturn(executor);
            Mockito.when(economy.getCurrencies()).thenReturn(new ConcurrentHashMap<>());
            Mockito.when(economy.getCurrencyRepository()).thenReturn(currencyRepository);

            Mockito.when(currencyRepository.createAsync(Mockito.any(Currency.class))).thenReturn(
                    CompletableFuture.failedFuture(new IllegalStateException("duplicate identifier"))
            );

            VaultMigrationManager manager = new VaultMigrationManager(economy);
            VaultMigrationManager.MigrationResult result = manager
                    .startMigration(false, false, "vault-coins")
                    .get(2, TimeUnit.SECONDS);

            assertFalse(result.isSuccess(), "Migration should fail when target currency cannot be created");
            assertEquals("Failed to get or create target currency", result.getErrorMessage(),
                    "Currency creation failures should propagate through the result");

            assertTrue(handler.messages(Level.SEVERE).stream().anyMatch(message -> message.contains("Error getting or creating target currency")),
                    "Logger should capture the underlying currency creation exception");
        }
    }

    private ExecutorService registerExecutor() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executors.add(executor);
        return executor;
    }

    private Logger registerLogger() {
        Logger logger = Logger.getLogger("VaultMigrationManagerTest" + loggers.size());
        logger.setUseParentHandlers(false);
        TestLogHandler handler = new TestLogHandler();
        logger.addHandler(handler);
        loggers.add(logger);
        handlers.add(handler);
        return logger;
    }

    private OfflinePlayer[] createOfflinePlayers() {
        OfflinePlayer alice = Mockito.mock(OfflinePlayer.class, Mockito.withSettings().lenient());
        OfflinePlayer bob = Mockito.mock(OfflinePlayer.class, Mockito.withSettings().lenient());

        Mockito.when(alice.getName()).thenReturn("Alice");
        Mockito.when(alice.getUniqueId()).thenReturn(UUID.randomUUID());
        Mockito.when(bob.getName()).thenReturn("Bob");
        Mockito.when(bob.getUniqueId()).thenReturn(UUID.randomUUID());

        return new OfflinePlayer[]{alice, bob};
    }

    private static class TestLogHandler extends Handler {
        private final List<LogRecord> records = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
            // No-op for in-memory handler
        }

        @Override
        public void close() {
            records.clear();
        }

        List<String> messages(Level level) {
            List<String> filtered = new ArrayList<>();
            synchronized (records) {
                for (LogRecord record : records) {
                    if (record.getLevel().equals(level)) {
                        filtered.add(record.getMessage());
                    }
                }
            }
            return filtered;
        }
    }
}
