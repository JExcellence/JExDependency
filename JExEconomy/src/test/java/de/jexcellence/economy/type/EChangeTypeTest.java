package de.jexcellence.economy.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EChangeTypeTest {

    @Test
    @DisplayName("each change type exposes the expected identifier and description")
    void changeTypeMetadataMatchesExpectations() {
        Map<EChangeType, String> expectedIdentifiers = new EnumMap<>(EChangeType.class);
        expectedIdentifiers.put(EChangeType.DEPOSIT, "deposit");
        expectedIdentifiers.put(EChangeType.WITHDRAW, "withdraw");
        expectedIdentifiers.put(EChangeType.SET, "set");
        expectedIdentifiers.put(EChangeType.TRANSFER_OUT, "transfer_out");
        expectedIdentifiers.put(EChangeType.TRANSFER_IN, "transfer_in");

        Map<EChangeType, String> expectedDescriptions = new EnumMap<>(EChangeType.class);
        expectedDescriptions.put(EChangeType.DEPOSIT, "Money is being added to the account.");
        expectedDescriptions.put(EChangeType.WITHDRAW, "Money is being removed from the account.");
        expectedDescriptions.put(EChangeType.SET, "The balance is being set to a specific amount.");
        expectedDescriptions.put(EChangeType.TRANSFER_OUT, "The balance is being transferred to another player.");
        expectedDescriptions.put(EChangeType.TRANSFER_IN, "The balance is being received from another player.");

        assertAll("identifier mapping",
                expectedIdentifiers.entrySet().stream()
                        .map(entry -> (Executable) () -> assertEquals(
                                entry.getValue(),
                                entry.getKey().getIdentifier(),
                                () -> "Expected identifier for " + entry.getKey() + " to match"
                        ))
        );

        assertAll("description mapping",
                expectedDescriptions.entrySet().stream()
                        .map(entry -> (Executable) () -> assertEquals(
                                entry.getValue(),
                                entry.getKey().getDescription(),
                                () -> "Expected description for " + entry.getKey() + " to match"
                        ))
        );

        Set<String> identifiers = Arrays.stream(EChangeType.values())
                .map(EChangeType::getIdentifier)
                .collect(Collectors.toSet());

        assertEquals(EChangeType.values().length, identifiers.size(), "Identifiers should be unique across change types");
    }

    @Test
    @DisplayName("findByIdentifier resolves change types regardless of case")
    void findByIdentifierIsCaseInsensitive() {
        assertAll(
                () -> assertEquals(Optional.of(EChangeType.DEPOSIT), EChangeType.findByIdentifier("deposit")),
                () -> assertEquals(Optional.of(EChangeType.WITHDRAW), EChangeType.findByIdentifier("WITHDRAW")),
                () -> assertEquals(Optional.of(EChangeType.SET), EChangeType.findByIdentifier("SeT")),
                () -> assertEquals(Optional.of(EChangeType.TRANSFER_OUT), EChangeType.findByIdentifier("TRANSFER_out")),
                () -> assertEquals(Optional.of(EChangeType.TRANSFER_IN), EChangeType.findByIdentifier("transfer_IN"))
        );
    }

    @Test
    @DisplayName("findByIdentifier returns empty when the identifier is unknown")
    void findByIdentifierReturnsEmptyForUnknownIdentifier() {
        assertFalse(EChangeType.findByIdentifier("unknown").isPresent(), "Unknown identifiers should yield an empty optional");
    }
}
