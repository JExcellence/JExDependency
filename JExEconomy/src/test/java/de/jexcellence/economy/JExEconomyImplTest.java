package de.jexcellence.economy;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.economy.adapter.CurrencyAdapter;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.repository.CurrencyLogRepository;
import de.jexcellence.economy.database.repository.CurrencyRepository;
import de.jexcellence.economy.database.repository.UserCurrencyRepository;
import de.jexcellence.economy.database.repository.UserRepository;
import de.jexcellence.economy.migrate.VaultMigrationManager;
import de.jexcellence.economy.service.CurrencyLogService;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JExEconomyImplTest {

    private ServerMock server;
    private Logger pluginLogger;
    private JExEconomy plugin;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.pluginLogger = Logger.getLogger("JExEconomy-Test");
        this.plugin = Mockito.mock(JExEconomy.class, Mockito.withSettings().lenient());

        PluginDescriptionFile description = new PluginDescriptionFile(
                "JExEconomy",
                "2.0.0",
                JExEconomy.class.getName()
        );

        Mockito.when(this.plugin.getLogger()).thenReturn(this.pluginLogger);
        Mockito.when(this.plugin.getServer()).thenReturn(this.server);
        Mockito.when(this.plugin.getDataFolder()).thenReturn(this.tempDir.toFile());
        Mockito.when(this.plugin.getDescription()).thenReturn(description);
        Mockito.when(this.plugin.getName()).thenReturn("JExEconomy");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onLoadRegistersAdapterAndInitializesPlatform() {
        try (MockedStatic<CentralLogger> centralLogger = Mockito.mockStatic(CentralLogger.class);
             MockedConstruction<RPlatform> platformConstruction = Mockito.mockConstruction(
                     RPlatform.class,
                     (mock, context) -> Mockito.when(mock.initialize())
                             .thenReturn(CompletableFuture.completedFuture(null))
             );
             MockedConstruction<VaultMigrationManager> migrationConstruction = Mockito.mockConstruction(
                     VaultMigrationManager.class
             )) {

            centralLogger.when(() -> CentralLogger.initialize(Mockito.any(JavaPlugin.class))).then(invocation -> null);
            centralLogger.when(() -> CentralLogger.getLogger(Mockito.anyString())).thenReturn(this.pluginLogger);

            JExEconomyImpl economy = new JExEconomyImpl(this.plugin);

            economy.onLoad();

            RPlatform platform = platformConstruction.constructed().getFirst();
            Mockito.verify(platform).initialize();
            assertSame(platform, economy.getPlatform(), "Platform abstraction should be stored after load");

            assertNotNull(economy.getCurrencyAdapter(), "Currency adapter must be created during load");

            RegisteredServiceProvider<CurrencyAdapter> registration =
                    this.server.getServicesManager().getRegistration(CurrencyAdapter.class);
            assertNotNull(registration, "Service manager should expose the adapter after registration");
            assertSame(economy.getCurrencyAdapter(), registration.getProvider(),
                    "Registered provider must match the delegate's adapter instance");

            VaultMigrationManager migrationManager = migrationConstruction.constructed().getFirst();
            assertSame(migrationManager, economy.getVaultMigrationManager(),
                    "Vault migration manager should be created during plugin load");
        }
    }

    @Test
    void onEnableCreatesCollaboratorsAndLoadsCurrencies() throws Exception {
        CompletableFuture<List<Currency>> currencyFuture = new CompletableFuture<>();

        try (MockedStatic<CentralLogger> centralLogger = Mockito.mockStatic(CentralLogger.class);
             MockedConstruction<RPlatform> platformConstruction = Mockito.mockConstruction(
                     RPlatform.class,
                     (mock, context) -> Mockito.when(mock.initialize())
                             .thenReturn(CompletableFuture.completedFuture(null))
             );
             MockedConstruction<VaultMigrationManager> migrationConstruction = Mockito.mockConstruction(
                     VaultMigrationManager.class
             );
             MockedConstruction<CommandFactory> commandConstruction = Mockito.mockConstruction(
                     CommandFactory.class,
                     (mock, context) -> Mockito.doNothing().when(mock).registerAllCommandsAndListeners()
             );
             MockedConstruction<UserRepository> userRepositoryConstruction = Mockito.mockConstruction(UserRepository.class);
             MockedConstruction<CurrencyRepository> currencyRepositoryConstruction = Mockito.mockConstruction(
                     CurrencyRepository.class,
                     (mock, context) -> Mockito.when(mock.findAllAsync(Mockito.anyInt(), Mockito.anyInt()))
                             .thenReturn(currencyFuture)
             );
             MockedConstruction<UserCurrencyRepository> userCurrencyRepositoryConstruction =
                     Mockito.mockConstruction(UserCurrencyRepository.class);
             MockedConstruction<CurrencyLogRepository> currencyLogRepositoryConstruction =
                     Mockito.mockConstruction(CurrencyLogRepository.class);
             MockedConstruction<CurrencyLogService> currencyLogServiceConstruction =
                     Mockito.mockConstruction(CurrencyLogService.class);
             MockedStatic<ViewFrame> viewFrameStatic = Mockito.mockStatic(ViewFrame.class)) {

            centralLogger.when(() -> CentralLogger.initialize(Mockito.any(JavaPlugin.class))).then(invocation -> null);
            centralLogger.when(() -> CentralLogger.getLogger(Mockito.anyString())).thenReturn(this.pluginLogger);

            ViewFrame viewFrame = Mockito.mock(ViewFrame.class, Mockito.withSettings().defaultAnswer(Mockito.RETURNS_SELF));
            viewFrameStatic.when(() -> ViewFrame.create(Mockito.any(JavaPlugin.class))).thenReturn(viewFrame);
            Mockito.when(viewFrame.register()).thenReturn(viewFrame);

            JExEconomyImpl economy = new JExEconomyImpl(this.plugin);
            economy.onLoad();

            Currency currency = Mockito.mock(Currency.class);
            Mockito.when(currency.getId()).thenReturn(42L);
            currencyFuture.complete(List.of(currency));

            economy.onEnable();

            CommandFactory commandFactory = commandConstruction.constructed().getFirst();
            Mockito.verify(commandFactory).registerAllCommandsAndListeners();

            assertSame(userRepositoryConstruction.constructed().getFirst(), economy.getUserRepository(),
                    "User repository should be captured for later access");
            assertSame(currencyRepositoryConstruction.constructed().getFirst(), economy.getCurrencyRepository(),
                    "Currency repository should be exposed via getter");
            assertSame(userCurrencyRepositoryConstruction.constructed().getFirst(), economy.getUserCurrencyRepository(),
                    "User currency repository should be exposed via getter");
            assertSame(currencyLogRepositoryConstruction.constructed().getFirst(), economy.getCurrencyLogRepository(),
                    "Currency log repository should be stored");
            assertSame(currencyLogServiceConstruction.constructed().getFirst(), economy.getLogService(),
                    "Currency log service should be initialized");
            assertSame(viewFrame, economy.getViewFrame(), "Inventory view frame should be created via builder chain");

            economy.getExecutor().shutdown();
            economy.getExecutor().awaitTermination(1, TimeUnit.SECONDS);

            assertEquals(1, economy.getCurrencies().size(),
                    "Loaded currencies should be cached after the async pipeline completes");
            assertSame(currency, economy.getCurrencies().get(42L),
                    "Cached currency should match the repository result");
        }
    }

    @Test
    void onDisableUnregistersAdapterAndShutsDownExecutor() {
        CompletableFuture<List<Currency>> currencyFuture = new CompletableFuture<>();

        try (MockedStatic<CentralLogger> centralLogger = Mockito.mockStatic(CentralLogger.class);
             MockedConstruction<RPlatform> platformConstruction = Mockito.mockConstruction(
                     RPlatform.class,
                     (mock, context) -> Mockito.when(mock.initialize())
                             .thenReturn(CompletableFuture.completedFuture(null))
             );
             MockedConstruction<VaultMigrationManager> migrationConstruction = Mockito.mockConstruction(
                     VaultMigrationManager.class
             );
             MockedConstruction<CommandFactory> commandConstruction = Mockito.mockConstruction(
                     CommandFactory.class,
                     (mock, context) -> Mockito.doNothing().when(mock).registerAllCommandsAndListeners()
             );
             MockedConstruction<UserRepository> userRepositoryConstruction = Mockito.mockConstruction(UserRepository.class);
             MockedConstruction<CurrencyRepository> currencyRepositoryConstruction = Mockito.mockConstruction(
                     CurrencyRepository.class,
                     (mock, context) -> Mockito.when(mock.findAllAsync(Mockito.anyInt(), Mockito.anyInt()))
                             .thenReturn(currencyFuture)
             );
             MockedConstruction<UserCurrencyRepository> userCurrencyRepositoryConstruction =
                     Mockito.mockConstruction(UserCurrencyRepository.class);
             MockedConstruction<CurrencyLogRepository> currencyLogRepositoryConstruction =
                     Mockito.mockConstruction(CurrencyLogRepository.class);
             MockedConstruction<CurrencyLogService> currencyLogServiceConstruction =
                     Mockito.mockConstruction(CurrencyLogService.class);
             MockedStatic<ViewFrame> viewFrameStatic = Mockito.mockStatic(ViewFrame.class)) {

            centralLogger.when(() -> CentralLogger.initialize(Mockito.any(JavaPlugin.class))).then(invocation -> null);
            centralLogger.when(() -> CentralLogger.getLogger(Mockito.anyString())).thenReturn(this.pluginLogger);

            ViewFrame viewFrame = Mockito.mock(ViewFrame.class, Mockito.withSettings().defaultAnswer(Mockito.RETURNS_SELF));
            viewFrameStatic.when(() -> ViewFrame.create(Mockito.any(JavaPlugin.class))).thenReturn(viewFrame);
            Mockito.when(viewFrame.register()).thenReturn(viewFrame);

            JExEconomyImpl economy = new JExEconomyImpl(this.plugin);
            economy.onLoad();

            currencyFuture.complete(List.of());
            economy.onEnable();

            assertNotNull(this.server.getServicesManager().getRegistration(CurrencyAdapter.class),
                    "Service registration should exist before shutdown");

            economy.onDisable();

            assertNull(this.server.getServicesManager().getRegistration(CurrencyAdapter.class),
                    "Currency adapter should be unregistered on shutdown");
            assertTrue(economy.getExecutor().isShutdown(),
                    "Executor service must be shut down during disable");
        }
    }
}
