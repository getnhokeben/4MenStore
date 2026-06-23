package com.example.sp.dto.promotion;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class DotGiamGiaRequest {
    private Integer id;
    private String maDotGiamGia;
    private String tenDotGiamGia;
    private String loaiGiamGia;
    private BigDecimal giaTriGiamGia;
    private BigDecimal soTienToiDa;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    private Boolean trangThai;
    private List<Integer> selectedSpctIds;
}
