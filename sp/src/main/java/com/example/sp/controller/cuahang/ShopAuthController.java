package com.example.sp.controller.cuahang;

import com.example.sp.dto.cuahang.ShopCustomerDTO;
import com.example.sp.dto.cuahang.ShopLoginRequest;
import com.example.sp.dto.cuahang.ShopRegisterRequest;
import com.example.sp.model.khachhang.KhachHang;
import com.example.sp.repository.khachhang.KhachHangRepository;
import com.example.sp.service.khachhang.CustomerAccountMailService;
import com.example.sp.service.nhanvien.KhoaSessionNhanVien;
import com.example.sp.service.cuahang.CustomerAuthService;
import com.example.sp.service.cuahang.ShopSessionKeys;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Map;

@RestController
@RequestMapping("/api/shop/auth")
@RequiredArgsConstructor
public class ShopAuthController {

    private final CustomerAuthService customerAuthService;
    private final KhachHangRepository khachHangRepository;
    private final CustomerAccountMailService customerAccountMailService;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    private final SecureRandom secureRandom =
            new SecureRandom();

    @PostMapping("/login")
    // Thực hiện xử lý nghiệp vụ của hàm login.
    public ShopCustomerDTO login(
            @Valid @RequestBody ShopLoginRequest request,
            HttpSession session
    ) {
        ShopCustomerDTO customer = customerAuthService.login(request);

        session.removeAttribute(KhoaSessionNhanVien.NHANVIEN_ID);
        session.removeAttribute(KhoaSessionNhanVien.NHANVIEN_VAITRO);
        session.setAttribute(ShopSessionKeys.CUSTOMER_ID, customer.getId());

        return customer;
    }

    @PostMapping("/register")
    // Thực hiện xử lý nghiệp vụ của hàm register.
    public ShopCustomerDTO register(
            @Valid @RequestBody ShopRegisterRequest request,
            HttpSession session
    ) {
        ShopCustomerDTO customer = customerAuthService.register(request);

        session.removeAttribute(KhoaSessionNhanVien.NHANVIEN_ID);
        session.removeAttribute(KhoaSessionNhanVien.NHANVIEN_VAITRO);
        session.setAttribute(ShopSessionKeys.CUSTOMER_ID, customer.getId());

        return customer;
    }

    @GetMapping("/me")
    // Thực hiện xử lý nghiệp vụ của hàm me.
    public ResponseEntity<ShopCustomerDTO> me(HttpSession session) {
        Integer customerId = (Integer) session.getAttribute(
                ShopSessionKeys.CUSTOMER_ID
        );

        ShopCustomerDTO customer = customerAuthService.getCurrentCustomer(customerId);

        if (customer == null) {
            session.removeAttribute(ShopSessionKeys.CUSTOMER_ID);
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(customer);
    }

    @PostMapping("/logout")
    // Thực hiện xử lý nghiệp vụ của hàm logout.
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();

        return ResponseEntity.ok(
                Map.of("message", "Đăng xuất thành công")
        );
    }

    public static class ChangePasswordRequest {

        @JsonAlias({"oldPassword", "currentPassword"})
        private String matKhauCu;

        @JsonAlias({"newPassword"})
        private String matKhauMoi;

        @JsonAlias({"confirmPassword", "confirmNewPassword"})
        private String xacNhanMatKhau;

        // Tải hoặc truy xuất dữ liệu cho get mat khau cu.
        public String getMatKhauCu() {
            return matKhauCu;
        }

        // Tạo hoặc cập nhật dữ liệu/trạng thái cho set mat khau cu.
        public void setMatKhauCu(String matKhauCu) {
            this.matKhauCu = matKhauCu;
        }

        // Tải hoặc truy xuất dữ liệu cho get mat khau moi.
        public String getMatKhauMoi() {
            return matKhauMoi;
        }

        // Tạo hoặc cập nhật dữ liệu/trạng thái cho set mat khau moi.
        public void setMatKhauMoi(String matKhauMoi) {
            this.matKhauMoi = matKhauMoi;
        }

        // Tải hoặc truy xuất dữ liệu cho get xac nhan mat khau.
        public String getXacNhanMatKhau() {
            return xacNhanMatKhau;
        }

        // Tạo hoặc cập nhật dữ liệu/trạng thái cho set xac nhan mat khau.
        public void setXacNhanMatKhau(String xacNhanMatKhau) {
            this.xacNhanMatKhau = xacNhanMatKhau;
        }
    }

    @RequestMapping(
            value = {"/doi-mat-khau", "/change-password"},
            method = {RequestMethod.POST, RequestMethod.PUT}
    )
    @Transactional
    // Thực hiện xử lý nghiệp vụ của hàm change password.
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request,
            HttpSession session
    ) {
        Integer customerId = (Integer) session.getAttribute(
                ShopSessionKeys.CUSTOMER_ID
        );

        if (customerId == null) {
            return ResponseEntity.status(401).body(
                    Map.of("message", "Bạn chưa đăng nhập")
            );
        }

        KhachHang customer = khachHangRepository
                .findById(customerId)
                .orElse(null);

        if (customer == null || !Boolean.TRUE.equals(customer.getTrangThai())) {
            session.removeAttribute(ShopSessionKeys.CUSTOMER_ID);

            return ResponseEntity.status(401).body(
                    Map.of("message", "Tài khoản không tồn tại hoặc đã bị khóa")
            );
        }

        String oldPassword = request.getMatKhauCu() == null
                ? ""
                : request.getMatKhauCu();

        String newPassword = request.getMatKhauMoi() == null
                ? ""
                : request.getMatKhauMoi();

        String confirmPassword = request.getXacNhanMatKhau() == null
                ? ""
                : request.getXacNhanMatKhau();

        if (oldPassword.isBlank()
                || newPassword.isBlank()
                || confirmPassword.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Vui lòng điền đầy đủ thông tin")
            );
        }

        if (newPassword.length() < 6 || newPassword.length() > 100) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Mật khẩu mới phải có từ 6 đến 100 ký tự")
            );
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Xác nhận mật khẩu mới không khớp")
            );
        }

        if (oldPassword.equals(newPassword)) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Mật khẩu mới không được trùng mật khẩu hiện tại"
                    )
            );
        }

        if (!matchesPassword(oldPassword, customer.getMatKhau())) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Mật khẩu hiện tại không đúng")
            );
        }

        customer.setMatKhau(passwordEncoder.encode(newPassword));
        khachHangRepository.save(customer);

        return ResponseEntity.ok(
                Map.of("message", "Đổi mật khẩu thành công")
        );
    }

    public static class ForgotPasswordRequest {
        private String email;

        // Tải hoặc truy xuất dữ liệu cho get email.
        public String getEmail() {
            return email;
        }

        // Tạo hoặc cập nhật dữ liệu/trạng thái cho set email.
        public void setEmail(String email) {
            this.email = email;
        }
    }

    @PostMapping({"/quen-mat-khau", "/forgot-password"})
    @Transactional(rollbackFor = Exception.class)
    // Thực hiện xử lý nghiệp vụ của hàm forgot password.
    public ResponseEntity<?> forgotPassword(
            @RequestBody ForgotPasswordRequest request
    ) {
        String email = request.getEmail() == null
                ? ""
                : request.getEmail().trim().toLowerCase();

        if (email.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Vui lòng nhập email")
            );
        }

        KhachHang customer = khachHangRepository
                .findByEmailIgnoreCase(email)
                .orElse(null);

        if (customer == null || !Boolean.TRUE.equals(customer.getTrangThai())) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Email không tồn tại hoặc tài khoản đã bị khóa"
                    )
            );
        }

        if (customer.getEmail() == null || customer.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Khách hàng chưa có email nhận mật khẩu")
            );
        }

        String temporaryPassword = generateTemporaryPassword();

        customer.setMatKhau(passwordEncoder.encode(temporaryPassword));
        khachHangRepository.saveAndFlush(customer);

        customerAccountMailService.sendResetPassword(
                customer,
                temporaryPassword
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Mật khẩu tạm thời đã được gửi về email khách hàng"
                )
        );
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

    // Thực hiện xử lý nghiệp vụ của hàm generate temporary password.
    private String generateTemporaryPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnopqrstuvwxyz";
        String digits = "23456789";
        String all = upper + lower + digits;

        StringBuilder password = new StringBuilder();

        password.append(upper.charAt(
                secureRandom.nextInt(upper.length())
        ));

        password.append(lower.charAt(
                secureRandom.nextInt(lower.length())
        ));

        password.append(digits.charAt(
                secureRandom.nextInt(digits.length())
        ));

        while (password.length() < 6) {
            password.append(all.charAt(
                    secureRandom.nextInt(all.length())
            ));
        }

        char[] chars = password.toString().toCharArray();

        for (int i = chars.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);

            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }
}
