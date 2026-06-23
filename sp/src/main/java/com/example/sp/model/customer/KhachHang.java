package com.example.sp.model.customer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "khach_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_kh")
    private Integer id;

    @Column(name = "ma_kh", unique = true)
    private String maKh;

    @Column(name = "ten_khach_hang")
    private String tenKhachHang;

    @Column(name = "ten_tai_khoan")
    private String tenTaiKhoan;

    @Column(name = "so_dien_thoai")
    private String soDienThoai;

    @Column(name = "email")
    private String email;

    @Column(name = "gioi_tinh")
    private String gioiTinh;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Column(name = "mat_khau")
    private String matKhau;

    @Column(name = "trang_thai")
    private Boolean trangThai;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Column(name = "cccd")
    private String cccd;

    // ===== Các field địa chỉ mới =====
    @Column(name = "tinh_thanh")
    private String tinhThanh;

    @Column(name = "tinh_thanh_code")
    private Integer tinhThanhCode;

    @Column(name = "quan_huyen")
    private String quanHuyen;

    @Column(name = "quan_huyen_code")
    private Integer quanHuyenCode;

    @Column(name = "phuong_xa")
    private String phuongXa;

    @Column(name = "phuong_xa_code")
    private Integer phuongXaCode;

    @Column(name = "dia_chi_chi_tiet")
    private String diaChiChiTiet;

    /**
     * Lấy địa chỉ đầy đủ để hiển thị
     */
    public String getDiaChiDisplay() {
        StringBuilder sb = new StringBuilder();
        if (diaChiChiTiet != null && !diaChiChiTiet.isEmpty()) {
            sb.append(diaChiChiTiet);
        }
        if (phuongXa != null && !phuongXa.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(phuongXa);
        }
        if (quanHuyen != null && !quanHuyen.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(quanHuyen);
        }
        if (tinhThanh != null && !tinhThanh.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(tinhThanh);
        }
        return sb.toString();
    }

    /**
     * Setter để tương thích với code cũ
     * Lưu địa chỉ vào field diaChiChiTiet
     */
    public void setDiaChi(String diaChi) {
        this.diaChiChiTiet = diaChi;
    }

    /**
     * Getter để tương thích với code cũ
     * Trả về địa chỉ đầy đủ
     */
    public String getDiaChi() {
        return getDiaChiDisplay();
    }

    @PrePersist
    public void prePersist() {
        if (trangThai == null) {
            trangThai = true;
        }
        ngayTao = LocalDateTime.now();
        ngayCapNhat = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        ngayCapNhat = LocalDateTime.now();
    }
}