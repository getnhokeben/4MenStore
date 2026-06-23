package com.example.sp.dto.pos;

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
    private String trangThai;
    private LocalDateTime ngayTao;
    private String tenKhachHang;
    private String soDienThoai;
    private String diaChiKhachHang;
    private Integer idKhachHang;
    private String maVoucher;
    private BigDecimal tongTienGoc;
    private BigDecimal soTienGiam;
    private BigDecimal tongTienThanhToan;
    private BigDecimal khachThanhToan;
    private BigDecimal tienThua;
    private List<PosOrderItemDTO> items;
}
