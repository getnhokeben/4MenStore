package com.example.sp.dto.banhang;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PosVoucherDTO {
    private Integer id;
    private String maVoucher;
    private String tenVoucher;
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
    private boolean applicable;
    private boolean selected;
    private String reason;
}
