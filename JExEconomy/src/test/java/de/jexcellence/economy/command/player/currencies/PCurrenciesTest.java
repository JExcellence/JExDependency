package de.jexcellence.economy.command.player.currencies;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.repository.CurrencyRepository;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PCurrenciesTest {

        private ServerMock server;
        private PlayerMock player;
        private PCurrencies command;
        private CurrencyRepository currencyRepository;
        private ExecutorService executorService;
        private MockedStatic<TranslationService> translationService;
        private final List<TranslationCapture> sentMessages = new ArrayList<>();

        @BeforeEach
        void setUp() {
                this.server = MockBukkit.mock();
                this.player = this.server.addPlayer();

                final PCurrenciesSection commandSection = Mockito.mock(PCurrenciesSection.class);
                Mockito.when(commandSection.getName()).thenReturn("pcurrencies");
                Mockito.when(commandSection.getDescription()).thenReturn("player currencies");
                Mockito.when(commandSection.getUsage()).thenReturn("/pcurrencies");
                Mockito.when(commandSection.getAliases()).thenReturn(List.of());
                Mockito.when(commandSection.getPermissions()).thenReturn(null);

                final JExEconomy plugin = Mockito.mock(JExEconomy.class);
                final JExEconomyImpl pluginImplementation = Mockito.mock(JExEconomyImpl.class);
                Mockito.when(plugin.getImpl()).thenReturn(pluginImplementation);

                this.currencyRepository = Mockito.mock(CurrencyRepository.class);
                Mockito.when(pluginImplementation.getCurrencyRepository()).thenReturn(this.currencyRepository);

                this.executorService = Mockito.mock(ExecutorService.class);
                Mockito.doAnswer(invocation -> {
                        final Runnable runnable = invocation.getArgument(0);
                        runnable.run();
                        return null;
                }).when(this.executorService).execute(Mockito.any());
                Mockito.when(pluginImplementation.getExecutor()).thenReturn(this.executorService);

                this.command = new PCurrencies(commandSection, plugin);

                this.translationService = Mockito.mockStatic(TranslationService.class);
                this.translationService.when(() -> TranslationService.create(Mockito.any(TranslationKey.class), Mockito.any()))
                                       .thenAnswer(this::captureTranslation);
        }

        @AfterEach
        void tearDown() {
                if (this.translationService != null) {
                        this.translationService.close();
                }
                MockBukkit.unmock();
        }

        @Test
        void overviewListsCurrenciesUsingDefaultOrdering() {
                Mockito.when(this.currencyRepository.findAll(Mockito.anyInt(), Mockito.anyInt()))
                       .thenReturn(List.of(
                               this.currency("beta", "B", "", ""),
                               this.currency("alpha", "A", "", ""),
                               this.currency("delta", "D", "", "")
                       ));

                assertTrue(this.command.execute(this.player, "pcurrencies", new String[]{"overview"}));

                assertEquals(4, this.sentMessages.size(), "Expected header plus three currency entries");
                final TranslationCapture header = this.sentMessages.get(0);
                assertEquals("currency.list.header", header.key().key());
                assertEquals(3, header.placeholders().get("count"));

                final List<String> identifiers = this.sentMessages.subList(1, this.sentMessages.size())
                                                                  .stream()
                                                                  .map(capture -> (String) capture.placeholders().get("identifier"))
                                                                  .toList();
                assertEquals(List.of("alpha", "beta", "delta"), identifiers);
        }

        @Test
        void overviewHonoursPaginationArguments() {
                Mockito.when(this.currencyRepository.findAll(Mockito.anyInt(), Mockito.anyInt()))
                       .thenReturn(List.of(
                               this.currency("alpha", "A", "", ""),
                               this.currency("bravo", "B", "", ""),
                               this.currency("charlie", "C", "", ""),
                               this.currency("delta", "D", "", ""),
                               this.currency("echo", "E", "", "")
                       ));

                assertTrue(this.command.execute(this.player, "pcurrencies", new String[]{"overview", "2", "2"}));

                assertEquals(3, this.sentMessages.size(), "Expected header and two paginated entries");
                final List<String> identifiers = this.sentMessages.subList(1, this.sentMessages.size())
                                                                  .stream()
                                                                  .map(capture -> (String) capture.placeholders().get("identifier"))
                                                                  .toList();
                assertEquals(List.of("charlie", "delta"), identifiers);
        }

        @Test
        void overviewSupportsSortingConfiguration() {
                Mockito.when(this.currencyRepository.findAll(Mockito.anyInt(), Mockito.anyInt()))
                       .thenReturn(List.of(
                               this.currency("alpha", "Z", "", ""),
                               this.currency("bravo", "M", "", ""),
                               this.currency("charlie", "A", "", "")
                       ));

                assertTrue(this.command.execute(this.player, "pcurrencies", new String[]{"overview", "1", "3", "symbol", "desc"}));

                assertEquals(4, this.sentMessages.size(), "Expected header plus sorted entries");
                final List<String> symbols = this.sentMessages.subList(1, this.sentMessages.size())
                                                              .stream()
                                                              .map(capture -> (String) capture.placeholders().get("symbol"))
                                                              .toList();
                assertEquals(List.of("Z", "M", "A"), symbols);
        }

        @Test
        void overviewSendsEmptyMessageWhenRepositoryReturnsNoCurrencies() {
                Mockito.when(this.currencyRepository.findAll(Mockito.anyInt(), Mockito.anyInt()))
                       .thenReturn(List.of());

                assertTrue(this.command.execute(this.player, "pcurrencies", new String[]{"overview"}));

                assertEquals(1, this.sentMessages.size(), "Expected only empty-state message");
                assertEquals("currency.list.empty", this.sentMessages.get(0).key().key());
        }

        @Test
        void overviewRejectsInvalidFilterExpressions() {
                Mockito.when(this.currencyRepository.findAll(Mockito.anyInt(), Mockito.anyInt()))
                       .thenReturn(List.of(this.currency("alpha", "A", "", "")));

                assertTrue(this.command.execute(this.player, "pcurrencies", new String[]{"overview", "1", "5", "identifier", "asc", "invalid"}));

                assertEquals(1, this.sentMessages.size(), "Expected invalid filter notification only");
                final TranslationCapture capture = this.sentMessages.get(0);
                assertEquals("currency.list.invalid_filter", capture.key().key());
                assertEquals("invalid", capture.placeholders().get("filter"));
        }

        private TranslationService captureTranslation(final InvocationOnMock invocation) {
                final TranslationKey key = invocation.getArgument(0, TranslationKey.class);
                final TranslationService translation = Mockito.mock(TranslationService.class, Answers.RETURNS_SELF);
                final Map<String, Object> placeholders = new LinkedHashMap<>();

                Mockito.when(translation.with(Mockito.anyString(), Mockito.any())).thenAnswer(withInvocation -> {
                        placeholders.put(withInvocation.getArgument(0), withInvocation.getArgument(1));
                        return translation;
                });

                Mockito.doAnswer(sendInvocation -> {
                        this.sentMessages.add(new TranslationCapture(key, new LinkedHashMap<>(placeholders)));
                        return null;
                }).when(translation).send();

                return translation;
        }

        private Currency currency(
                final String identifier,
                final String symbol,
                final String prefix,
                final String suffix
        ) {
                return new Currency(prefix, suffix, identifier, symbol, Material.GOLD_INGOT);
        }

        private record TranslationCapture(
                TranslationKey key,
                Map<String, Object> placeholders
        ) {
        }
}
