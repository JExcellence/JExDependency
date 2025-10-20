package com.raindropcentral.rplatform.console;

import be.seeseemelk.mockbukkit.MockBukkit;
import de.jexcellence.jextranslate.api.LocaleResolver;
import de.jexcellence.jextranslate.api.MessageFormatter;
import de.jexcellence.jextranslate.api.Placeholder;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationRepository;
import de.jexcellence.jextranslate.api.TranslationService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.ConsoleCommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleMessengerTest {

    private Logger logger;
    private TestLogHandler logHandler;
    private ConsoleCommandSender consoleSender;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        this.consoleSender = MockBukkit.getMock().getConsoleSender();
        this.logger = this.consoleSender.getServer().getLogger();
        this.logHandler = new TestLogHandler();
        this.logger.addHandler(this.logHandler);
    }

    @AfterEach
    void tearDown() {
        this.logger.removeHandler(this.logHandler);
        MockBukkit.unmock();
    }

    @Test
    void logLevelMethodsRouteThroughLoggerWithPrefix() {
        final TranslationRepository repository = Mockito.mock(TranslationRepository.class);
        final MessageFormatter formatter = Mockito.mock(MessageFormatter.class);
        final LocaleResolver localeResolver = Mockito.mock(LocaleResolver.class);
        Mockito.when(localeResolver.getDefaultLocale()).thenReturn(Locale.US);

        final TranslationKey key = TranslationKey.of("diagnostic.message");
        final String template = "<green>Hello Adventurer</green>";
        final String prefixTemplate = "<gray>[R] </gray>";
        Mockito.when(repository.getTranslation(key, Locale.US)).thenReturn(Optional.of(template));
        Mockito.when(repository.getTranslation(TranslationKey.of("prefix"), Locale.US)).thenReturn(Optional.of(prefixTemplate));

        final Component messageComponent = Component.text("Hello Adventurer", NamedTextColor.GREEN);
        final Component prefixComponent = Component.text("[R] ", NamedTextColor.GRAY);
        Mockito.when(formatter.formatComponent(Mockito.eq(template), Mockito.anyList(), Mockito.eq(Locale.US)))
                .thenReturn(messageComponent);
        Mockito.when(formatter.formatComponent(Mockito.eq(prefixTemplate), Mockito.anyList(), Mockito.eq(Locale.US)))
                .thenReturn(prefixComponent);

        final TranslationService.ServiceConfiguration configuration =
                new TranslationService.ServiceConfiguration(repository, formatter, localeResolver);

        try (MockedStatic<TranslationService> translationService = Mockito.mockStatic(TranslationService.class)) {
            translationService.when(TranslationService::getConfiguration).thenReturn(configuration);

            final ConsoleMessenger messenger = new ConsoleMessenger(this.logger);
            final Map<String, Object> placeholders = Map.of("player", "Adventurer");

            messenger.info(key, placeholders);
            LogRecord infoRecord = this.logHandler.singleRecord();
            assertEquals(Level.INFO, infoRecord.getLevel());
            assertEquals("[R] Hello Adventurer", infoRecord.getMessage());

            this.logHandler.clear();
            messenger.warn(key, placeholders);
            LogRecord warnRecord = this.logHandler.singleRecord();
            assertEquals(Level.WARNING, warnRecord.getLevel());
            assertEquals("[R] Hello Adventurer", warnRecord.getMessage());

            this.logHandler.clear();
            messenger.error(key, placeholders);
            LogRecord errorRecord = this.logHandler.singleRecord();
            assertEquals(Level.SEVERE, errorRecord.getLevel());
            assertEquals("[R] Hello Adventurer", errorRecord.getMessage());

            this.logHandler.clear();
            messenger.log(Level.CONFIG, key, placeholders);
            LogRecord genericRecord = this.logHandler.singleRecord();
            assertEquals(Level.CONFIG, genericRecord.getLevel());
            assertEquals("[R] Hello Adventurer", genericRecord.getMessage());
        }
    }

    @Test
    void translateProducesRichComponentWithPrefixColors() {
        final TranslationRepository repository = Mockito.mock(TranslationRepository.class);
        final MessageFormatter formatter = Mockito.mock(MessageFormatter.class);
        final LocaleResolver localeResolver = Mockito.mock(LocaleResolver.class);
        Mockito.when(localeResolver.getDefaultLocale()).thenReturn(Locale.US);

        final TranslationKey key = TranslationKey.of("console.success");
        final String template = "<green>Operation completed</green>";
        final String prefixTemplate = "<blue>[Console] </blue>";
        Mockito.when(repository.getTranslation(key, Locale.US)).thenReturn(Optional.of(template));
        Mockito.when(repository.getTranslation(TranslationKey.of("prefix"), Locale.US)).thenReturn(Optional.of(prefixTemplate));

        final Component messageComponent = Component.text("Operation completed", NamedTextColor.GREEN);
        final Component prefixComponent = Component.text("[Console] ", NamedTextColor.BLUE);
        Mockito.when(formatter.formatComponent(Mockito.eq(template), Mockito.anyList(), Mockito.eq(Locale.US)))
                .thenReturn(messageComponent);
        Mockito.when(formatter.formatComponent(Mockito.eq(prefixTemplate), Mockito.anyList(), Mockito.eq(Locale.US)))
                .thenReturn(prefixComponent);

        final TranslationService.ServiceConfiguration configuration =
                new TranslationService.ServiceConfiguration(repository, formatter, localeResolver);

        try (MockedStatic<TranslationService> translationService = Mockito.mockStatic(TranslationService.class)) {
            translationService.when(TranslationService::getConfiguration).thenReturn(configuration);

            final ConsoleMessenger messenger = new ConsoleMessenger(this.logger, null, true, null);
            final TranslatedMessage translated = messenger.translate(key, Map.of("status", "ok"));

            final Component expected = Component.join(
                    JoinConfiguration.noSeparators(),
                    prefixComponent,
                    messageComponent
            );
            assertEquals(expected, translated.component());
            assertEquals("[Console] Operation completed", translated.asPlainText());
        }
    }

    @Test
    void translateFallsBackToKeyWhenTranslationMissing() {
        final TranslationRepository repository = Mockito.mock(TranslationRepository.class);
        final MessageFormatter formatter = Mockito.mock(MessageFormatter.class);
        final LocaleResolver localeResolver = Mockito.mock(LocaleResolver.class);
        Mockito.when(localeResolver.getDefaultLocale()).thenReturn(Locale.UK);

        final TranslationKey key = TranslationKey.of("console.missing");
        final String prefixTemplate = "<gray>[Fallback] </gray>";
        Mockito.when(repository.getTranslation(key, Locale.UK)).thenReturn(Optional.empty());
        Mockito.when(repository.getTranslation(TranslationKey.of("prefix"), Locale.UK)).thenReturn(Optional.of(prefixTemplate));

        final Component prefixComponent = Component.text("[Fallback] ", NamedTextColor.GRAY);
        Mockito.when(formatter.formatComponent(Mockito.eq(prefixTemplate), Mockito.anyList(), Mockito.eq(Locale.UK)))
                .thenReturn(prefixComponent);

        final TranslationService.ServiceConfiguration configuration =
                new TranslationService.ServiceConfiguration(repository, formatter, localeResolver);

        try (MockedStatic<TranslationService> translationService = Mockito.mockStatic(TranslationService.class)) {
            translationService.when(TranslationService::getConfiguration).thenReturn(configuration);

            final ConsoleMessenger messenger = new ConsoleMessenger(this.logger);
            final TranslatedMessage translated = messenger.translate(key, Map.of());

            final Component expected = Component.join(
                    JoinConfiguration.noSeparators(),
                    prefixComponent,
                    Component.text(key.key())
            );
            assertEquals(expected, translated.component());
            assertEquals("[Fallback] console.missing", translated.asPlainText());
        }
    }

    @Test
    void translateEmitsWarningAndFallbackWhenFormatterFails() {
        final TranslationRepository repository = Mockito.mock(TranslationRepository.class);
        final MessageFormatter formatter = Mockito.mock(MessageFormatter.class);
        final LocaleResolver localeResolver = Mockito.mock(LocaleResolver.class);
        Mockito.when(localeResolver.getDefaultLocale()).thenReturn(Locale.CANADA);

        final TranslationKey key = TranslationKey.of("console.failure");
        final String template = "<red>Error happened</red>";
        Mockito.when(repository.getTranslation(key, Locale.CANADA)).thenReturn(Optional.of(template));

        Mockito.when(formatter.formatComponent(Mockito.eq(template), Mockito.anyList(), Mockito.eq(Locale.CANADA)))
                .thenThrow(new IllegalStateException("bad format"));

        final TranslationService.ServiceConfiguration configuration =
                new TranslationService.ServiceConfiguration(repository, formatter, localeResolver);

        try (MockedStatic<TranslationService> translationService = Mockito.mockStatic(TranslationService.class)) {
            translationService.when(TranslationService::getConfiguration).thenReturn(configuration);

            final ConsoleMessenger messenger = new ConsoleMessenger(this.logger, Locale.CANADA, false, null);
            final TranslatedMessage translated = messenger.translate(key, Map.of());

            assertEquals(Component.text("[" + key.key() + "]"), translated.component());
            assertEquals("[console.failure]", translated.asPlainText());

            LogRecord warning = this.logHandler.lastRecord();
            assertNotNull(warning);
            assertEquals(Level.WARNING, warning.getLevel());
            assertTrue(warning.getMessage().contains("Failed to render console translation for key " + key));
        }
    }

    @Test
    void translateBuildsPlaceholderVariantsAndHandlesNulls() {
        final TranslationRepository repository = Mockito.mock(TranslationRepository.class);
        final MessageFormatter formatter = Mockito.mock(MessageFormatter.class);
        final LocaleResolver localeResolver = Mockito.mock(LocaleResolver.class);
        Mockito.when(localeResolver.getDefaultLocale()).thenReturn(Locale.GERMANY);

        final TranslationKey key = TranslationKey.of("console.placeholders");
        final String template = "Placeholder test";
        final String prefixTemplate = "<gold>[Placeholders] </gold>";
        Mockito.when(repository.getTranslation(key, Locale.GERMANY)).thenReturn(Optional.of(template));
        Mockito.when(repository.getTranslation(TranslationKey.of("prefix"), Locale.GERMANY)).thenReturn(Optional.of(prefixTemplate));

        final Component messageComponent = Component.text("Placeholder test", NamedTextColor.WHITE);
        final Component prefixComponent = Component.text("[Placeholders] ", NamedTextColor.GOLD);
        final ArgumentCaptor<List<Placeholder>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.when(formatter.formatComponent(Mockito.eq(template), captor.capture(), Mockito.eq(Locale.GERMANY)))
                .thenReturn(messageComponent);
        Mockito.when(formatter.formatComponent(Mockito.eq(prefixTemplate), Mockito.anyList(), Mockito.eq(Locale.GERMANY)))
                .thenReturn(prefixComponent);

        final TranslationService.ServiceConfiguration configuration =
                new TranslationService.ServiceConfiguration(repository, formatter, localeResolver);

        try (MockedStatic<TranslationService> translationService = Mockito.mockStatic(TranslationService.class)) {
            translationService.when(TranslationService::getConfiguration).thenReturn(configuration);

            final LinkedHashMap<String, Object> placeholderValues = new LinkedHashMap<>();
            placeholderValues.put("text", "value");
            placeholderValues.put("number", 42);
            final Component richComponent = Component.text("Rich", NamedTextColor.LIGHT_PURPLE);
            placeholderValues.put("component", richComponent);
            final LocalDateTime now = LocalDateTime.of(2024, 1, 1, 12, 30);
            placeholderValues.put("timestamp", now);
            final Placeholder prebuilt = Placeholder.of("prebuilt", "custom");
            placeholderValues.put("direct", prebuilt);
            placeholderValues.put("missing", null);

            final ConsoleMessenger messenger = new ConsoleMessenger(this.logger);
            final TranslatedMessage translated = messenger.translate(key, placeholderValues);
            assertEquals("[Placeholders] Placeholder test", translated.asPlainText());

            final List<Placeholder> captured = captor.getValue();
            assertEquals(6, captured.size());

            final Placeholder textPlaceholder = captured.get(0);
            assertEquals("text", textPlaceholder.key());
            assertEquals("value", textPlaceholder.asText());
            assertEquals(Placeholder.PlaceholderType.TEXT, textPlaceholder.type());

            final Placeholder numberPlaceholder = captured.get(1);
            assertEquals("number", numberPlaceholder.key());
            assertEquals(42, numberPlaceholder.value());
            assertEquals(Placeholder.PlaceholderType.NUMBER, numberPlaceholder.type());

            final Placeholder componentPlaceholder = captured.get(2);
            assertEquals("component", componentPlaceholder.key());
            assertEquals(richComponent, componentPlaceholder.asComponent());
            assertEquals(Placeholder.PlaceholderType.RICH_TEXT, componentPlaceholder.type());

            final Placeholder datePlaceholder = captured.get(3);
            assertEquals("timestamp", datePlaceholder.key());
            assertEquals(now, datePlaceholder.value());
            assertEquals(Placeholder.PlaceholderType.DATE_TIME, datePlaceholder.type());

            final Placeholder prebuiltPlaceholder = captured.get(4);
            assertSame(prebuilt, prebuiltPlaceholder);

            final Placeholder nullPlaceholder = captured.get(5);
            assertEquals("missing", nullPlaceholder.key());
            assertEquals("", nullPlaceholder.asText());
            assertEquals(Placeholder.PlaceholderType.TEXT, nullPlaceholder.type());
        }
    }

    private static final class TestLogHandler extends Handler {

        private final List<LogRecord> records = new java.util.ArrayList<>();

        @Override
        public void publish(final LogRecord record) {
            if (record != null) {
                this.records.add(record);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        LogRecord singleRecord() {
            assertEquals(1, this.records.size(), "Expected exactly one log record");
            return this.records.getFirst();
        }

        LogRecord lastRecord() {
            if (this.records.isEmpty()) {
                return null;
            }
            return this.records.getLast();
        }

        void clear() {
            this.records.clear();
        }
    }
}
