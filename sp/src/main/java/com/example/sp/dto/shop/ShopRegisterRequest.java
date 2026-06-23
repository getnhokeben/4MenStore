package com.example.sp.dto.shop;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShopRegisterRequest {
    @NotBlank(message = "Vui lòng nhập họ tên")
    private String tenKhachHang;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    private String soDienThoai;

    private String email;
    private String diaChi;
    private String tenTaiKhoan;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    private String matKhau;
}
