package com.raindropcentral.commands;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.OfflinePlayerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.raindropcentral.commands.testplugin.command.TestCommandSection;
import de.jexcellence.evaluable.EnumInfo;
import de.jexcellence.evaluable.error.CommandError;
import de.jexcellence.evaluable.error.EErrorType;
import de.jexcellence.evaluable.error.ErrorContext;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BukkitCommandTest {

    private ServerMock server;
    private ConsoleCommandSenderMock consoleSender;
    private PlayerMock onlinePlayer;
    private RecordingCommandSection commandSection;
    private TestBukkitCommand command;
    private Logger logger;
    private TestLogHandler logHandler;

    @BeforeEach
    void setUp() throws Exception {
        this.server = MockBukkit.mock();
        MockBukkit.createMockPlugin();
        this.consoleSender = this.server.getConsoleSender();
        this.onlinePlayer = this.server.addPlayer("TestPlayer");
        this.commandSection = new RecordingCommandSection();
        this.command = new TestBukkitCommand(this.commandSection);
        this.logger = Logger.getLogger("RCommands");
        this.logHandler = new TestLogHandler();
        this.logger.addHandler(this.logHandler);
        Field cacheField = BukkitCommand.class.getDeclaredField("enumConstantsCache");
        cacheField.setAccessible(true);
        ((Map<?, ?>) cacheField.get(null)).clear();
    }

    @AfterEach
    void tearDown() {
        if (this.logger != null && this.logHandler != null) {
            this.logger.removeHandler(this.logHandler);
        }
        MockBukkit.unmock();
    }

    @Test
    void executeReturnsTrueOnSuccess() {
        String[] args = {"alpha", "beta"};

        boolean result = this.command.execute(this.onlinePlayer, "test", args);

        assertTrue(result);
        assertEquals(1, this.command.invocations.size());
        TestBukkitCommand.Invocation invocation = this.command.invocations.get(0);
        assertSame(this.onlinePlayer, invocation.sender);
        assertEquals("test", invocation.alias);
        assertEquals(List.of("alpha", "beta"), invocation.arguments);
    }

    @Test
    void executeReturnsFalseOnCommandErrorAndSendsMessage() {
        this.command.failNextInvocation(new CommandError(0, EErrorType.MISSING_ARGUMENT));

        boolean result = this.command.execute(this.consoleSender, "root", new String[0]);

        assertFalse(result);
        assertEquals("marker-MISSING_ARGUMENT", this.consoleSender.nextMessage());
        assertTrue(this.logHandler.records.isEmpty(), "Handled errors should not log warnings");
    }

    @Test
    void executeReturnsFalseOnUnexpectedExceptionAndLogsWarning() {
        RuntimeException failure = new RuntimeException("boom");
        this.command.crashNextInvocation(failure);

        boolean result = this.command.execute(this.consoleSender, "root", new String[0]);

        assertFalse(result);
        assertEquals("marker-INTERNAL", this.consoleSender.nextMessage());
        assertEquals(1, this.logHandler.records.size());
        LogRecord record = this.logHandler.records.get(0);
        assertEquals(Level.WARNING, record.getLevel());
        assertSame(failure, record.getThrown());
        assertTrue(record.getMessage().contains("Error occurred while executing the command"));
    }

    @Test
    void tabCompleteReturnsSuggestionsOnSuccess() {
        List<String> expected = List.of("one", "two");
        this.command.setTabCompletionResult(expected);

        List<String> suggestions = this.command.tabComplete(this.onlinePlayer, "test", new String[]{"par"});

        assertEquals(expected, suggestions);
        assertEquals(1, this.command.tabCompletions.size());
        TestBukkitCommand.TabInvocation invocation = this.command.tabCompletions.get(0);
        assertSame(this.onlinePlayer, invocation.sender);
        assertEquals("test", invocation.alias);
        assertEquals(List.of("par"), invocation.arguments);
    }

    @Test
    void tabCompleteReturnsEmptyListOnCommandError() {
        this.command.failNextTabCompletion(new CommandError(1, EErrorType.MALFORMED_INTEGER));

        List<String> suggestions = this.command.tabComplete(this.consoleSender, "test", new String[]{"bad"});

        assertSame(BukkitCommand.EMPTY_STRING_LIST, suggestions);
        assertEquals("marker-MALFORMED_INTEGER", this.consoleSender.nextMessage());
    }

    @Test
    void tabCompleteReturnsEmptyListOnException() {
        RuntimeException failure = new RuntimeException("tab");
        this.command.crashNextTabCompletion(failure);

        List<String> suggestions = this.command.tabComplete(this.consoleSender, "test", new String[]{"bad"});

        assertSame(BukkitCommand.EMPTY_STRING_LIST, suggestions);
        assertEquals("marker-INTERNAL", this.consoleSender.nextMessage());
        assertEquals(1, this.logHandler.records.size());
        assertSame(failure, this.logHandler.records.get(0).getThrown());
    }

    @Test
    void enumParameterResolvesValuesAndCachesLookups() throws Exception {
        String[] args = {TestEnum.FIRST.name().toLowerCase(Locale.ROOT)};

        TestEnum value = this.command.enumParameter(args, 0, TestEnum.class);

        assertEquals(TestEnum.FIRST, value);
        Field cacheField = BukkitCommand.class.getDeclaredField("enumConstantsCache");
        cacheField.setAccessible(true);
        Map<Class<? extends Enum<?>>, EnumInfo> cache = (Map<Class<? extends Enum<?>>, EnumInfo>) cacheField.get(null);
        EnumInfo cached = cache.get(TestEnum.class);
        assertNotNull(cached, "Enum info should be cached after first lookup");
        TestEnum again = this.command.enumParameter(args, 0, TestEnum.class);
        assertEquals(TestEnum.FIRST, again);
        assertSame(cached, cache.get(TestEnum.class), "Cached enum info should be reused");

        CommandError error = assertThrows(CommandError.class, () -> this.command.enumParameter(new String[]{"missing"}, 0, TestEnum.class));
        assertEquals(EErrorType.MALFORMED_ENUM, error.errorType);

        TestEnum fallback = TestEnum.SECOND;
        assertSame(fallback, this.command.enumParameterOrElse(new String[0], 1, TestEnum.class, fallback));
    }

    @Test
    void stringParameterReturnsValueAndThrowsWhenMissing() {
        assertEquals("value", this.command.stringParameter(new String[]{"value"}, 0));
        CommandError error = assertThrows(CommandError.class, () -> this.command.stringParameter(new String[]{}, 0));
        assertEquals(EErrorType.MISSING_ARGUMENT, error.errorType);
    }

    @Test
    void playerParameterResolvesOnlinePlayersAndSupportsFallback() {
        String[] args = {this.onlinePlayer.getName()};
        Player resolved = this.command.playerParameter(args, 0);
        assertSame(this.onlinePlayer, resolved);

        Player fallback = this.onlinePlayer;
        assertSame(fallback, this.command.playerParameterOrElse(new String[0], 0, fallback));

        CommandError error = assertThrows(CommandError.class, () -> this.command.playerParameter(new String[]{"Unknown"}, 0));
        assertEquals(EErrorType.PLAYER_NOT_ONLINE, error.errorType);
    }

    @Test
    void offlinePlayerParameterHonoursHistoryRequirementAndFallback() {
        OfflinePlayerMock known = new OfflinePlayerMock(this.server, UUID.randomUUID(), "Known");
        known.setHasPlayedBefore(true);
        this.server.setOfflinePlayers(known);
        OfflinePlayerMock fallback = new OfflinePlayerMock(this.server, UUID.randomUUID(), "Fallback");
        fallback.setHasPlayedBefore(true);

        String[] args = {known.getName()};
        assertSame(known, this.command.offlinePlayerParameter(args, 0, true));
        assertSame(fallback, this.command.offlinePlayerParameterOrElse(new String[0], 1, false, fallback));

        OfflinePlayerMock neverPlayed = new OfflinePlayerMock(this.server, UUID.randomUUID(), "Never");
        neverPlayed.setHasPlayedBefore(false);
        this.server.setOfflinePlayers(neverPlayed);
        CommandError error = assertThrows(CommandError.class, () -> this.command.offlinePlayerParameter(new String[]{neverPlayed.getName()}, 0, true));
        assertEquals(EErrorType.PLAYER_UNKNOWN, error.errorType);
    }

    @Test
    void uuidParameterParsesValuesAndFallback() {
        UUID value = UUID.randomUUID();
        assertEquals(value, this.command.uuidParameter(new String[]{value.toString()}, 0));
        assertEquals(value, this.command.uuidParameterOrElse(new String[0], 1, value));
        CommandError error = assertThrows(CommandError.class, () -> this.command.uuidParameter(new String[]{"bad"}, 0));
        assertEquals(EErrorType.MALFORMED_UUID, error.errorType);
    }

    @Test
    void numericParameterParsersHandleSuccessFailureAndFallbacks() {
        assertEquals(12, this.command.integerParameter(new String[]{"12"}, 0));
        assertEquals(12, this.command.integerParameterOrElse(new String[0], 1, 12));
        CommandError intError = assertThrows(CommandError.class, () -> this.command.integerParameter(new String[]{"x"}, 0));
        assertEquals(EErrorType.MALFORMED_INTEGER, intError.errorType);

        assertEquals(24L, this.command.longParameter(new String[]{"24"}, 0));
        assertEquals(24L, this.command.longParameterOrElse(new String[0], 1, 24L));
        CommandError longError = assertThrows(CommandError.class, () -> this.command.longParameter(new String[]{"x"}, 0));
        assertEquals(EErrorType.MALFORMED_LONG, longError.errorType);

        assertEquals(3.14D, this.command.doubleParameter(new String[]{"3.14"}, 0));
        assertEquals(3.14D, this.command.doubleParameterOrElse(new String[0], 1, 3.14D));
        CommandError doubleError = assertThrows(CommandError.class, () -> this.command.doubleParameter(new String[]{"x"}, 0));
        assertEquals(EErrorType.MALFORMED_DOUBLE, doubleError.errorType);

        assertEquals(2.5F, this.command.floatParameter(new String[]{"2.5"}, 0));
        assertEquals(2.5F, this.command.floatParameterOrElse(new String[0], 1, 2.5F));
        CommandError floatError = assertThrows(CommandError.class, () -> this.command.floatParameter(new String[]{"x"}, 0));
        assertEquals(EErrorType.MALFORMED_FLOAT, floatError.errorType);
    }

    @Test
    void resolveArgumentThrowsForNegativeAndMissingIndices() throws Exception {
        Method method = BukkitCommand.class.getDeclaredMethod("resolveArgument", String[].class, int.class);
        method.setAccessible(true);
        InvocationTargetException negative = assertThrows(InvocationTargetException.class, () -> method.invoke(this.command, new Object[]{new String[]{"value"}, -1}));
        assertInstanceOf(IllegalArgumentException.class, negative.getCause());

        InvocationTargetException missing = assertThrows(InvocationTargetException.class, () -> method.invoke(this.command, new Object[]{new String[]{"value"}, 1}));
        Throwable cause = missing.getCause();
        CommandError error = assertInstanceOf(CommandError.class, cause);
        assertEquals(EErrorType.MISSING_ARGUMENT, error.errorType);
    }

    @Test
    void sendComponentMessageSerializesToLegacyText() {
        this.command.failNextInvocation(new CommandError(0, EErrorType.NOT_A_PLAYER));

        this.command.execute(this.consoleSender, "alias", new String[]{"ignored"});

        assertEquals("marker-NOT_A_PLAYER", this.consoleSender.nextMessage());
    }

    @Test
    void handleErrorRoutesAllErrorTypesToCommandSection() {
        for (EErrorType type : EErrorType.values()) {
            ConsoleCommandSenderMock sender = this.server.getConsoleSender();
            CommandError error;
            if (type == EErrorType.MALFORMED_ENUM) {
                error = new CommandError(0, type, new EnumInfo(TestEnum.class));
            } else {
                error = new CommandError(0, type);
            }
            this.command.failNextInvocation(error);

            boolean result = this.command.execute(sender, "alias", new String[]{type.name()});

            assertFalse(result);
            assertEquals(this.commandSection.expectedMessage(type), sender.nextMessage());
        }
    }

    private enum TestEnum {
        FIRST,
        SECOND
    }

    private static final class RecordingCommandSection extends TestCommandSection {

        private final Map<EErrorType, String> markers = new EnumMap<>(EErrorType.class);
        private final String internalMarker;

        private RecordingCommandSection() {
            super("contextawarecommand");
            for (EErrorType type : EErrorType.values()) {
                this.markers.put(type, "marker-" + type.name());
            }
            this.internalMarker = "marker-INTERNAL";
        }

        String expectedMessage(EErrorType type) {
            return this.markers.get(type);
        }

        @Override
        public Component getInternalErrorMessage(ErrorContext context) {
            return Component.text(this.internalMarker);
        }

        @Override
        public Component getMalformedDoubleMessage(ErrorContext context) {
            return Component.text(this.markers.get(EErrorType.MALFORMED_DOUBLE));
        }

        @Override
        public Component getMalformedFloatMessage(ErrorContext context) {
            return Component.text(this.markers.get(EErrorType.MALFORMED_FLOAT));
        }

        @Override
        public Component getMalformedLongMessage(ErrorContext context) {
            return Component.text(this.markers.get(EErrorType.MALFORMED_LONG));
        }

        @Override
        public Component getMalformedIntegerMessage(ErrorContext context) {
            return Component.text(this.markers.get(EErrorType.MALFORMED_INTEGER));
        }

        @Override
        public Component getMalformedUuidMessage(ErrorContext context) {
            return Component.text(this.markers.get(EErrorType.MALFORMED_UUID));
        }

        @Override
        public Component getMalformedEnumMessage(ErrorContext context, EnumInfo enumInfo) {
            return Component.text(this.markers.get(EErrorType.MALFORMED_ENUM));
        }

        @Override
        public Component getMissingArgumentMessage(ErrorContext context) {
            return Component.text(this.markers.get(EErrorType.MISSING_ARGUMENT));
        }

        @Override
        public Component getNotAPlayerMessage(ErrorContext context) {
            return Component.text(this.markers.get(EErrorType.NOT_A_PLAYER));
        }

        @Override
        public Component getNotAConsoleMessage(ErrorContext context) {
            return Component.text(this.markers.get(EErrorType.NOT_A_CONSOLE));
        }

        @Override
        public Component getPlayerUnknownMessage(ErrorContext context) {
            return Component.text(this.markers.get(EErrorType.PLAYER_UNKNOWN));
        }

        @Override
        public Component getPlayerNotOnlineMessage(ErrorContext context) {
            return Component.text(this.markers.get(EErrorType.PLAYER_NOT_ONLINE));
        }
    }

    private static final class TestBukkitCommand extends BukkitCommand {

        private final List<Invocation> invocations = new CopyOnWriteArrayList<>();
        private final List<TabInvocation> tabCompletions = new CopyOnWriteArrayList<>();
        private volatile CommandError nextInvocationError;
        private volatile RuntimeException nextInvocationException;
        private volatile CommandError nextTabError;
        private volatile RuntimeException nextTabException;
        private volatile List<String> nextTabResult = List.of();

        private TestBukkitCommand(TestCommandSection section) {
            super(section);
        }

        void failNextInvocation(CommandError error) {
            this.nextInvocationError = error;
            this.nextInvocationException = null;
        }

        void crashNextInvocation(RuntimeException exception) {
            this.nextInvocationException = exception;
            this.nextInvocationError = null;
        }

        void setTabCompletionResult(List<String> result) {
            this.nextTabResult = result;
            this.nextTabError = null;
            this.nextTabException = null;
        }

        void failNextTabCompletion(CommandError error) {
            this.nextTabError = error;
            this.nextTabException = null;
        }

        void crashNextTabCompletion(RuntimeException exception) {
            this.nextTabException = exception;
            this.nextTabError = null;
        }

        @Override
        protected void onInvocation(CommandSender sender, String alias, String[] args) {
            this.invocations.add(new Invocation(sender, alias, args));
            if (this.nextInvocationError != null) {
                CommandError error = this.nextInvocationError;
                this.nextInvocationError = null;
                throw error;
            }
            if (this.nextInvocationException != null) {
                RuntimeException exception = this.nextInvocationException;
                this.nextInvocationException = null;
                throw exception;
            }
        }

        @Override
        protected List<String> onTabCompletion(CommandSender sender, String alias, String[] args) {
            this.tabCompletions.add(new TabInvocation(sender, alias, args));
            if (this.nextTabError != null) {
                CommandError error = this.nextTabError;
                this.nextTabError = null;
                throw error;
            }
            if (this.nextTabException != null) {
                RuntimeException exception = this.nextTabException;
                this.nextTabException = null;
                throw exception;
            }
            return this.nextTabResult;
        }

        <T extends Enum<?>> T enumParameter(String[] args, int index, Class<T> enumClass) {
            return super.enumParameter(args, index, enumClass);
        }

        <T extends Enum<?>> T enumParameterOrElse(String[] args, int index, Class<T> enumClass, T fallback) {
            return super.enumParameterOrElse(args, index, enumClass, fallback);
        }

        String stringParameter(String[] args, int index) {
            return super.stringParameter(args, index);
        }

        Player playerParameter(String[] args, int index) {
            return super.playerParameter(args, index);
        }

        Player playerParameterOrElse(String[] args, int index, Player fallback) {
            return super.playerParameterOrElse(args, index, fallback);
        }

        @NotNull
        org.bukkit.OfflinePlayer offlinePlayerParameter(String[] args, int index, boolean hasToHavePlayed) {
            return super.offlinePlayerParameter(args, index, hasToHavePlayed);
        }

        org.bukkit.OfflinePlayer offlinePlayerParameterOrElse(String[] args, int index, boolean hasToHavePlayed, org.bukkit.OfflinePlayer fallback) {
            return super.offlinePlayerParameterOrElse(args, index, hasToHavePlayed, fallback);
        }

        UUID uuidParameter(String[] args, int index) {
            return super.uuidParameter(args, index);
        }

        UUID uuidParameterOrElse(String[] args, int index, UUID fallback) {
            return super.uuidParameterOrElse(args, index, fallback);
        }

        Integer integerParameter(String[] args, int index) {
            return super.integerParameter(args, index);
        }

        Integer integerParameterOrElse(String[] args, int index, Integer fallback) {
            return super.integerParameterOrElse(args, index, fallback);
        }

        Long longParameter(String[] args, int index) {
            return super.longParameter(args, index);
        }

        Long longParameterOrElse(String[] args, int index, Long fallback) {
            return super.longParameterOrElse(args, index, fallback);
        }

        Double doubleParameter(String[] args, int index) {
            return super.doubleParameter(args, index);
        }

        Double doubleParameterOrElse(String[] args, int index, Double fallback) {
            return super.doubleParameterOrElse(args, index, fallback);
        }

        Float floatParameter(String[] args, int index) {
            return super.floatParameter(args, index);
        }

        Float floatParameterOrElse(String[] args, int index, Float fallback) {
            return super.floatParameterOrElse(args, index, fallback);
        }

        private record Invocation(CommandSender sender, String alias, List<String> arguments) {
            private Invocation(CommandSender sender, String alias, String[] args) {
                this(sender, alias, List.copyOf(Arrays.asList(args)));
            }
        }

        private record TabInvocation(CommandSender sender, String alias, List<String> arguments) {
            private TabInvocation(CommandSender sender, String alias, String[] args) {
                this(sender, alias, List.copyOf(Arrays.asList(args)));
            }
        }
    }

    private static final class TestLogHandler extends Handler {

        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            this.records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
