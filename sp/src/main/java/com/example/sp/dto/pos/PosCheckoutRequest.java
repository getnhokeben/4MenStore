package com.example.sp.dto.pos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PosCheckoutRequest {
    @NotNull(message = "Vui lòng nhập số tiền khách thanh toán")
    private BigDecimal khachThanhToan;

    private String phuongThucThanhToan;
}
