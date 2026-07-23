package com.example.sp.dto.cuahang;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShopPaymentRequest {

    @NotBlank(message = "Vui lòng chọn cổng thanh toán")
    private String gateway;

    @NotBlank(message = "Thiếu mã đơn hàng")
    private String maHoaDon;
}
