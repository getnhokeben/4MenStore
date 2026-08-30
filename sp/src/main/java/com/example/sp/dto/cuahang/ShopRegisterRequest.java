package com.example.sp.dto.cuahang;

import com.example.sp.validation.CustomerNameValidator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShopRegisterRequest {
    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 255, message = "Họ tên không được vượt quá 255 ký tự")
    @Pattern(
            regexp = CustomerNameValidator.PATTERN,
            message = CustomerNameValidator.INVALID_MESSAGE
    )
    private String tenKhachHang;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(
            regexp = "^(03|05|07|08|09)\\d{8}$",
            message = "Số điện thoại không đúng định dạng Việt Nam"
    )
    private String soDienThoai;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String diaChi;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Size(min = 6, max = 100, message = "Mật khẩu phải có từ 6 đến 100 ký tự")
    private String matKhau;
}
