package com.example.sp.service.tienich;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Predicate;

public final class GeneratedCodeUtil {

    private static final int MAX_CODE_LENGTH = 15;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyyyy");

    private GeneratedCodeUtil() {
    }

    public static String fromNameAndDate(String name, LocalDateTime createdAt, String fallbackName, Predicate<String> exists) {
        String base = normalizeCodePart(name);
        if (base.isBlank()) {
            base = normalizeCodePart(fallbackName);
        }
        if (base.isBlank()) {
            base = "MA";
        }

        String datePart = (createdAt == null ? LocalDateTime.now() : createdAt).format(DATE_FORMAT);
        for (int sequence = 0; sequence < 1000; sequence++) {
            String suffix = sequence == 0 ? "" : String.valueOf(sequence);
            int baseLength = Math.max(1, MAX_CODE_LENGTH - datePart.length() - suffix.length());
            String code = base.substring(0, Math.min(base.length(), baseLength)) + suffix + datePart;
            if (exists == null || !exists.test(code)) {
                return code;
            }
        }

        throw new IllegalStateException("Khong the tao ma duy nhat");
    }

    private static String normalizeCodePart(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);
        return normalized;
    }
}
