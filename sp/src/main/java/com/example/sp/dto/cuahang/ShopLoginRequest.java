package com.example.sp.dto.cuahang;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShopLoginRequest {
    @NotBlank(message = "Vui long nhap email")
    private String identifier;

    @NotBlank(message = "Vui long nhap mat khau")
    private String matKhau;
}
