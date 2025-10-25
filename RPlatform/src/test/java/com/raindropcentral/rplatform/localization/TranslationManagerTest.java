package com.raindropcentral.rplatform.localization;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import de.jexcellence.jextranslate.api.LocaleResolver;
import de.jexcellence.jextranslate.api.MessageFormatter;
import de.jexcellence.jextranslate.api.TranslationRepository;
import de.jexcellence.jextranslate.api.TranslationService;
import de.jexcellence.jextranslate.impl.MiniMessageFormatter;
import de.jexcellence.jextranslate.impl.YamlTranslationRepository;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationManagerTest {

    private ServerMock server;
    private JavaPlugin plugin;
    private TestLogHandler logHandler;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.load(TranslationTestPlugin.class);
        this.logHandler = new TestLogHandler();
        this.plugin.getLogger().addHandler(this.logHandler);
    }

    @AfterEach
    void tearDown() {
        this.plugin.getLogger().removeHandler(this.logHandler);
        MockBukkit.unmock();
    }

    @Test
    void initializeConfiguresTranslationServiceAndLogsLocaleCount() {
        final TranslationRepository repository = Mockito.mock(TranslationRepository.class);
        Mockito.when(repository.getAvailableLocales()).thenReturn(Set.of(Locale.ENGLISH, Locale.GERMAN));

        try (MockedStatic<YamlTranslationRepository> repositoryStatic = Mockito.mockStatic(YamlTranslationRepository.class);
             MockedStatic<TranslationService> translationService = Mockito.mockStatic(TranslationService.class)) {
            repositoryStatic.when(() -> YamlTranslationRepository.create(Mockito.any(), Mockito.eq(Locale.ENGLISH)))
                    .thenReturn(repository);

            final AtomicReference<TranslationService.ServiceConfiguration> capturedConfiguration = new AtomicReference<>();
            translationService.when(() -> TranslationService.configure(Mockito.any())).thenAnswer(invocation -> {
                TranslationService.ServiceConfiguration configuration = invocation.getArgument(0);
                capturedConfiguration.set(configuration);
                return null;
            });

            final TranslationManager manager = new TranslationManager(this.plugin);
            manager.initialize();

            final TranslationService.ServiceConfiguration configuration = capturedConfiguration.get();
            assertNotNull(configuration, "TranslationService.configure should receive a configuration");
            assertSame(repository, configuration.repository(), "Repository supplied during initialization should originate from the stub");
            assertEquals(Locale.ENGLISH, configuration.localeResolver().getDefaultLocale(),
                    "Auto-detected locale resolver should inherit the manager default");
            assertTrue(configuration.formatter() instanceof MiniMessageFormatter,
                    "Translation manager should configure a MiniMessage formatter");
            assertTrue(this.logHandler.containsMessage("Translation service initialized with 2 locales"));
        }
    }

    @Test
    void setPlayerLocaleClearsCacheWhenResolverAcceptsChange() {
        final TranslationRepository repository = Mockito.mock(TranslationRepository.class);
        final MessageFormatter formatter = Mockito.mock(MessageFormatter.class);
        final LocaleResolver resolver = Mockito.mock(LocaleResolver.class);

        try (MockedStatic<YamlTranslationRepository> repositoryStatic = Mockito.mockStatic(YamlTranslationRepository.class);
             MockedStatic<TranslationService> translationService = Mockito.mockStatic(TranslationService.class)) {
            repositoryStatic.when(() -> YamlTranslationRepository.create(Mockito.any(), Mockito.eq(Locale.ENGLISH)))
                    .thenReturn(repository);

            final TranslationManager manager = new TranslationManager(this.plugin);

            final TranslationService.ServiceConfiguration configuration =
                    new TranslationService.ServiceConfiguration(repository, formatter, resolver);
            translationService.when(TranslationService::getConfiguration).thenReturn(configuration);

            final Player player = this.server.addPlayer();
            Mockito.when(resolver.setPlayerLocale(player, Locale.GERMAN)).thenReturn(true);

            assertTrue(manager.setPlayerLocale(player, Locale.GERMAN));
            translationService.verify(() -> TranslationService.clearLocaleCache(player));
        }
    }

    @Test
    void getPlayerLocaleReturnsResolverValueOrDefault() {
        final TranslationRepository repository = Mockito.mock(TranslationRepository.class);
        final MessageFormatter formatter = Mockito.mock(MessageFormatter.class);
        final LocaleResolver resolver = Mockito.mock(LocaleResolver.class);

        try (MockedStatic<YamlTranslationRepository> repositoryStatic = Mockito.mockStatic(YamlTranslationRepository.class);
             MockedStatic<TranslationService> translationService = Mockito.mockStatic(TranslationService.class)) {
            repositoryStatic.when(() -> YamlTranslationRepository.create(Mockito.any(), Mockito.eq(Locale.JAPANESE)))
                    .thenReturn(repository);

            final TranslationManager manager = new TranslationManager(this.plugin, Locale.JAPANESE);

            final TranslationService.ServiceConfiguration configuration =
                    new TranslationService.ServiceConfiguration(repository, formatter, resolver);
            translationService.when(TranslationService::getConfiguration).thenReturn(configuration);

            final Player player = this.server.addPlayer();
            Mockito.when(resolver.resolveLocale(player)).thenReturn(Optional.of(Locale.KOREAN));

            assertSame(Locale.KOREAN, manager.getPlayerLocale(player));

            Mockito.when(resolver.resolveLocale(player)).thenReturn(Optional.empty());

            assertSame(Locale.JAPANESE, manager.getPlayerLocale(player));
        }
    }

    @Test
    void initializeThrowsWhenServiceAlreadyConfigured() {
        final TranslationRepository repository = Mockito.mock(TranslationRepository.class);
        Mockito.when(repository.getAvailableLocales()).thenReturn(Set.of(Locale.ENGLISH));

        try (MockedStatic<YamlTranslationRepository> repositoryStatic = Mockito.mockStatic(YamlTranslationRepository.class);
             MockedStatic<TranslationService> translationService = Mockito.mockStatic(TranslationService.class)) {
            repositoryStatic.when(() -> YamlTranslationRepository.create(Mockito.any(), Mockito.eq(Locale.ENGLISH)))
                    .thenReturn(repository);

            final AtomicInteger configureInvocations = new AtomicInteger();
            translationService.when(() -> TranslationService.configure(Mockito.any())).thenAnswer(invocation -> {
                if (configureInvocations.getAndIncrement() > 0) {
                    throw new IllegalStateException("Already configured");
                }
                return null;
            });

            final TranslationManager manager = new TranslationManager(this.plugin);
            manager.initialize();

            final IllegalStateException exception = assertThrows(IllegalStateException.class, manager::initialize);
            assertEquals("Already configured", exception.getMessage());
        }
    }

    @Test
    void constructorPropagatesRepositoryCreationFailure() {
        try (MockedStatic<YamlTranslationRepository> repositoryStatic = Mockito.mockStatic(YamlTranslationRepository.class)) {
            repositoryStatic.when(() -> YamlTranslationRepository.create(Mockito.any(), Mockito.eq(Locale.ENGLISH)))
                    .thenThrow(new IllegalStateException("missing translation resources"));

            final IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> new TranslationManager(this.plugin));
            assertEquals("missing translation resources", exception.getMessage());
        }
    }

    public static class TranslationTestPlugin extends JavaPlugin {

        private Path dataFolder;

        @Override
        public @NotNull File getDataFolder() {
            ensureDataFolder();
            return this.dataFolder.toFile();
        }

        @Override
        public void onLoad() {
            ensureDataFolder();
        }

        @Override
        public void onDisable() {
            if (this.dataFolder == null) {
                return;
            }

            try {
                Files.walkFileTree(this.dataFolder, new SimpleFileVisitor<>() {
                    @Override
                    public java.nio.file.FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.deleteIfExists(file);
                        return java.nio.file.FileVisitResult.CONTINUE;
                    }

                    @Override
                    public java.nio.file.FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.deleteIfExists(dir);
                        return java.nio.file.FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException ignored) {
            }
        }

        private void ensureDataFolder() {
            if (this.dataFolder != null) {
                return;
            }

            try {
                this.dataFolder = Files.createTempDirectory("translation-manager-test");
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create plugin data folder", e);
            }
        }
    }

    private static final class TestLogHandler extends Handler {

        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(final LogRecord record) {
            if (record != null && record.getMessage() != null) {
                this.messages.add(record.getMessage());
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        boolean containsMessage(final String messageFragment) {
            return this.messages.stream().anyMatch(message -> message.contains(messageFragment));
        }
    }
}
