package com.example.sp.service.tienich;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;

public final class SearchTextUtil {

    // Thực hiện xử lý nghiệp vụ của hàm search text util.
    private SearchTextUtil() {
    }

    // Thực hiện xử lý nghiệp vụ của hàm key.
    public static String key(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        return Normalizer.normalize(cleaned, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    // Thực hiện xử lý nghiệp vụ của hàm contains.
    public static boolean contains(String searchKey, String... values) {
        if (searchKey == null) {
            return true;
        }
        if (values == null) {
            return false;
        }
        for (String value : values) {
            String valueKey = key(value);
            if (valueKey != null && valueKey.contains(searchKey)) {
                return true;
            }
        }
        return false;
    }

    static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = repairUtf8Mojibake(value).trim().replaceAll("\\s+", " ");
        return cleaned.isEmpty() ? null : Normalizer.normalize(cleaned, Normalizer.Form.NFC);
    }

    // Thực hiện xử lý nghiệp vụ của hàm repair utf8 mojibake.
    private static String repairUtf8Mojibake(String value) {
        if (!looksLikeUtf8Mojibake(value)) {
            return value;
        }
        try {
            String decoded = new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            return decoded.indexOf('\uFFFD') >= 0 ? value : decoded;
        } catch (RuntimeException ignored) {
            return value;
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm looks like utf8 mojibake.
    private static boolean looksLikeUtf8Mojibake(String value) {
        return value.contains("Ã")
                || value.contains("Â")
                || value.contains("Ä")
                || value.contains("Æ")
                || value.contains("áº")
                || value.contains("á»")
                || value.chars().anyMatch(ch -> ch >= 0x80 && ch <= 0x9F);
    }
}
