
package com.example.sp.controller.xacthuc;
import com.example.sp.dto.nhanvien.DangNhapNhanVienRequest;
import com.example.sp.dto.nhanvien.NhanVienDTO;
import com.example.sp.model.nhanvien.NhanVien;
import com.example.sp.repository.nhanvien.NhanVienRepository;
import com.example.sp.service.nhanvien.EmployeeAccountMailService;
import com.example.sp.service.nhanvien.KhoaSessionNhanVien;
import com.example.sp.service.cuahang.ShopSessionKeys;
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
@RequestMapping("/api/nhanvien/auth")
@RequiredArgsConstructor
public class XacThucNhanVienController {

    private final NhanVienRepository nhanVienRepository;

    // Hai dòng private phải nằm tại đây, không dán vào bên trong hàm.
    private final EmployeeAccountMailService employeeAccountMailService;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    private final SecureRandom secureRandom =
            new SecureRandom();

    @PostMapping("/login")
    // Thực hiện xử lý nghiệp vụ của hàm dang nhap.
    public ResponseEntity<?> dangNhap(
            @Valid @RequestBody DangNhapNhanVienRequest request,
            HttpSession session
    ) {
        String email = request.getEmail() == null
                ? ""
                : request.getEmail().trim();

        String password = request.getMatKhau() == null
                ? ""
                : request.getMatKhau();

        NhanVien employee = nhanVienRepository
                .findByEmailIgnoreCase(email)
                .orElse(null);

        if (employee == null || !Boolean.TRUE.equals(employee.getTrangThai())) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Tài khoản không tồn tại hoặc đã bị khóa"
                    )
            );
        }

        if (!matchesPassword(password, employee.getMatKhau())) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Email hoặc mật khẩu không đúng"
                    )
            );
        }

        // Tài khoản cũ dùng mật khẩu thường sẽ được mã hóa sau khi đăng nhập.
        if (!isBcryptHash(employee.getMatKhau())) {
            employee.setMatKhau(passwordEncoder.encode(password));
            nhanVienRepository.save(employee);
        }

        session.removeAttribute(ShopSessionKeys.CUSTOMER_ID);
        session.setAttribute(
                KhoaSessionNhanVien.NHANVIEN_ID,
                employee.getId()
        );
        session.setAttribute(
                KhoaSessionNhanVien.NHANVIEN_VAITRO,
                employee.getVaiTro()
        );

        return ResponseEntity.ok(toDto(employee));
    }

    @GetMapping("/me")
    // Thực hiện xử lý nghiệp vụ của hàm me.
    public ResponseEntity<?> me(HttpSession session) {
        Integer employeeId = (Integer) session.getAttribute(
                KhoaSessionNhanVien.NHANVIEN_ID
        );

        if (employeeId == null) {
            return ResponseEntity.noContent().build();
        }

        NhanVien employee = nhanVienRepository
                .findById(employeeId)
                .orElse(null);

        if (employee == null || !Boolean.TRUE.equals(employee.getTrangThai())) {
            session.removeAttribute(KhoaSessionNhanVien.NHANVIEN_ID);
            session.removeAttribute(KhoaSessionNhanVien.NHANVIEN_VAITRO);

            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(toDto(employee));
    }

    @PostMapping("/logout")
    // Thực hiện xử lý nghiệp vụ của hàm dang xuat.
    public ResponseEntity<?> dangXuat(HttpSession session) {
        session.invalidate();

        return ResponseEntity.ok(
                Map.of("message", "Đăng xuất thành công")
        );
    }

    public static class ChangePasswordRequest {
        private String matKhauCu;
        private String matKhauMoi;

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

    @PostMapping({"/doi-mat-khau", "/change-password"})
    // Thực hiện xử lý nghiệp vụ của hàm change password.
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request,
            HttpSession session
    ) {
        Integer employeeId = (Integer) session.getAttribute(
                KhoaSessionNhanVien.NHANVIEN_ID
        );

        if (employeeId == null) {
            return ResponseEntity.status(401).body(
                    Map.of("message", "Chưa đăng nhập")
            );
        }

        NhanVien employee = nhanVienRepository
                .findById(employeeId)
                .orElse(null);

        if (employee == null || !Boolean.TRUE.equals(employee.getTrangThai())) {
            session.removeAttribute(KhoaSessionNhanVien.NHANVIEN_ID);
            session.removeAttribute(KhoaSessionNhanVien.NHANVIEN_VAITRO);

            return ResponseEntity.status(401).body(
                    Map.of(
                            "message",
                            "Tài khoản không tồn tại hoặc đã bị khóa"
                    )
            );
        }

        String oldPassword = request.getMatKhauCu() == null
                ? ""
                : request.getMatKhauCu().trim();

        String newPassword = request.getMatKhauMoi() == null
                ? ""
                : request.getMatKhauMoi().trim();

        if (oldPassword.isBlank() || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Vui lòng điền đầy đủ thông tin")
            );
        }

        if (newPassword.length() < 6 || newPassword.length() > 100) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Mật khẩu mới phải có từ 6 đến 100 ký tự")
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

        if (!matchesPassword(oldPassword, employee.getMatKhau())) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Mật khẩu hiện tại không đúng")
            );
        }

        employee.setMatKhau(passwordEncoder.encode(newPassword));
        nhanVienRepository.save(employee);

        return ResponseEntity.ok(
                Map.of("message", "Đổi mật khẩu thành công")
        );
    }

    @PostMapping({"/quen-mat-khau", "/forgot-password"})
    @Transactional(rollbackFor = Exception.class)
    // Thực hiện xử lý nghiệp vụ của hàm forgot password.
    public ResponseEntity<?> forgotPassword(
            @RequestBody ForgotPasswordRequest request
    ) {
        String email = request.getEmail() == null
                ? ""
                : request.getEmail().trim();

        if (email.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Vui lòng nhập email")
            );
        }

        NhanVien employee = nhanVienRepository
                .findByEmailIgnoreCase(email)
                .orElse(null);

        if (employee == null || !Boolean.TRUE.equals(employee.getTrangThai())) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Email không tồn tại hoặc tài khoản đã bị khóa"
                    )
            );
        }

        if (employee.getEmail() == null || employee.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Tài khoản chưa có email để nhận mật khẩu"
                    )
            );
        }

        String temporaryPassword = generateTemporaryPassword();

        employee.setMatKhau(passwordEncoder.encode(temporaryPassword));

        // Lưu trước, nếu gửi mail lỗi sẽ ném lỗi và transaction được rollback.
        nhanVienRepository.saveAndFlush(employee);

        employeeAccountMailService.sendResetPassword(
                employee,
                temporaryPassword
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Mật khẩu tạm thời đã được gửi về email"
                )
        );
    }

    // Thực hiện xử lý nghiệp vụ của hàm to dto.
    private NhanVienDTO toDto(NhanVien employee) {
        NhanVienDTO dto = new NhanVienDTO();

        dto.setId(employee.getId());
        dto.setHoTen(employee.getHoTen());
        dto.setEmail(employee.getEmail());
        dto.setVaiTro(employee.getVaiTro());

        return dto;
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
                value.startsWith("$2a$") ||
                        value.startsWith("$2b$") ||
                        value.startsWith("$2y$")
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

        char[] characters = password.toString().toCharArray();

        for (int i = characters.length - 1; i > 0; i--) {
            int randomIndex = secureRandom.nextInt(i + 1);

            char temporary = characters[i];
            characters[i] = characters[randomIndex];
            characters[randomIndex] = temporary;
        }

        return new String(characters);
    }
}

