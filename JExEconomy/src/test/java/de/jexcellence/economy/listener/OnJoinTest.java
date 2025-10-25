package de.jexcellence.economy.listener;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.database.entity.UserCurrency;
import de.jexcellence.economy.database.repository.UserCurrencyRepository;
import de.jexcellence.economy.database.repository.UserRepository;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnJoinTest {

    @Mock
    private JExEconomy plugin;

    @Mock
    private JExEconomyImpl pluginImplementation;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCurrencyRepository userCurrencyRepository;

    @Mock
    private ExecutorService executorService;

    @Mock
    private Currency currency;

    private ServerMock server;

    private OnJoin listener;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(this.executorService).execute(any());

        Map<Long, Currency> currencies = new LinkedHashMap<>();
        currencies.put(1L, this.currency);

        when(this.plugin.getImpl()).thenReturn(this.pluginImplementation);
        when(this.pluginImplementation.getUserRepository()).thenReturn(this.userRepository);
        when(this.pluginImplementation.getUserCurrencyRepository()).thenReturn(this.userCurrencyRepository);
        when(this.pluginImplementation.getCurrencies()).thenReturn(currencies);
        when(this.pluginImplementation.getExecutor()).thenReturn(this.executorService);
        when(this.currency.getId()).thenReturn(1L);
        when(this.currency.getIdentifier()).thenReturn("coins");

        this.listener = new OnJoin(this.plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void shouldCreateNewUserAndInitializeCurrenciesOnFirstJoin() throws Exception {
        UUID playerId = UUID.randomUUID();
        String playerName = "NewPlayer";

        when(this.userRepository.findByAttributesAsync(anyMap()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(this.userCurrencyRepository.findByAttributesAsync(anyMap()))
                .thenReturn(CompletableFuture.completedFuture(null));

        AsyncPlayerPreLoginEvent event = new AsyncPlayerPreLoginEvent(
                playerName,
                InetAddress.getLoopbackAddress(),
                playerId
        );

        this.listener.onPlayerPreLogin(event);

        ArgumentCaptor<Map<String, Object>> userLookupCaptor = ArgumentCaptor.forClass(Map.class);
        verify(this.userRepository).findByAttributesAsync(userLookupCaptor.capture());
        Map<String, Object> userLookup = userLookupCaptor.getValue();
        assertEquals(playerId, userLookup.get("uniqueId"));

        ArgumentCaptor<User> createdUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(this.userRepository).create(createdUserCaptor.capture());
        User createdUser = createdUserCaptor.getValue();
        assertEquals(playerId, createdUser.getUniqueId());
        assertEquals(playerName, createdUser.getPlayerName());

        verify(this.userRepository, never()).update(any());

        ArgumentCaptor<Map<String, Object>> currencyLookupCaptor = ArgumentCaptor.forClass(Map.class);
        verify(this.userCurrencyRepository).findByAttributesAsync(currencyLookupCaptor.capture());
        Map<String, Object> currencyLookup = currencyLookupCaptor.getValue();
        assertEquals(playerId, currencyLookup.get("player.uniqueId"));
        assertEquals(1L, currencyLookup.get("currency.id"));

        ArgumentCaptor<UserCurrency> createdAssociationCaptor = ArgumentCaptor.forClass(UserCurrency.class);
        verify(this.userCurrencyRepository).create(createdAssociationCaptor.capture());
        UserCurrency createdAssociation = createdAssociationCaptor.getValue();
        assertEquals(playerId, createdAssociation.getPlayer().getUniqueId());
        assertSame(this.currency, createdAssociation.getCurrency());
    }

    @Test
    void shouldUpdateExistingUserOnReturningJoin() throws Exception {
        UUID playerId = UUID.randomUUID();
        String oldName = "OldName";
        String updatedName = "UpdatedName";

        User existingUser = new User(playerId, oldName);
        UserCurrency existingAssociation = new UserCurrency(existingUser, this.currency);

        when(this.userRepository.findByAttributesAsync(anyMap()))
                .thenReturn(CompletableFuture.completedFuture(existingUser));
        when(this.userCurrencyRepository.findByAttributesAsync(anyMap()))
                .thenReturn(CompletableFuture.completedFuture(existingAssociation));

        AsyncPlayerPreLoginEvent event = new AsyncPlayerPreLoginEvent(
                updatedName,
                InetAddress.getLoopbackAddress(),
                playerId
        );

        this.listener.onPlayerPreLogin(event);

        verify(this.userRepository, never()).create(any());

        verify(this.userRepository).update(existingUser);
        assertEquals(updatedName, existingUser.getPlayerName());

        verify(this.userCurrencyRepository, times(1)).findByAttributesAsync(anyMap());
        verify(this.userCurrencyRepository, never()).create(any());
    }
}
