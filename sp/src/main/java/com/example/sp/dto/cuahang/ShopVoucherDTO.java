package com.example.sp.dto.cuahang;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopVoucherDTO {
    private Integer id;
    private String maPgg;
    private String tenPgg;
    private String displayText;
    private String discountText;
    private String loaiGiam;
    private BigDecimal giaTri;
    private BigDecimal giaTriToiDa;
    private BigDecimal dieuKienDonHang;
    private BigDecimal soTienGiam;
    private BigDecimal canMuaThem;
    private Integer soLuong;
    private Integer soLuongDaDung;
    private Integer soLuongConLai;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    private Boolean applicable;
    private Boolean selected;
    private String reason;
}
