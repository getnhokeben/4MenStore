package com.example.sp.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * DTO flatten dữ liệu từ hoa_don_chi_tiet JOIN chi_tiet_san_pham
 * JOIN san_pham, mau_sac, kich_co, kieu_dang, loai_ao, phong_cach_mac
 * → dùng để hiển thị bảng sản phẩm trong trang chi tiết hóa đơn.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoaDonChiTietDTO {

    private Integer idHdct;

    // Từ san_pham
    private String maSanPham;   // san_pham.ma_sp
    private String tenSanPham;  // san_pham.ten_sp

    // Từ các bảng lookup qua chi_tiet_san_pham
    private String mauSac;      // mau_sac.ten_mau_sac
    private String kichCo;      // kich_co.ten_kich_co
    private String kieuDang;    // kieu_dang.ten_kieu_dang
    private String loaiAo;      // loai_ao.ten_loai
    private String phongCach;   // phong_cach_mac.ten_phong_cach

    // Từ hoa_don_chi_tiet
    private Integer soLuong;
    private BigDecimal donGia;
    private BigDecimal thanhTien;
}