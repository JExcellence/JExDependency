package de.jexcellence.economy.service;

import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.CurrencyLog;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.database.repository.CurrencyLogRepository;
import de.jexcellence.economy.type.EChangeType;
import de.jexcellence.economy.type.ELogLevel;
import de.jexcellence.economy.type.ELogType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyLogServiceTest {

    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    @Mock
    private CurrencyLogRepository logRepository;

    private CurrencyLogService service;

    @BeforeEach
    void setUp() {
        service = new CurrencyLogService(logRepository, DIRECT_EXECUTOR);
    }

    @Test
    void logBalanceChangeSuccessPopulatesTransactionLog() {
        Currency currency = new Currency("gold");
        User user = new User(UUID.randomUUID(), "PlayerOne");
        when(logRepository.create(any(CurrencyLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompletableFuture<CurrencyLog> future = service.logBalanceChange(
            user,
            currency,
            EChangeType.DEPOSIT,
            10.0,
            15.0,
            5.0,
            "Quest reward",
            null,
            true,
            null
        );

        CurrencyLog savedLog = future.join();
        assertNotNull(savedLog);

        ArgumentCaptor<CurrencyLog> captor = ArgumentCaptor.forClass(CurrencyLog.class);
        verify(logRepository).create(captor.capture());
        CurrencyLog captured = captor.getValue();

        assertEquals(user.getUniqueId(), captured.getPlayerUuid());
        assertEquals(user.getPlayerName(), captured.getPlayerName());
        assertSame(currency, captured.getCurrency());
        assertEquals(ELogType.TRANSACTION, captured.getLogType());
        assertEquals(ELogLevel.INFO, captured.getLogLevel());
        assertEquals(EChangeType.DEPOSIT, captured.getOperationType());
        assertEquals(10.0, captured.getOldBalance());
        assertEquals(15.0, captured.getNewBalance());
        assertEquals(5.0, captured.getAmount());
        assertEquals("Quest reward", captured.getReason());
        assertTrue(captured.isSuccess());
        assertEquals(
            "Successfully 5.00 deposit to/from gold account",
            captured.getDescription()
        );
        assertSame(captured, savedLog);
        verifyNoMoreInteractions(logRepository);
    }

    @Test
    void logBalanceChangeFailureCapturesErrorDetails() {
        Currency currency = new Currency("silver");
        User user = new User(UUID.randomUUID(), "PlayerTwo");
        Player initiator = mock(Player.class);
        UUID initiatorUuid = UUID.randomUUID();
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 25565);

        when(initiator.getUniqueId()).thenReturn(initiatorUuid);
        when(initiator.getName()).thenReturn("Admin");
        when(initiator.getAddress()).thenReturn(address);
        when(logRepository.create(any(CurrencyLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CurrencyLog savedLog = service.logBalanceChange(
            user,
            currency,
            EChangeType.WITHDRAW,
            50.0,
            25.0,
            25.0,
            "Purchase",
            initiator,
            false,
            "Insufficient permissions"
        ).join();

        ArgumentCaptor<CurrencyLog> captor = ArgumentCaptor.forClass(CurrencyLog.class);
        verify(logRepository).create(captor.capture());
        CurrencyLog captured = captor.getValue();

        assertFalse(captured.isSuccess());
        assertEquals("Insufficient permissions", captured.getErrorMessage());
        assertEquals(ELogLevel.ERROR, captured.getLogLevel());
        assertEquals(initiatorUuid, captured.getInitiatorUuid());
        assertEquals("Admin", captured.getInitiatorName());
        assertEquals("127.0.0.1", captured.getIpAddress());
        assertSame(savedLog, captured);
        verifyNoMoreInteractions(logRepository);
    }

    @Test
    void logCurrencyManagementSuccessIncludesDetails() {
        Currency currency = new Currency("credits");
        Player initiator = mock(Player.class);
        UUID initiatorUuid = UUID.randomUUID();
        InetSocketAddress address = new InetSocketAddress("192.168.1.5", 2345);

        when(initiator.getUniqueId()).thenReturn(initiatorUuid);
        when(initiator.getName()).thenReturn("Manager");
        when(initiator.getAddress()).thenReturn(address);
        when(logRepository.create(any(CurrencyLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CurrencyLog result = service.logCurrencyManagement(
            currency,
            "created",
            initiator,
            true,
            "Initial creation",
            null
        ).join();

        ArgumentCaptor<CurrencyLog> captor = ArgumentCaptor.forClass(CurrencyLog.class);
        verify(logRepository).create(captor.capture());
        CurrencyLog captured = captor.getValue();

        assertEquals(ELogType.MANAGEMENT, captured.getLogType());
        assertEquals(ELogLevel.INFO, captured.getLogLevel());
        assertTrue(captured.isSuccess());
        assertEquals("Initial creation", captured.getDetails());
        assertEquals(initiatorUuid, captured.getInitiatorUuid());
        assertEquals("Manager", captured.getInitiatorName());
        assertEquals("192.168.1.5", captured.getIpAddress());
        assertSame(result, captured);
        verifyNoMoreInteractions(logRepository);
    }

    @Test
    void logCurrencyManagementFailureRecordsError() {
        Currency currency = new Currency("credits");
        when(logRepository.create(any(CurrencyLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CurrencyLog result = service.logCurrencyManagement(
            currency,
            "deleted",
            null,
            false,
            "Cleanup",
            "Database locked"
        ).join();

        ArgumentCaptor<CurrencyLog> captor = ArgumentCaptor.forClass(CurrencyLog.class);
        verify(logRepository).create(captor.capture());
        CurrencyLog captured = captor.getValue();

        assertFalse(captured.isSuccess());
        assertEquals(ELogLevel.ERROR, captured.getLogLevel());
        assertEquals("Database locked", captured.getErrorMessage());
        assertEquals("Cleanup", captured.getDetails());
        assertSame(result, captured);
        verifyNoMoreInteractions(logRepository);
    }

    @Test
    void logSystemOperationSuccessUsesProvidedLevel() {
        when(logRepository.create(any(CurrencyLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CurrencyLog log = service.logSystemOperation(
            "Cache warmed",
            ELogLevel.DEBUG,
            "Took 150ms",
            null
        ).join();

        ArgumentCaptor<CurrencyLog> captor = ArgumentCaptor.forClass(CurrencyLog.class);
        verify(logRepository).create(captor.capture());
        CurrencyLog captured = captor.getValue();

        assertTrue(captured.isSuccess());
        assertEquals(ELogType.SYSTEM, captured.getLogType());
        assertEquals(ELogLevel.DEBUG, captured.getLogLevel());
        assertEquals("Took 150ms", captured.getDetails());
        assertSame(log, captured);
        verifyNoMoreInteractions(logRepository);
    }

    @Test
    void logSystemOperationFailureMarksUnsuccessful() {
        when(logRepository.create(any(CurrencyLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CurrencyLog log = service.logSystemOperation(
            "Cache refresh",
            ELogLevel.ERROR,
            null,
            "Timeout"
        ).join();

        ArgumentCaptor<CurrencyLog> captor = ArgumentCaptor.forClass(CurrencyLog.class);
        verify(logRepository).create(captor.capture());
        CurrencyLog captured = captor.getValue();

        assertFalse(captured.isSuccess());
        assertEquals("Timeout", captured.getErrorMessage());
        assertEquals(ELogLevel.ERROR, captured.getLogLevel());
        assertSame(log, captured);
        verifyNoMoreInteractions(logRepository);
    }

    @Test
    void logAuditEventSuccessRecordsPlayerInformation() {
        Player player = mock(Player.class);
        UUID playerUuid = UUID.randomUUID();
        InetSocketAddress address = new InetSocketAddress("10.0.0.2", 1234);

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(player.getName()).thenReturn("Moderator");
        when(player.getAddress()).thenReturn(address);
        when(logRepository.create(any(CurrencyLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CurrencyLog log = service.logAuditEvent(
            player,
            "reviewed logs",
            "player123",
            true,
            "No issues"
        ).join();

        ArgumentCaptor<CurrencyLog> captor = ArgumentCaptor.forClass(CurrencyLog.class);
        verify(logRepository).create(captor.capture());
        CurrencyLog captured = captor.getValue();

        assertEquals(ELogType.AUDIT, captured.getLogType());
        assertEquals(ELogLevel.INFO, captured.getLogLevel());
        assertTrue(captured.isSuccess());
        assertEquals(playerUuid, captured.getPlayerUuid());
        assertEquals("Moderator", captured.getPlayerName());
        assertEquals("10.0.0.2", captured.getIpAddress());
        assertEquals("No issues", captured.getDetails());
        assertSame(log, captured);
        verifyNoMoreInteractions(logRepository);
    }

    @Test
    void logAuditEventFailureSetsWarningLevel() {
        Player player = mock(Player.class);
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(player.getName()).thenReturn("Moderator");
        when(player.getAddress()).thenReturn(null);
        when(logRepository.create(any(CurrencyLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CurrencyLog log = service.logAuditEvent(
            player,
            "revoked access",
            "player456",
            false,
            null
        ).join();

        ArgumentCaptor<CurrencyLog> captor = ArgumentCaptor.forClass(CurrencyLog.class);
        verify(logRepository).create(captor.capture());
        CurrencyLog captured = captor.getValue();

        assertEquals(ELogLevel.WARNING, captured.getLogLevel());
        assertFalse(captured.isSuccess());
        assertEquals(playerUuid, captured.getPlayerUuid());
        assertSame(log, captured);
        verifyNoMoreInteractions(logRepository);
    }

    @Test
    void getPlayerTransactionHistorySortsAndLimits() {
        UUID playerUuid = UUID.randomUUID();
        Currency currency = new Currency("emerald");
        CurrencyLog first = buildTransactionLog(playerUuid, "Alpha", currency, EChangeType.DEPOSIT, LocalDateTime.now().minusMinutes(2));
        CurrencyLog second = buildTransactionLog(playerUuid, "Alpha", currency, EChangeType.WITHDRAW, LocalDateTime.now());
        CurrencyLog third = buildTransactionLog(playerUuid, "Alpha", currency, EChangeType.DEPOSIT, LocalDateTime.now().minusMinutes(1));

        when(logRepository.findListByAttributes(any(Map.class))).thenReturn(List.of(first, second, third));

        List<CurrencyLog> result = service.getPlayerTransactionHistory(playerUuid, 2).join();

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(logRepository).findListByAttributes(captor.capture());
        Map<String, Object> attributes = captor.getValue();
        assertEquals(playerUuid, attributes.get("playerUuid"));
        assertEquals(ELogType.TRANSACTION, attributes.get("logType"));
        assertEquals(true, attributes.get("success"));
        assertEquals(List.of(second, third), result);
    }

    @Test
    void getCurrencyTransactionHistorySortsAndLimits() {
        Currency currency = new Currency("ruby");
        CurrencyLog a = buildTransactionLog(UUID.randomUUID(), "User", currency, EChangeType.DEPOSIT, LocalDateTime.now());
        CurrencyLog b = buildTransactionLog(UUID.randomUUID(), "User", currency, EChangeType.WITHDRAW, LocalDateTime.now().minusMinutes(5));
        when(logRepository.findListByAttributes(any(Map.class))).thenReturn(List.of(a, b));

        List<CurrencyLog> result = service.getCurrencyTransactionHistory(1L, 1).join();

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(logRepository).findListByAttributes(captor.capture());
        Map<String, Object> attributes = captor.getValue();
        assertEquals(1L, attributes.get("currency.id"));
        assertEquals(ELogType.TRANSACTION, attributes.get("logType"));
        assertEquals(true, attributes.get("success"));
        assertEquals(List.of(a), result);
    }

    @Test
    void getRecentErrorsMergesDistinctLogs() {
        CurrencyLog errorLog = assignId(createLogWithLevel(ELogLevel.ERROR, LocalDateTime.now().minusMinutes(1)), 1L);
        CurrencyLog criticalLog = assignId(createLogWithLevel(ELogLevel.CRITICAL, LocalDateTime.now()), 2L);
        CurrencyLog failedLog = assignId(createLogWithSuccess(false, LocalDateTime.now().minusMinutes(2)), 3L);

        when(logRepository.findListByAttributes(eq(Map.of("logLevel", ELogLevel.ERROR)))).thenReturn(List.of(errorLog));
        when(logRepository.findListByAttributes(eq(Map.of("logLevel", ELogLevel.CRITICAL)))).thenReturn(List.of(criticalLog));
        when(logRepository.findListByAttributes(eq(Map.of("success", false)))).thenReturn(List.of(errorLog, failedLog));

        List<CurrencyLog> result = service.getRecentErrors(2).join();

        assertEquals(List.of(criticalLog, errorLog), result);
        verify(logRepository, times(3)).findListByAttributes(any(Map.class));
    }

    @Test
    void getLogsByCriteriaBuildsAttributeMap() {
        CurrencyLog log = createLogWithLevel(ELogLevel.INFO, LocalDateTime.now());
        when(logRepository.findListByAttributes(any(Map.class))).thenReturn(List.of(log));

        UUID playerUuid = UUID.randomUUID();
        List<CurrencyLog> result = service.getLogsByCriteria(
            playerUuid,
            3L,
            ELogType.SYSTEM,
            ELogLevel.INFO,
            Boolean.TRUE,
            10
        ).join();

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(logRepository).findListByAttributes(captor.capture());
        Map<String, Object> attributes = captor.getValue();

        assertEquals(playerUuid, attributes.get("playerUuid"));
        assertEquals(3L, attributes.get("currency.id"));
        assertEquals(ELogType.SYSTEM, attributes.get("logType"));
        assertEquals(ELogLevel.INFO, attributes.get("logLevel"));
        assertEquals(true, attributes.get("success"));
        assertEquals(List.of(log), result);
    }

    @Test
    void getLogsByCriteriaOmitsNullFilters() {
        CurrencyLog log = createLogWithLevel(ELogLevel.INFO, LocalDateTime.now());
        when(logRepository.findListByAttributes(any(Map.class))).thenReturn(List.of(log));

        List<CurrencyLog> result = service.getLogsByCriteria(
            null,
            null,
            null,
            null,
            null,
            5
        ).join();

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(logRepository).findListByAttributes(captor.capture());
        Map<String, Object> attributes = captor.getValue();

        assertTrue(attributes.isEmpty());
        assertEquals(List.of(log), result);
    }

    @Test
    void getLogsByOperationTypeFiltersTransactions() {
        CurrencyLog log = createLogWithLevel(ELogLevel.INFO, LocalDateTime.now());
        when(logRepository.findListByAttributes(any(Map.class))).thenReturn(List.of(log));

        List<CurrencyLog> result = service.getLogsByOperationType(EChangeType.SET, 1).join();

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(logRepository).findListByAttributes(captor.capture());
        Map<String, Object> attributes = captor.getValue();

        assertEquals(EChangeType.SET, attributes.get("operationType"));
        assertEquals(ELogType.TRANSACTION, attributes.get("logType"));
        assertEquals(List.of(log), result);
    }

    @Test
    void getCurrencyManagementLogsFiltersByCurrency() {
        CurrencyLog log = createLogWithLevel(ELogLevel.INFO, LocalDateTime.now());
        when(logRepository.findListByAttributes(any(Map.class))).thenReturn(List.of(log));

        List<CurrencyLog> result = service.getCurrencyManagementLogs(9L, 1).join();

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(logRepository).findListByAttributes(captor.capture());
        Map<String, Object> attributes = captor.getValue();

        assertEquals(9L, attributes.get("currency.id"));
        assertEquals(ELogType.MANAGEMENT, attributes.get("logType"));
        assertEquals(List.of(log), result);
    }

    @Test
    void getPlayerAuditLogsFiltersByPlayer() {
        CurrencyLog log = createLogWithLevel(ELogLevel.INFO, LocalDateTime.now());
        when(logRepository.findListByAttributes(any(Map.class))).thenReturn(List.of(log));

        UUID playerUuid = UUID.randomUUID();
        List<CurrencyLog> result = service.getPlayerAuditLogs(playerUuid, 2).join();

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(logRepository).findListByAttributes(captor.capture());
        Map<String, Object> attributes = captor.getValue();

        assertEquals(playerUuid, attributes.get("playerUuid"));
        assertEquals(ELogType.AUDIT, attributes.get("logType"));
        assertEquals(List.of(log), result);
    }

    @Test
    void getSystemLogsUsesOptionalLevel() {
        CurrencyLog log = createLogWithLevel(ELogLevel.ERROR, LocalDateTime.now());
        when(logRepository.findListByAttributes(any(Map.class))).thenReturn(List.of(log));

        List<CurrencyLog> resultWithLevel = service.getSystemLogs(ELogLevel.WARNING, 1).join();
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(logRepository).findListByAttributes(captor.capture());
        Map<String, Object> attributesWithLevel = captor.getValue();
        assertEquals(ELogType.SYSTEM, attributesWithLevel.get("logType"));
        assertEquals(ELogLevel.WARNING, attributesWithLevel.get("logLevel"));
        assertEquals(List.of(log), resultWithLevel);

        when(logRepository.findListByAttributes(Map.of("logType", ELogType.SYSTEM))).thenReturn(List.of(log));

        List<CurrencyLog> resultWithoutLevel = service.getSystemLogs(null, 1).join();
        verify(logRepository).findListByAttributes(Map.of("logType", ELogType.SYSTEM));
        assertEquals(List.of(log), resultWithoutLevel);
    }

    @Test
    void getBasicCurrencyStatsAggregatesValues() {
        Currency currency = new Currency("onyx");
        UUID playerUuid = UUID.randomUUID();
        CurrencyLog deposit = buildTransactionLog(playerUuid, "PlayerA", currency, EChangeType.DEPOSIT, LocalDateTime.now());
        CurrencyLog withdraw = buildTransactionLog(playerUuid, "PlayerA", currency, EChangeType.WITHDRAW, LocalDateTime.now().minusMinutes(1));
        CurrencyLog otherDeposit = buildTransactionLog(UUID.randomUUID(), "PlayerB", currency, EChangeType.DEPOSIT, LocalDateTime.now().minusMinutes(2));

        when(logRepository.findListByAttributes(Map.of(
            "currency.id", 7L,
            "logType", ELogType.TRANSACTION,
            "success", true
        ))).thenReturn(List.of(deposit, withdraw, otherDeposit));

        CurrencyLogService.BasicCurrencyStats stats = service.getBasicCurrencyStats(7L).join();

        assertEquals(7L, stats.getCurrencyId());
        assertEquals(3, stats.getTotalTransactions());
        assertEquals(2, stats.getTotalDeposits());
        assertEquals(1, stats.getTotalWithdrawals());
        assertEquals(2, stats.getUniqueUsers());
    }

    private CurrencyLog buildTransactionLog(
        UUID playerUuid,
        String playerName,
        Currency currency,
        EChangeType changeType,
        LocalDateTime timestamp
    ) {
        CurrencyLog log = new CurrencyLog(
            playerUuid,
            playerName,
            currency,
            changeType,
            0.0,
            0.0,
            0.0,
            "desc",
            null
        );
        log.setTimestamp(timestamp);
        return log;
    }

    private CurrencyLog createLogWithLevel(ELogLevel level, LocalDateTime timestamp) {
        CurrencyLog log = new CurrencyLog(ELogType.SYSTEM, level, "description");
        log.setTimestamp(timestamp);
        return log;
    }

    private CurrencyLog createLogWithSuccess(boolean success, LocalDateTime timestamp) {
        CurrencyLog log = new CurrencyLog(ELogType.SYSTEM, ELogLevel.ERROR, "description");
        log.setTimestamp(timestamp);
        log.setSuccess(success);
        return log;
    }

    private CurrencyLog assignId(CurrencyLog log, long id) {
        try {
            Field idField = findIdField(log.getClass());
            idField.setAccessible(true);
            idField.set(log, id);
            return log;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to assign id for test setup", exception);
        }
    }

    private Field findIdField(Class<?> type) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField("id");
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException("id");
    }
}
