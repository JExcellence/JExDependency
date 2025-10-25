package de.jexcellence.economy.database.entity;

import de.jexcellence.economy.type.EChangeType;
import de.jexcellence.economy.type.ELogLevel;
import de.jexcellence.economy.type.ELogType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class CurrencyLogTest {

    @Test
    void settersExposeCompleteStateIncludingInitiatorAndErrors() throws Exception {
        Currency currency = new Currency("emeralds");
        CurrencyLog log = new CurrencyLog(ELogType.SYSTEM, ELogLevel.WARNING, "Initial event");

        LocalDateTime timestamp = LocalDateTime.of(2024, 5, 10, 12, 45, 30);
        UUID playerUuid = UUID.randomUUID();
        UUID initiatorUuid = UUID.randomUUID();

        log.setTimestamp(timestamp);
        log.setLogType(ELogType.AUDIT);
        log.setLogLevel(ELogLevel.ERROR);
        log.setPlayerUuid(playerUuid);
        log.setPlayerName("PlayerOne");
        log.setInitiatorUuid(initiatorUuid);
        log.setInitiatorName("AdminUser");
        log.setCurrency(currency);
        log.setOperationType(EChangeType.SET);
        log.setOldBalance(125.5);
        log.setNewBalance(150.75);
        log.setAmount(25.25);
        log.setDescription("Balance adjusted");
        log.setDetails("{\"source\":\"console\"}");
        log.setReason("Administrative correction");
        log.setSuccess(true);
        log.setErrorMessage("Validation failed");
        log.setIpAddress("10.0.0.5");
        log.setMetadata("{\"traceId\":\"abc-123\"}");

        assertEquals(timestamp, log.getTimestamp());
        assertEquals(ELogType.AUDIT, log.getLogType());
        assertEquals(ELogLevel.ERROR, log.getLogLevel());
        assertEquals(playerUuid, log.getPlayerUuid());
        assertEquals("PlayerOne", log.getPlayerName());
        assertEquals(initiatorUuid, log.getInitiatorUuid());
        assertEquals("AdminUser", log.getInitiatorName());
        assertEquals(currency, log.getCurrency());
        assertEquals(EChangeType.SET, log.getOperationType());
        assertEquals(125.5, log.getOldBalance());
        assertEquals(150.75, log.getNewBalance());
        assertEquals(25.25, log.getAmount());
        assertEquals("Balance adjusted", log.getDescription());
        assertEquals("{\"source\":\"console\"}", log.getDetails());
        assertEquals("Administrative correction", log.getReason());
        assertFalse(log.isSuccess());
        assertEquals("Validation failed", log.getErrorMessage());
        assertEquals("10.0.0.5", log.getIpAddress());
        assertEquals("{\"traceId\":\"abc-123\"}", log.getMetadata());
    }

    @Test
    void convenienceMethodsCoverSuccessChangeAmountAndLinking() throws Exception {
        Currency currency = new Currency("tokens");
        setEntityId(currency, 42L);

        UUID playerUuid = UUID.randomUUID();
        CurrencyLog log = new CurrencyLog(
            playerUuid,
            "PlayerTwo",
            currency,
            EChangeType.DEPOSIT,
            200.0,
            260.0,
            60.0,
            "Deposit complete",
            "Quest reward"
        );
        setEntityId(log, 99L);

        UUID initiatorUuid = UUID.randomUUID();
        log.setInitiatorUuid(initiatorUuid);

        assertEquals(60.0, log.getChangeAmount());
        assertTrue(log.wasSuccessful());
        assertTrue(log.involvesPlayer(playerUuid));
        assertTrue(log.involvesPlayer(initiatorUuid));
        assertFalse(log.involvesPlayer(UUID.randomUUID()));
        assertTrue(log.involvesCurrency(42L));
        assertFalse(log.involvesCurrency(7L));

        String description = log.toString();
        assertNotNull(description);
        assertTrue(description.contains("CurrencyLog"));
        assertTrue(description.contains("type=TRANSACTION"));
        assertTrue(description.contains("level=INFO"));
        assertTrue(description.contains("operation=DEPOSIT"));
        assertTrue(description.contains("description='Deposit complete'"));
        assertTrue(description.contains("success=true"));

        log.setSuccess(false);
        assertFalse(log.wasSuccessful());

        log.setSuccess(true);
        log.setErrorMessage("Database unavailable");
        assertFalse(log.wasSuccessful());
    }

    private void setEntityId(Object entity, long id) throws Exception {
        Class<?> type = entity.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("id");
                field.setAccessible(true);
                field.set(entity, id);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        fail("Unable to locate id field on entity: " + entity.getClass().getName());
    }
}
