package com.example.sp.dto.cuahang;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ShopReviewDTO {
    private Integer id;
    private Integer idSp;
    private String tenSp;
    private String hinhAnh;
    private String tenKhachHang;
    private Integer diemDanhGia;
    private String noiDung;
    private String phanHoiQuanTri;
    private Boolean trangThai;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayPhanHoi;
}
