/*
 * ShopItemAdminPurchaseCommandTest.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rds.items;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests admin purchase-command behavior on shop items.
 */
class ShopItemAdminPurchaseCommandTest {

    @Test
    void cyclesCommandExecutionModes() {
        assertEquals(ShopItem.CommandExecutionMode.PLAYER, ShopItem.CommandExecutionMode.SERVER.next());
        assertEquals(ShopItem.CommandExecutionMode.SERVER, ShopItem.CommandExecutionMode.PLAYER.next());
    }

    @Test
    void parsesCommandInputWithoutDelay() {
        final ShopItem.AdminPurchaseCommand command = ShopItem.AdminPurchaseCommand.fromInput(
                "broadcast %player_name%",
                ShopItem.CommandExecutionMode.SERVER
        );

        assertEquals("broadcast %player_name%", command.command());
        assertEquals(0L, command.delayTicks());
        assertEquals(ShopItem.CommandExecutionMode.SERVER, command.executionMode());
    }

    @Test
    void parsesCommandInputWithDelay() {
        final ShopItem.AdminPurchaseCommand command = ShopItem.AdminPurchaseCommand.fromInput(
                "40 | eco give %player_name% 100",
                ShopItem.CommandExecutionMode.PLAYER
        );

        assertEquals("eco give %player_name% 100", command.command());
        assertEquals(40L, command.delayTicks());
        assertEquals(ShopItem.CommandExecutionMode.PLAYER, command.executionMode());
    }

    @Test
    void rejectsMalformedDelayedInput() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ShopItem.AdminPurchaseCommand.fromInput(
                        "soon | eco give %player_name% 100",
                        ShopItem.CommandExecutionMode.SERVER
                )
        );
    }

    @Test
    void normalizesNegativeDelayToZero() {
        final ShopItem.AdminPurchaseCommand command = new ShopItem.AdminPurchaseCommand(
                "say hello",
                ShopItem.CommandExecutionMode.SERVER,
                -20L
        );

        assertEquals("say hello", command.command());
        assertEquals(0L, command.delayTicks());
    }

    @Test
    void rejectsBlankCommands() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ShopItem.AdminPurchaseCommand("   ", ShopItem.CommandExecutionMode.SERVER, 0L)
        );
    }
}
