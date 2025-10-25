package de.jexcellence.economy;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rplatform.RPlatform;
import de.jexcellence.dependency.JEDependency;
import de.jexcellence.economy.adapter.CurrencyAdapter;
import de.jexcellence.economy.database.repository.CurrencyLogRepository;
import de.jexcellence.economy.database.repository.CurrencyRepository;
import de.jexcellence.economy.database.repository.UserCurrencyRepository;
import de.jexcellence.economy.database.repository.UserRepository;
import de.jexcellence.economy.migrate.VaultMigrationManager;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import jakarta.persistence.EntityManagerFactory;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JExEconomyTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void pluginLifecycleDelegatesToImplementation() {
        try (
                MockedStatic<JEDependency> dependencyBootstrap = mockStatic(JEDependency.class);
                MockedConstruction<RPlatform> platformConstruction = mockConstruction(RPlatform.class, (mock, context) -> {
                    when(mock.initialize()).thenReturn(CompletableFuture.completedFuture(null));
                    when(mock.getEntityManagerFactory()).thenReturn(mock(EntityManagerFactory.class));
                });
                MockedConstruction<CurrencyAdapter> currencyAdapterConstruction = mockConstruction(CurrencyAdapter.class);
                MockedConstruction<VaultMigrationManager> vaultMigrationManagerConstruction = mockConstruction(VaultMigrationManager.class);
                MockedConstruction<UserRepository> userRepositoryConstruction = mockConstruction(UserRepository.class);
                MockedConstruction<UserCurrencyRepository> userCurrencyRepositoryConstruction = mockConstruction(UserCurrencyRepository.class);
                MockedConstruction<CurrencyLogRepository> currencyLogRepositoryConstruction = mockConstruction(CurrencyLogRepository.class);
                MockedConstruction<CurrencyRepository> currencyRepositoryConstruction = mockConstruction(CurrencyRepository.class, (mock, context) ->
                        when(mock.findAllAsync(anyInt(), anyInt()))
                                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()))
                );
                MockedConstruction<CommandFactory> commandFactoryConstruction = mockConstruction(CommandFactory.class, (mock, context) ->
                        doNothing().when(mock).registerAllCommandsAndListeners()
                );
                MockedStatic<ViewFrame> viewFrameStatic = mockStatic(ViewFrame.class)
        ) {
            final ViewFrame builder = mock(ViewFrame.class, Mockito.withSettings().defaultAnswer(Answers.RETURNS_SELF));
            final ViewFrame registeredFrame = mock(ViewFrame.class);
            when(builder.register()).thenReturn(registeredFrame);
            viewFrameStatic.when(() -> ViewFrame.create(any(JavaPlugin.class))).thenReturn(builder);

            dependencyBootstrap.when(() -> JEDependency.initializeWithRemapping(any(JavaPlugin.class), eq(JExEconomy.class)))
                    .thenAnswer(invocation -> null);

            final JExEconomy plugin = MockBukkit.load(JExEconomy.class);
            final JExEconomyImpl implementation = plugin.getImpl();

            assertThat(implementation).as("Plugin delegate should be created during onLoad()").isNotNull();
            assertThat(platformConstruction.constructed()).hasSize(1);
            assertThat(implementation.getPlatform()).isSameAs(platformConstruction.constructed().get(0));

            assertThat(currencyAdapterConstruction.constructed()).hasSize(1);
            assertThat(implementation.getCurrencyAdapter()).isSameAs(currencyAdapterConstruction.constructed().get(0));

            final RegisteredServiceProvider<CurrencyAdapter> registeredServiceProvider =
                    this.server.getServicesManager().getRegistration(CurrencyAdapter.class);
            assertThat(registeredServiceProvider).as("CurrencyAdapter service should be registered on enable").isNotNull();
            assertThat(registeredServiceProvider.getProvider()).isSameAs(implementation.getCurrencyAdapter());

            assertThat(commandFactoryConstruction.constructed()).hasSize(1);
            final CommandFactory commandFactory = commandFactoryConstruction.constructed().get(0);
            assertThat(implementation.getCommandFactory()).isSameAs(commandFactory);
            verify(commandFactory).registerAllCommandsAndListeners();

            assertThat(implementation.getViewFrame()).isSameAs(registeredFrame);

            dependencyBootstrap.verify(() -> JEDependency.initializeWithRemapping(plugin, JExEconomy.class));

            plugin.onDisable();

            assertThat(implementation.getExecutor().isShutdown()).isTrue();
            assertThat(this.server.getServicesManager().getRegistration(CurrencyAdapter.class))
                    .as("CurrencyAdapter service should be unregistered on disable")
                    .isNull();
        }
    }
}
