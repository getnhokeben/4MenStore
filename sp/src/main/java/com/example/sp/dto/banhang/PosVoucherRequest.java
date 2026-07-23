package com.example.sp.dto.banhang;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PosVoucherRequest {
    @NotBlank(message = "Vui lòng nhập mã giảm giá")
    private String maVoucher;
}
