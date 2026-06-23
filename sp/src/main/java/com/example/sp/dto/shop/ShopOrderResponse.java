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
public class ShopOrderResponse {
    private Integer id;
    private String maHoaDon;
    private String trangThai;
    private String phuongThucThanhToan;
    private String invoiceUrl;
    private BigDecimal tongTienGoc;
    private BigDecimal soTienGiam;
    private BigDecimal phiVanChuyen;
    private BigDecimal tongTienThanhToan;
}
