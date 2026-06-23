package com.example.sp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "chi_tiet_san_pham")
@Data
public class ChiTietSanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_spct")
    private Integer idSpct;

    @Column(name = "id_san_pham")
    private Integer idSanPham;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_san_pham", insertable = false, updatable = false)
    private SanPham sanPham;

    @ManyToOne @JoinColumn(name = "id_kich_co")
    private KichCo kichCo;

    @ManyToOne @JoinColumn(name = "id_mau_sac")
    private MauSac mauSac;

    @ManyToOne @JoinColumn(name = "id_loai_ao")
    private LoaiAo loaiAo;

    @ManyToOne
    @JoinColumn(name = "id_phong_cach_mac")
    private PhongCachMac phongCachMac;

    @ManyToOne
    @JoinColumn(name = "id_kieu_dang")
    private KieuDang kieuDang;

    @Column(name = "ma_chi_tiet_san_pham", unique = true)
    private String maChiTietSanPham;

    @Column(name = "so_luong_ton")
    private Integer soLuongTon;

    @Column(name = "don_gia")
    private BigDecimal donGia;

    @Column(name = "trang_thai")
    private Boolean trangThai = true;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Column(name = "nguoi_tao")
    private String nguoiTao;

    @Column(name = "nguoi_cap_nhat")
    private String nguoiCapNhat;
}
