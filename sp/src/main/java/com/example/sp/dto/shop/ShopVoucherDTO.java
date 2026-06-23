package com.example.sp.dto.shop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopVoucherDTO {
    private Integer id;
    private String maPgg;
    private String tenPgg;
    private String loaiGiam;
    private BigDecimal giaTri;
    private BigDecimal giaTriToiDa;
    private BigDecimal dieuKienDonHang;
    private BigDecimal soTienGiam;
}
