package com.example.sp.dto.nhanvien;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DangNhapNhanVienRequest {
    @NotBlank(message = "Vui lòng nhập email")
    private String email;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    private String matKhau;
}
