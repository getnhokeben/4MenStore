package com.example.sp.service.tienich;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyRoundingUtilTest {

    @Test
    void roundsDownBelowHalfThousand() {
        assertEquals(new BigDecimal("10000"), MoneyRoundingUtil.round(new BigDecimal("10499")));
    }

    @Test
    void roundsUpFromHalfThousand() {
        assertEquals(new BigDecimal("11000"), MoneyRoundingUtil.round(new BigDecimal("10500")));
    }

    @Test
    void keepsWholeThousandsUnchanged() {
        assertEquals(new BigDecimal("158000"), MoneyRoundingUtil.round(new BigDecimal("158000")));
    }

    @Test
    void treatsNullAsZero() {
        assertEquals(BigDecimal.ZERO, MoneyRoundingUtil.round(null));
    }

    @Test
    void nonNegativeVariantClampsNegativeValues() {
        assertEquals(BigDecimal.ZERO, MoneyRoundingUtil.roundNonNegative(new BigDecimal("-1500")));
    }
}
