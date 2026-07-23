package com.example.sp.dto.banhang;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PosCheckoutRequest {
    @NotNull(message = "Vui long nhap so tien khach thanh toan")
    private BigDecimal khachThanhToan;

    private String phuongThucThanhToan;

    private BigDecimal tienMat;

    private BigDecimal chuyenKhoan;
}
