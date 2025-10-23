package com.raindropcentral.economy.engine;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BalanceEngineTest {

    @Test
    void applyDepositAddsAmountToBalance() {
        BigDecimal startingBalance = new BigDecimal("125.50");
        BigDecimal deposit = new BigDecimal("24.50");

        BigDecimal result = BalanceEngine.applyDeposit(startingBalance, deposit);

        assertEquals(new BigDecimal("150.00"), result);
    }

    @Test
    void applyDepositAllowsZeroAmount() {
        BigDecimal startingBalance = new BigDecimal("75.25");

        BigDecimal result = BalanceEngine.applyDeposit(startingBalance, BigDecimal.ZERO);

        assertEquals(startingBalance, result);
    }

    @Test
    void applyDepositRejectsNegativeAmounts() {
        BigDecimal startingBalance = BigDecimal.valueOf(50);
        BigDecimal negativeDeposit = BigDecimal.valueOf(-10);

        assertThrows(IllegalArgumentException.class, () -> BalanceEngine.applyDeposit(startingBalance, negativeDeposit));
    }

    @Test
    void applyWithdrawalSubtractsAmountFromBalance() {
        BigDecimal startingBalance = BigDecimal.valueOf(200);
        BigDecimal withdrawal = new BigDecimal("75.25");

        BigDecimal result = BalanceEngine.applyWithdrawal(startingBalance, withdrawal);

        assertEquals(new BigDecimal("124.75"), result);
    }

    @Test
    void applyWithdrawalAllowsZeroAmount() {
        BigDecimal startingBalance = new BigDecimal("99.99");

        BigDecimal result = BalanceEngine.applyWithdrawal(startingBalance, BigDecimal.ZERO);

        assertEquals(startingBalance, result);
    }

    @Test
    void applyWithdrawalRejectsNegativeAmounts() {
        BigDecimal startingBalance = BigDecimal.valueOf(80);
        BigDecimal negativeWithdrawal = BigDecimal.valueOf(-5);

        assertThrows(IllegalArgumentException.class, () -> BalanceEngine.applyWithdrawal(startingBalance, negativeWithdrawal));
    }

    @Test
    void applyWithdrawalRejectsAmountsExceedingBalance() {
        BigDecimal startingBalance = BigDecimal.valueOf(40);
        BigDecimal excessiveWithdrawal = new BigDecimal("40.01");

        assertThrows(IllegalArgumentException.class, () -> BalanceEngine.applyWithdrawal(startingBalance, excessiveWithdrawal));
    }

    @Test
    void calculateNetChangeReturnsPositiveDifference() {
        BigDecimal depositsTotal = new BigDecimal("300.25");
        BigDecimal withdrawalsTotal = new BigDecimal("150.10");

        BigDecimal result = BalanceEngine.calculateNetChange(depositsTotal, withdrawalsTotal);

        assertEquals(new BigDecimal("150.15"), result);
    }

    @Test
    void calculateNetChangeReturnsZeroWhenTotalsMatch() {
        BigDecimal total = new BigDecimal("123.45");

        BigDecimal result = BalanceEngine.calculateNetChange(total, total);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateNetChangeReturnsNegativeDifference() {
        BigDecimal depositsTotal = BigDecimal.valueOf(90);
        BigDecimal withdrawalsTotal = new BigDecimal("135.50");

        BigDecimal result = BalanceEngine.calculateNetChange(depositsTotal, withdrawalsTotal);

        assertEquals(new BigDecimal("-45.50"), result);
    }
}
