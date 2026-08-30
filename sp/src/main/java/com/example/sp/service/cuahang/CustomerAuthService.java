package com.example.sp.service.cuahang;

import com.example.sp.dto.cuahang.ShopCustomerDTO;
import com.example.sp.dto.cuahang.ShopLoginRequest;
import com.example.sp.dto.cuahang.ShopRegisterRequest;
import com.example.sp.model.khachhang.KhachHang;
import com.example.sp.repository.khachhang.KhachHangRepository;
import com.example.sp.repository.nhanvien.NhanVienRepository;
import com.example.sp.service.tienich.GeneratedCodeUtil;
import com.example.sp.validation.CustomerNameValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomerAuthService {

    private static final String PHONE_PATTERN = "^(03|05|07|08|09)\\d{8}$";
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @Transactional
    // Thực hiện xử lý nghiệp vụ của hàm register.
    public ShopCustomerDTO register(ShopRegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ thông tin đăng ký");
        }

        String name = requireText(
                CustomerNameValidator.normalize(request.getTenKhachHang()),
                "Vui lòng nhập họ tên"
        );
        String phone = normalizePhone(request.getSoDienThoai());
        String password = requirePassword(request.getMatKhau());
        String email = requireEmail(request.getEmail());
        String address = optionalText(request.getDiaChi(), 255, "Địa chỉ");

        if (name.length() > 255 || !CustomerNameValidator.isValid(name)) {
            throw new IllegalArgumentException(
                    CustomerNameValidator.INVALID_MESSAGE
            );
        }

        if (khachHangRepository.existsBySoDienThoai(phone)) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
        }

        if (khachHangRepository.existsByEmailIgnoreCase(email)
                || nhanVienRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        KhachHang customer = KhachHang.builder()
                .maKh(generateCustomerCode(name))
                .tenKhachHang(name)
                .tenTaiKhoan(email)
                .soDienThoai(phone)
                .email(email)
                .diaChi(address)
                .matKhau(passwordEncoder.encode(password))
                .trangThai(true)
                .build();

        return toDTO(khachHangRepository.save(customer));
    }

    @Transactional
    // Thực hiện xử lý nghiệp vụ của hàm login.
    public ShopCustomerDTO login(ShopLoginRequest request) {
        String email = requireEmail(request.getIdentifier());
        String password = requireText(
                request.getMatKhau(),
                "Vui lòng nhập mật khẩu"
        );

        KhachHang customer = khachHangRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Email hoặc mật khẩu không đúng"
                        )
                );

        if (!Boolean.TRUE.equals(customer.getTrangThai())) {
            throw new IllegalArgumentException(
                    "Tài khoản đã bị ngừng hoạt động"
            );
        }

        if (!matchesPassword(password, customer.getMatKhau())) {
            throw new IllegalArgumentException(
                    "Email hoặc mật khẩu không đúng"
            );
        }

        if (!isBcryptHash(customer.getMatKhau())) {
            customer.setMatKhau(passwordEncoder.encode(password));
            khachHangRepository.save(customer);
        }

        return toDTO(customer);
    }

    @Transactional(readOnly = true)
    // Tải hoặc truy xuất dữ liệu cho get current customer.
    public ShopCustomerDTO getCurrentCustomer(Integer customerId) {
        if (customerId == null) {
            return null;
        }

        return khachHangRepository.findById(customerId)
                .filter(customer ->
                        Boolean.TRUE.equals(customer.getTrangThai())
                )
                .map(this::toDTO)
                .orElse(null);
    }

    // Thực hiện xử lý nghiệp vụ của hàm to dto.
    public ShopCustomerDTO toDTO(KhachHang customer) {
        return ShopCustomerDTO.builder()
                .id(customer.getId())
                .maKh(customer.getMaKh())
                .tenKhachHang(customer.getTenKhachHang())
                .tenTaiKhoan(customer.getEmail())
                .soDienThoai(customer.getSoDienThoai())
                .email(customer.getEmail())
                .diaChi(customer.getDiaChiDisplay())
                .build();
    }

    // Thực hiện xử lý nghiệp vụ của hàm matches password.
    private boolean matchesPassword(
            String rawPassword,
            String storedPassword
    ) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        return isBcryptHash(storedPassword)
                ? passwordEncoder.matches(rawPassword, storedPassword)
                : storedPassword.equals(rawPassword);
    }

    // Kiểm tra điều kiện và tính hợp lệ cho is bcrypt hash.
    private boolean isBcryptHash(String value) {
        return value != null && (
                value.startsWith("$2a$")
                        || value.startsWith("$2b$")
                        || value.startsWith("$2y$")
        );
    }

    // Thực hiện xử lý nghiệp vụ của hàm generate customer code.
    private String generateCustomerCode(String name) {
        return GeneratedCodeUtil.fromNameAndDate(
                name,
                null,
                "KH",
                khachHangRepository::existsByMaKh
        );
    }

    // Thực hiện xử lý nghiệp vụ của hàm normalize phone.
    private String normalizePhone(String value) {
        String phone = requireText(
                value,
                "Vui lòng nhập số điện thoại"
        ).replaceAll("\\D", "");

        if (!phone.matches(PHONE_PATTERN)) {
            throw new IllegalArgumentException(
                    "Số điện thoại không đúng định dạng Việt Nam"
            );
        }

        return phone;
    }

    // Thực hiện xử lý nghiệp vụ của hàm require email.
    private String requireEmail(String value) {
        String email = trimToNull(value);

        if (email == null) {
            throw new IllegalArgumentException("Vui lòng nhập email");
        }

        String normalized = email.toLowerCase(Locale.ROOT);

        if (!normalized.matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }

        return normalized;
    }

    // Thực hiện xử lý nghiệp vụ của hàm require password.
    private String requirePassword(String value) {
        String password = requireText(
                value,
                "Vui lòng nhập mật khẩu"
        );

        if (password.length() < 6 || password.length() > 100) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải có từ 6 đến 100 ký tự"
            );
        }

        return password;
    }

    // Thực hiện xử lý nghiệp vụ của hàm require text.
    private String requireText(String value, String message) {
        String trimmed = trimToNull(value);

        if (trimmed == null) {
            throw new IllegalArgumentException(message);
        }

        return trimmed;
    }

    // Thực hiện xử lý nghiệp vụ của hàm optional text.
    private String optionalText(String value, int maxLength, String label) {
        String trimmed = trimToNull(value);

        if (trimmed != null && trimmed.length() > maxLength) {
            throw new IllegalArgumentException(
                    label + " không được vượt quá " + maxLength + " ký tự"
            );
        }

        return trimmed;
    }

    // Thực hiện xử lý nghiệp vụ của hàm trim to null.
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }
}
