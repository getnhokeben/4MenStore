package com.example.sp.dto.cuahang;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ShopOrderHistoryDTO {
    private Integer id;
    private String maHoaDon;
    private String loaiDon;
    private String hinhThucNhanHang;
    private String trangThai;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayThanhToan;
    private LocalDateTime ngayCapNhat;
    private String tenKhachHang;
    private String soDienThoai;
    private String diaChiKhachHang;
    private String ghiChu;
    private String phuongThucThanhToan;
    private BigDecimal tongTienGoc;
    private BigDecimal soTienGiam;
    private BigDecimal phiVanChuyen;
    private BigDecimal tongTienThanhToan;
    private String maVoucher;
    private String tenVoucher;
    private String voucherDisplay;
    private String voucherDiscountText;
    private List<ShopOrderHistoryItemDTO> items;
}
