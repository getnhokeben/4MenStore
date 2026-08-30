package com.example.sp.model.sanpham;

import com.example.sp.model.khachhang.KhachHang;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "phan_hoi_san_pham", uniqueConstraints = @UniqueConstraint(columnNames = {"id_sp", "id_kh"}))
@Getter
@Setter
@NoArgsConstructor
public class PhanHoiSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_phan_hoi")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sp", nullable = false)
    private SanPham sanPham;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_kh", nullable = false)
    private KhachHang khachHang;

    @Column(name = "diem_danh_gia", nullable = false)
    private Integer diemDanhGia;

    @Column(name = "noi_dung", columnDefinition = "NVARCHAR(1000)")
    private String noiDung;

    @Column(name = "phan_hoi_quan_tri", columnDefinition = "NVARCHAR(1000)")
    private String phanHoiQuanTri;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat", nullable = false)
    private LocalDateTime ngayCapNhat;

    @Column(name = "ngay_phan_hoi")
    private LocalDateTime ngayPhanHoi;

    // Thực hiện xử lý nghiệp vụ của hàm mark updated.
    public void markUpdated() {
        ngayCapNhat = LocalDateTime.now();
    }
}
