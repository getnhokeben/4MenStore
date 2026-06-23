package com.example.sp.dto.shop;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShopLoginRequest {
    @NotBlank(message = "Vui lòng nhập email, số điện thoại hoặc tên tài khoản")
    private String identifier;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    private String matKhau;
}
