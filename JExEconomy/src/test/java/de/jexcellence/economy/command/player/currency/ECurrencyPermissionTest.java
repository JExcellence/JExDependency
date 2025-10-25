package de.jexcellence.economy.command.player.currency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ECurrencyPermissionTest {

    @Test
    @DisplayName("each currency permission exposes its configured metadata")
    void metadataMatchesEnumConfiguration() {
        Map<ECurrencyPermission, String> expectedInternalNames = new EnumMap<>(ECurrencyPermission.class);
        expectedInternalNames.put(ECurrencyPermission.CURRENCY, "command");
        expectedInternalNames.put(ECurrencyPermission.CURRENCY_OTHER, "commandOther");

        Map<ECurrencyPermission, String> expectedFallbackNodes = new EnumMap<>(ECurrencyPermission.class);
        expectedFallbackNodes.put(ECurrencyPermission.CURRENCY, "currency.command");
        expectedFallbackNodes.put(ECurrencyPermission.CURRENCY_OTHER, "currency.command.other");

        for (ECurrencyPermission permission : ECurrencyPermission.values()) {
            assertEquals(
                    expectedInternalNames.get(permission),
                    permission.getInternalName(),
                    "Internal identifier should align with the enum declaration"
            );
            assertEquals(
                    expectedFallbackNodes.get(permission),
                    permission.getFallbackNode(),
                    "Fallback node should align with the enum declaration"
            );
        }
    }

    @Test
    @DisplayName("static lookup helpers resolve permissions by internal name")
    void fromInternalNameResolvesPermissions() {
        Optional<ECurrencyPermission> resolved = ECurrencyPermission.fromInternalName(" command ");

        assertTrue(resolved.isPresent(), "Trimmed input should resolve to a currency permission");
        assertSame(ECurrencyPermission.CURRENCY, resolved.get(), "Expected the base currency permission to be resolved");

        assertFalse(
                ECurrencyPermission.fromInternalName(null).isPresent(),
                "Null input should not resolve to a permission"
        );
        assertFalse(
                ECurrencyPermission.fromInternalName("   ").isPresent(),
                "Blank input should not resolve to a permission"
        );
    }

    @Test
    @DisplayName("static lookup helpers resolve permissions by fallback node")
    void fromFallbackNodeResolvesPermissions() {
        Optional<ECurrencyPermission> resolved = ECurrencyPermission.fromFallbackNode(" currency.command.other ");

        assertTrue(resolved.isPresent(), "Trimmed fallback input should resolve to a currency permission");
        assertSame(
                ECurrencyPermission.CURRENCY_OTHER,
                resolved.get(),
                "Expected the cross-player currency permission to be resolved"
        );

        assertFalse(
                ECurrencyPermission.fromFallbackNode(null).isPresent(),
                "Null fallback node should not resolve to a permission"
        );
        assertFalse(
                ECurrencyPermission.fromFallbackNode(" ").isPresent(),
                "Blank fallback node should not resolve to a permission"
        );
    }

    @Test
    @DisplayName("helper methods describe the default grant recommendations")
    void helperMethodsReflectDefaultGrantGuidance() {
        assertTrue(
                ECurrencyPermission.CURRENCY.isSuitableForGeneralPlayers(),
                "Base currency permission should be available to general players"
        );
        assertFalse(
                ECurrencyPermission.CURRENCY.allowsCrossPlayerAccess(),
                "Base currency permission should not allow cross-player access"
        );

        assertTrue(
                ECurrencyPermission.CURRENCY_OTHER.allowsCrossPlayerAccess(),
                "Cross-player permission should be flagged as accessing other players"
        );
        assertFalse(
                ECurrencyPermission.CURRENCY_OTHER.isSuitableForGeneralPlayers(),
                "Cross-player permission should not be granted to general players by default"
        );
    }
}
