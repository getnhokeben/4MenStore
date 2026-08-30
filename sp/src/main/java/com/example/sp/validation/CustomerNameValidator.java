package com.example.sp.validation;

public final class CustomerNameValidator {

    public static final String PATTERN = "^[\\p{L}]+(?: [\\p{L}]+)+$";
    public static final String INVALID_MESSAGE =
            "Họ tên phải có ít nhất 2 từ, chỉ được chứa chữ cái và khoảng trắng";

    // Thực hiện xử lý nghiệp vụ của hàm customer name validator.
    private CustomerNameValidator() {
    }

    // Thực hiện xử lý nghiệp vụ của hàm normalize.
    public static String normalize(String value) {
        return value == null
                ? null
                : value.trim().replaceAll("\\s+", " ");
    }

    // Kiểm tra điều kiện và tính hợp lệ cho is valid.
    public static boolean isValid(String value) {
        return value != null && value.matches(PATTERN);
    }
}
