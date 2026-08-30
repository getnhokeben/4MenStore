package com.example.sp.model.khachhang;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dia_chi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaChi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dc")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_kh", nullable = false)
    private KhachHang khachHang;

    @Column(name = "ma_dia_chi", length = 255)
    private String maDiaChi;

    // Nhà riêng, Công ty, Nhà bố mẹ...
    @Column(name = "ten_dia_chi", length = 255)
    private String tenDiaChi;

    @Column(name = "ten_nguoi_nhan", length = 255)
    private String tenNguoiNhan;

    @Column(name = "so_dien_thoai", length = 15)
    private String soDienThoai;

    @Column(name = "thanh_pho", length = 255)
    private String thanhPho;

    @Column(name = "quan", length = 255)
    private String quan;

    @Column(name = "phuong", length = 255)
    private String phuong;

    @Column(name = "dia_chi_cu_the", length = 255)
    private String diaChiCuThe;

    @Column(name = "mac_dinh")
    private Boolean macDinh;

    @Column(name = "trang_thai")
    private Boolean trangThai;

    @PrePersist
    // Thực hiện xử lý nghiệp vụ của hàm pre persist.
    public void prePersist() {
        if (trangThai == null) {
            trangThai = true;
        }

        if (macDinh == null) {
            macDinh = false;
        }
    }
}
