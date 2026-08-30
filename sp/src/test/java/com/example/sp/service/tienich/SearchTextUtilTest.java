package com.example.sp.service.tienich;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchTextUtilTest {

    @Test
    void normalizesVietnameseSearchWithoutAccents() {
        assertEquals("giam gia mua he", SearchTextUtil.key("Giảm giá mùa hè"));
        assertTrue(SearchTextUtil.contains("mua he", "Ưu đãi Mùa Hè đặc biệt"));
    }

    @Test
    void repairsUtf8KeywordDecodedAsLatin1() {
        assertEquals("giam", SearchTextUtil.key("Giáº£m"));
        assertEquals("mua he", SearchTextUtil.key("mÃ¹a hÃ¨"));
    }
}
