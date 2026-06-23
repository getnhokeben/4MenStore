package com.example.sp.dto.promotion;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PhieuGiamGiaRequest {
    private Integer id;
    private String maPgg;
    private String tenPgg;
    private String loaiGiam;
    private BigDecimal giaTri;
    private BigDecimal giaTriToiDa;
    private BigDecimal dieuKienDonHang;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    private Integer soLuong;
    private Integer soLuongDaDung;
    private Boolean trangThai;
    private List<Integer> khachHangIds;
}
