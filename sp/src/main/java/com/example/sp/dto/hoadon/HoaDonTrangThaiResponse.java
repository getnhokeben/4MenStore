package com.example.sp.dto.hoadon;

import com.example.sp.model.hoadon.HoaDon;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class HoaDonTrangThaiResponse {

    private Integer id;
    private String maHoaDon;
    private String trangThai;
    private Boolean daGiuTon;
    private Boolean daTruTon;
    private Boolean daHoanTon;
    private String lyDoHoanHang;
    private String ghiChuHoanHang;
    private LocalDateTime ngayThanhToan;
    private LocalDateTime ngayCapNhat;
    private LocalDateTime ngayYeuCauHoan;
    private LocalDateTime ngayNhanHangHoan;

    // Thực hiện xử lý nghiệp vụ của hàm from.
    public static HoaDonTrangThaiResponse from(HoaDon hoaDon) {
        if (hoaDon == null) {
            throw new IllegalArgumentException("Không có dữ liệu hóa đơn");
        }
        return HoaDonTrangThaiResponse.builder()
                .id(hoaDon.getId())
                .maHoaDon(hoaDon.getMaHoaDon())
                .trangThai(hoaDon.getTrangThai())
                .daGiuTon(hoaDon.getDaGiuTon())
                .daTruTon(hoaDon.getDaTruTon())
                .daHoanTon(hoaDon.getDaHoanTon())
                .lyDoHoanHang(hoaDon.getLyDoHoanHang())
                .ghiChuHoanHang(hoaDon.getGhiChuHoanHang())
                .ngayThanhToan(hoaDon.getNgayThanhToan())
                .ngayCapNhat(hoaDon.getNgayCapNhat())
                .ngayYeuCauHoan(hoaDon.getNgayYeuCauHoan())
                .ngayNhanHangHoan(hoaDon.getNgayNhanHangHoan())
                .build();
    }
}
