package com.example.sp.dto.banhang;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosOrderDTO {
    private Integer id;
    private String maHoaDon;
    private String loaiDon;
    private String hinhThucNhanHang;
    private String trangThai;
    private LocalDateTime ngayTao;
    private String tenKhachHang;
    private String soDienThoai;
    private String diaChiKhachHang;
    private BigDecimal phiVanChuyen;
    private Integer idKhachHang;
    private String maVoucher;
    private BigDecimal tongTienGoc;
    private BigDecimal soTienGiam;
    private BigDecimal tongTienThanhToan;
    private BigDecimal khachThanhToan;
    private BigDecimal tienThua;
    private String tenVoucher;
    private String voucherDisplay;
    private String voucherDiscountText;
    private String voucherHintCode;
    private BigDecimal voucherHintNeedMore;
    private BigDecimal voucherHintDiscount;
    private BigDecimal voucherHintOrderValue;
    private List<PosOrderItemDTO> items;
}
