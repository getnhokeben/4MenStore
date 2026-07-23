package com.example.sp.controller.xacthuc;

import com.example.sp.model.khachhang.KhachHang;
import com.example.sp.model.nhanvien.NhanVien;
import com.example.sp.repository.khachhang.KhachHangRepository;
import com.example.sp.repository.nhanvien.NhanVienRepository;
import com.example.sp.service.cuahang.ShopSessionKeys;
import com.example.sp.service.khachhang.CustomerAccountMailService;
import com.example.sp.service.nhanvien.EmployeeAccountMailService;
import com.example.sp.service.nhanvien.KhoaSessionNhanVien;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UnifiedAuthController {

    private static final String EMPLOYEE_REDIRECT = "/san-pham/trang-chu";
    private static final String CUSTOMER_REDIRECT = "/shop";

    private final NhanVienRepository nhanVienRepository;
    private final KhachHangRepository khachHangRepository;
    private final EmployeeAccountMailService employeeAccountMailService;
    private final CustomerAccountMailService customerAccountMailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public static class LoginRequest {
        private String email;
        private String matKhau;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getMatKhau() {
            return matKhau;
        }

        public void setMatKhau(String matKhau) {
            this.matKhau = matKhau;
        }
    }

    public static class ForgotPasswordRequest {
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpSession session
    ) {
        String email = normalizeEmail(request == null ? null : request.getEmail());
        String password = request == null || request.getMatKhau() == null
                ? ""
                : request.getMatKhau();

        if (email.isBlank() || password.isBlank()) {
            return badRequest("Vui lòng nhập email và mật khẩu.");
        }

        NhanVien employee = nhanVienRepository.findByEmailIgnoreCase(email).orElse(null);
        KhachHang customer = khachHangRepository.findByEmailIgnoreCase(email).orElse(null);

        boolean employeePasswordMatches = employee != null
                && matchesPassword(password, employee.getMatKhau());
        boolean customerPasswordMatches = customer != null
                && matchesPassword(password, customer.getMatKhau());
        boolean employeeMatches = employeePasswordMatches
                && Boolean.TRUE.equals(employee.getTrangThai());
        boolean customerMatches = customerPasswordMatches
                && Boolean.TRUE.equals(customer.getTrangThai());

        if (employeeMatches && customerMatches) {
            return badRequest(
                    "Email này đang được dùng cho cả tài khoản nhân viên và khách hàng. "
                            + "Vui lòng liên hệ quản trị viên để tách tài khoản."
            );
        }

        if (employeeMatches) {
            upgradeEmployeePasswordIfNeeded(employee, password);
            session.removeAttribute(ShopSessionKeys.CUSTOMER_ID);
            session.setAttribute(KhoaSessionNhanVien.NHANVIEN_ID, employee.getId());
            session.setAttribute(KhoaSessionNhanVien.NHANVIEN_VAITRO, employee.getVaiTro());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("accountType", "EMPLOYEE");
            result.put("role", employee.getVaiTro());
            result.put("redirect", EMPLOYEE_REDIRECT);
            return ResponseEntity.ok(result);
        }

        if (customerMatches) {
            upgradeCustomerPasswordIfNeeded(customer, password);
            session.removeAttribute(KhoaSessionNhanVien.NHANVIEN_ID);
            session.removeAttribute(KhoaSessionNhanVien.NHANVIEN_VAITRO);
            session.setAttribute(ShopSessionKeys.CUSTOMER_ID, customer.getId());

            return ResponseEntity.ok(Map.of(
                    "accountType", "CUSTOMER",
                    "role", "Khách hàng",
                    "redirect", CUSTOMER_REDIRECT
            ));
        }

        if (employeePasswordMatches || customerPasswordMatches) {
            return badRequest("Tài khoản đã bị khóa hoặc ngừng hoạt động.");
        }

        return badRequest("Email hoặc mật khẩu không đúng.");
    }

    @PostMapping({"/quen-mat-khau", "/forgot-password"})
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        String email = normalizeEmail(request == null ? null : request.getEmail());
        if (email.isBlank()) {
            return badRequest("Vui lòng nhập email.");
        }

        NhanVien employee = nhanVienRepository.findByEmailIgnoreCase(email)
                .filter(item -> Boolean.TRUE.equals(item.getTrangThai()))
                .orElse(null);
        KhachHang customer = khachHangRepository.findByEmailIgnoreCase(email)
                .filter(item -> Boolean.TRUE.equals(item.getTrangThai()))
                .orElse(null);

        if (employee != null && customer != null) {
            return badRequest(
                    "Email này đang được dùng cho nhiều loại tài khoản. "
                            + "Vui lòng liên hệ quản trị viên để được hỗ trợ."
            );
        }

        String temporaryPassword = generateTemporaryPassword();
        if (employee != null) {
            employee.setMatKhau(passwordEncoder.encode(temporaryPassword));
            nhanVienRepository.saveAndFlush(employee);
            employeeAccountMailService.sendResetPassword(employee, temporaryPassword);
            return resetSuccess();
        }

        if (customer != null) {
            customer.setMatKhau(passwordEncoder.encode(temporaryPassword));
            khachHangRepository.saveAndFlush(customer);
            customerAccountMailService.sendResetPassword(customer, temporaryPassword);
            return resetSuccess();
        }

        return badRequest("Email không tồn tại hoặc tài khoản đã bị khóa.");
    }

    private void upgradeEmployeePasswordIfNeeded(NhanVien employee, String rawPassword) {
        if (!isBcryptHash(employee.getMatKhau())) {
            employee.setMatKhau(passwordEncoder.encode(rawPassword));
            nhanVienRepository.save(employee);
        }
    }

    private void upgradeCustomerPasswordIfNeeded(KhachHang customer, String rawPassword) {
        if (!isBcryptHash(customer.getMatKhau())) {
            customer.setMatKhau(passwordEncoder.encode(rawPassword));
            khachHangRepository.save(customer);
        }
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
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

    private String generateTemporaryPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnopqrstuvwxyz";
        String digits = "23456789";
        String all = upper + lower + digits;

        StringBuilder value = new StringBuilder();
        value.append(randomCharacter(upper));
        value.append(randomCharacter(lower));
        value.append(randomCharacter(digits));
        while (value.length() < 10) {
            value.append(randomCharacter(all));
        }

        for (int index = value.length() - 1; index > 0; index--) {
            int swapIndex = secureRandom.nextInt(index + 1);
            char current = value.charAt(index);
            value.setCharAt(index, value.charAt(swapIndex));
            value.setCharAt(swapIndex, current);
        }
        return value.toString();
    }

    private char randomCharacter(String characters) {
        return characters.charAt(secureRandom.nextInt(characters.length()));
    }

    private String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private ResponseEntity<?> resetSuccess() {
        return ResponseEntity.ok(Map.of(
                "message",
                "Mật khẩu tạm thời đã được gửi về email của tài khoản."
        ));
    }
}
