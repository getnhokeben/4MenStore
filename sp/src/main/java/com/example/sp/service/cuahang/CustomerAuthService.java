package com.example.sp.service.cuahang;

import com.example.sp.dto.cuahang.ShopCustomerDTO;
import com.example.sp.dto.cuahang.ShopLoginRequest;
import com.example.sp.dto.cuahang.ShopRegisterRequest;
import com.example.sp.model.khachhang.KhachHang;
import com.example.sp.repository.khachhang.KhachHangRepository;
import com.example.sp.service.tienich.GeneratedCodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerAuthService {

    private static final String PHONE_PATTERN = "^(03|05|07|08|09)\\d{8}$";
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final KhachHangRepository khachHangRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @Transactional
    public ShopCustomerDTO register(ShopRegisterRequest request) {
        String name = requireText(
                request.getTenKhachHang(),
                "Vui long nhap ho ten"
        );

        String phone = normalizePhone(request.getSoDienThoai());
        String password = requirePassword(request.getMatKhau());
        String email = requireEmail(request.getEmail());

        if (khachHangRepository.existsBySoDienThoai(phone)) {
            throw new IllegalArgumentException("So dien thoai da duoc su dung");
        }

        if (khachHangRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email da duoc su dung");
        }

        KhachHang customer = KhachHang.builder()
                .maKh(generateCustomerCode(name))
                .tenKhachHang(name)
                .tenTaiKhoan(email)
                .soDienThoai(phone)
                .email(email)
                .matKhau(passwordEncoder.encode(password))
                .trangThai(true)
                .build();

        return toDTO(khachHangRepository.save(customer));
    }

    @Transactional
    public ShopCustomerDTO login(ShopLoginRequest request) {
        String email = requireEmail(request.getIdentifier());
        String password = requireText(
                request.getMatKhau(),
                "Vui long nhap mat khau"
        );

        KhachHang customer = khachHangRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Email hoac mat khau khong dung"
                        )
                );

        if (!Boolean.TRUE.equals(customer.getTrangThai())) {
            throw new IllegalArgumentException(
                    "Tai khoan da bi ngung hoat dong"
            );
        }

        if (!matchesPassword(password, customer.getMatKhau())) {
            throw new IllegalArgumentException(
                    "Email hoac mat khau khong dung"
            );
        }

        if (!isBcryptHash(customer.getMatKhau())) {
            customer.setMatKhau(passwordEncoder.encode(password));
            khachHangRepository.save(customer);
        }

        return toDTO(customer);
    }

    @Transactional(readOnly = true)
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

    private boolean isBcryptHash(String value) {
        return value != null && (
                value.startsWith("$2a$")
                        || value.startsWith("$2b$")
                        || value.startsWith("$2y$")
        );
    }

    private String generateCustomerCode(String name) {
        return GeneratedCodeUtil.fromNameAndDate(
                name,
                null,
                "KH",
                khachHangRepository::existsByMaKh
        );
    }

    private String normalizePhone(String value) {
        String phone = requireText(
                value,
                "Vui long nhap so dien thoai"
        ).replaceAll("\\D", "");

        if (!phone.matches(PHONE_PATTERN)) {
            throw new IllegalArgumentException(
                    "So dien thoai khong dung dinh dang Viet Nam"
            );
        }

        return phone;
    }

    private String requireEmail(String value) {
        String email = trimToNull(value);

        if (email == null) {
            throw new IllegalArgumentException("Vui long nhap email");
        }

        String normalized = email.toLowerCase();

        if (!normalized.matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("Email khong hop le");
        }

        return normalized;
    }

    private String requirePassword(String value) {
        String password = requireText(
                value,
                "Vui long nhap mat khau"
        );

        if (password.length() < 6) {
            throw new IllegalArgumentException(
                    "Mat khau phai co toi thieu 6 ky tu"
            );
        }

        return password;
    }

    private String requireText(String value, String message) {
        String trimmed = trimToNull(value);

        if (trimmed == null) {
            throw new IllegalArgumentException(message);
        }

        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }
}
