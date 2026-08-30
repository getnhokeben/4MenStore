package com.example.sp.service.tienich;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Applies the store-wide VND cash rounding rule.
 * Every monetary amount is rounded to the nearest 1,000 VND.
 */
public final class MoneyRoundingUtil {

    public static final BigDecimal ROUNDING_UNIT = new BigDecimal("1000");

    // Thực hiện xử lý nghiệp vụ của hàm money rounding util.
    private MoneyRoundingUtil() {
    }

    // Thực hiện xử lý nghiệp vụ của hàm round.
    public static BigDecimal round(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        return value.divide(ROUNDING_UNIT, 0, RoundingMode.HALF_UP)
                .multiply(ROUNDING_UNIT);
    }

    // Thực hiện xử lý nghiệp vụ của hàm round non negative.
    public static BigDecimal roundNonNegative(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
        return round(safe);
    }
}
