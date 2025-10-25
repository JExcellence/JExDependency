package de.jexcellence.economy.command.player.currency;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ECurrencyActionTest {

        @Test
        void fromStringResolvesHelpActionCaseInsensitive() {
                final Optional<ECurrencyAction> lowerCase = ECurrencyAction.fromString("help");
                final Optional<ECurrencyAction> mixedCase = ECurrencyAction.fromString("HeLp");
                final Optional<ECurrencyAction> upperCase = ECurrencyAction.fromString("HELP");
                final Optional<ECurrencyAction> padded = ECurrencyAction.fromString("  help  ");

                assertTrue(lowerCase.isPresent());
                assertSame(ECurrencyAction.HELP, lowerCase.orElseThrow());

                assertTrue(mixedCase.isPresent());
                assertSame(ECurrencyAction.HELP, mixedCase.orElseThrow());

                assertTrue(upperCase.isPresent());
                assertSame(ECurrencyAction.HELP, upperCase.orElseThrow());

                assertTrue(padded.isPresent());
                assertSame(ECurrencyAction.HELP, padded.orElseThrow());
        }

        @Test
        void fromStringReturnsEmptyForNullBlankOrUnknownValues() {
                assertTrue(ECurrencyAction.fromString(null).isEmpty());
                assertTrue(ECurrencyAction.fromString("").isEmpty());
                assertTrue(ECurrencyAction.fromString("   ").isEmpty());
                assertTrue(ECurrencyAction.fromString("unknown").isEmpty());
        }

        @Test
        void actionIdentifiersAreLowercaseAndNonBlank() {
                for (final ECurrencyAction action : ECurrencyAction.values()) {
                        final String identifier = action.getActionName();

                        assertEquals(action.name().toLowerCase(), identifier);
                        assertFalse(identifier.isBlank());
                }
        }

        @Test
        void permissionRequirementsMatchExpectations() {
                for (final ECurrencyAction action : ECurrencyAction.values()) {
                        assertFalse(action.requiresSpecialPermissions(), () -> action + " should not require permissions");
                }
        }

        @Test
        void informationalContractHoldsForAllActions() {
                for (final ECurrencyAction action : ECurrencyAction.values()) {
                        assertTrue(action.isInformationalAction(), () -> action + " should be informational");
                        assertEquals(
                                action.isInformationalAction(),
                                !action.requiresSpecialPermissions(),
                                "Informational actions must not require special permissions"
                        );
                }
        }
}
