package com.example.sp.service.shop;

import com.example.sp.dto.shop.ShopCustomerDTO;
import com.example.sp.dto.shop.ShopLoginRequest;
import com.example.sp.dto.shop.ShopRegisterRequest;
import com.example.sp.model.customer.KhachHang;
import com.example.sp.repository.KhachHangRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class CustomerAuthService {

    private static final String PHONE_PATTERN = "^(03|05|07|08|09)\\d{8}$";
    private static final DateTimeFormatter CODE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final KhachHangRepository khachHangRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public ShopCustomerDTO register(ShopRegisterRequest request) {
        String name = requireText(request.getTenKhachHang(), "Vui lòng nhập họ tên");
        String phone = normalizePhone(request.getSoDienThoai());
        String password = requirePassword(request.getMatKhau());
        String email = normalizeEmail(request.getEmail());
        String username = trimToNull(request.getTenTaiKhoan());
        if (username == null) username = phone;

        if (khachHangRepository.existsBySoDienThoai(phone)) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
        }
        if (email != null && khachHangRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }
        if (khachHangRepository.existsByTenTaiKhoanIgnoreCase(username)) {
            throw new IllegalArgumentException("Tên tài khoản đã được sử dụng");
        }

        KhachHang customer = KhachHang.builder()
                .maKh(generateCustomerCode())
                .tenKhachHang(name)
                .tenTaiKhoan(username)
                .soDienThoai(phone)
                .email(email)
                .diaChiChiTiet(trimToNull(request.getDiaChi()))
                .matKhau(passwordEncoder.encode(password))
                .trangThai(true)
                .build();
        return toDTO(khachHangRepository.save(customer));
    }

    @Transactional
    public ShopCustomerDTO login(ShopLoginRequest request) {
        String identifier = requireText(request.getIdentifier(), "Vui lòng nhập tài khoản");
        String password = requireText(request.getMatKhau(), "Vui lòng nhập mật khẩu");

        KhachHang customer = khachHangRepository.findByLoginIdentifier(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản hoặc mật khẩu không đúng"));
        if (!Boolean.TRUE.equals(customer.getTrangThai())) {
            throw new IllegalArgumentException("Tài khoản đã bị ngừng hoạt động");
        }
        if (!matchesPassword(password, customer.getMatKhau())) {
            throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không đúng");
        }
        if (!isBCryptHash(customer.getMatKhau())) {
            customer.setMatKhau(passwordEncoder.encode(password));
            khachHangRepository.save(customer);
        }
        return toDTO(customer);
    }

    @Transactional(readOnly = true)
    public ShopCustomerDTO getCurrentCustomer(Integer customerId) {
        if (customerId == null) return null;
        return khachHangRepository.findById(customerId)
                .filter(customer -> Boolean.TRUE.equals(customer.getTrangThai()))
                .map(this::toDTO)
                .orElse(null);
    }

    public ShopCustomerDTO toDTO(KhachHang customer) {
        if (customer == null) return null;
        return ShopCustomerDTO.builder()
                .id(customer.getId())
                .maKh(customer.getMaKh())
                .tenKhachHang(customer.getTenKhachHang())
                .tenTaiKhoan(customer.getTenTaiKhoan())
                .soDienThoai(customer.getSoDienThoai())
                .email(customer.getEmail())
                .diaChi(customer.getDiaChi())
                .build();
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) return false;
        if (isBCryptHash(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return storedPassword.equals(rawPassword);
    }

    private boolean isBCryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }

    private String generateCustomerCode() {
        String code;
        do {
            code = "KH" + LocalDateTime.now().format(CODE_FORMAT);
        } while (khachHangRepository.existsByMaKh(code));
        return code;
    }

    private String normalizePhone(String value) {
        String phone = requireText(value, "Vui lòng nhập số điện thoại").replaceAll("\\D", "");
        if (!phone.matches(PHONE_PATTERN)) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng Việt Nam");
        }
        return phone;
    }

    private String normalizeEmail(String value) {
        String email = trimToNull(value);
        if (email == null) return null;
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
        return email;
    }

    private String requirePassword(String value) {
        String password = requireText(value, "Vui lòng nhập mật khẩu");
        if (password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        }
        return password;
    }

    private String requireText(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) throw new IllegalArgumentException(message);
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
