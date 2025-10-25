package de.jexcellence.economy.database.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserCurrencyTest {

    @Test
    void gettersAndSettersShouldUpdateAndReturnValues() {
        User originalUser = new User(UUID.randomUUID(), "Original");
        Currency originalCurrency = new Currency("coins");
        originalCurrency.setSymbol("¤");

        UserCurrency userCurrency = new UserCurrency(originalUser, originalCurrency, 25.0);

        assertEquals(originalUser, userCurrency.getPlayer());
        assertEquals(originalCurrency, userCurrency.getCurrency());
        assertEquals(25.0, userCurrency.getBalance(), 0.0000001);

        User updatedUser = new User(UUID.randomUUID(), "Updated");
        Currency updatedCurrency = new Currency("gems");
        updatedCurrency.setSymbol("✦");

        userCurrency.setPlayer(updatedUser);
        userCurrency.setCurrency(updatedCurrency);
        userCurrency.setBalance(40.5);

        assertEquals(updatedUser, userCurrency.getPlayer());
        assertEquals(updatedCurrency, userCurrency.getCurrency());
        assertEquals(40.5, userCurrency.getBalance(), 0.0000001);

        assertThrows(IllegalArgumentException.class, () -> userCurrency.setBalance(-1.0));
    }

    @Test
    void balanceMutationHelpersShouldAdjustBalanceAndValidateInput() {
        User user = new User(UUID.randomUUID(), "Depositor");
        Currency currency = new Currency("credits");
        UserCurrency userCurrency = new UserCurrency(user, currency, 100.0);

        assertTrue(userCurrency.deposit(25.5));
        assertEquals(125.5, userCurrency.getBalance(), 0.0000001);

        assertTrue(userCurrency.deposit(0.0));
        assertEquals(125.5, userCurrency.getBalance(), 0.0000001);

        assertThrows(IllegalArgumentException.class, () -> userCurrency.deposit(-5.0));

        assertTrue(userCurrency.withdraw(20.5));
        assertEquals(105.0, userCurrency.getBalance(), 0.0000001);

        assertFalse(userCurrency.withdraw(200.0));
        assertEquals(105.0, userCurrency.getBalance(), 0.0000001);

        assertTrue(userCurrency.withdraw(0.0));
        assertEquals(105.0, userCurrency.getBalance(), 0.0000001);

        assertThrows(IllegalArgumentException.class, () -> userCurrency.withdraw(-3.0));

        assertTrue(userCurrency.hasSufficientBalance(50.0));
        assertFalse(userCurrency.hasSufficientBalance(150.0));
        assertThrows(IllegalArgumentException.class, () -> userCurrency.hasSufficientBalance(-1.0));
    }

    @Test
    void equalityShouldDependOnPlayerAndCurrencyOnly() {
        User sharedUser = new User(UUID.randomUUID(), "Shared");
        Currency sharedCurrency = new Currency("tokens");
        UserCurrency first = new UserCurrency(sharedUser, sharedCurrency, 10.0);
        UserCurrency second = new UserCurrency(sharedUser, sharedCurrency, 50.0);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(first, first);
        assertNotEquals(first, null);

        User differentUser = new User(UUID.randomUUID(), "Other");
        Currency differentCurrency = new Currency("points");

        UserCurrency userVariant = new UserCurrency(differentUser, sharedCurrency, 10.0);
        UserCurrency currencyVariant = new UserCurrency(sharedUser, differentCurrency, 10.0);

        assertNotEquals(first, userVariant);
        assertNotEquals(first, currencyVariant);
    }
}
