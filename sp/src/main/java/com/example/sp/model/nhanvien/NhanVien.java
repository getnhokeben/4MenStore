 package com.example.sp.model.nhanvien;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "nhan_vien")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NhanVien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nv")
    private Integer id;

    @Column(name = "ma_nv", unique = true, length = 50)
    private String maNv;

    @Column(name = "ho_ten", length = 255)
    private String hoTen;

    @Column(name = "so_dien_thoai", length = 20)
    private String soDienThoai;

    @Column(name = "email", length = 255)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "mat_khau", length = 255)
    private String matKhau;

    @Column(name = "cccd", length = 20)
    private String cccd;

    @Column(name = "gioi_tinh", length = 30)
    private String gioiTinh;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    /*
     * Cột địa chỉ cũ: lưu địa chỉ đầy đủ để tương thích dữ liệu cũ.
     */
    @Column(name = "dia_chi", length = 500)
    private String diaChi;

    /*
     * Các cột địa chỉ mới.
     */
    @Column(name = "dia_chi_chi_tiet", length = 255)
    private String diaChiChiTiet;

    @Column(name = "phuong_xa", length = 255)
    private String phuongXa;

    @Column(name = "quan_huyen", length = 255)
    private String quanHuyen;

    @Column(name = "tinh_thanh", length = 255)
    private String tinhThanh;

    @Column(name = "vai_tro", length = 50)
    private String vaiTro;

    @Column(name = "trang_thai")
    private Boolean trangThai;

    @Column(name = "ngay_vao_lam")
    private LocalDate ngayVaoLam;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Transient
    // Tải hoặc truy xuất dữ liệu cho get dia chi display.
    public String getDiaChiDisplay() {
        String diaChiMoi = java.util.stream.Stream.of(
                        diaChiChiTiet,
                        phuongXa,
                        tinhThanh
                )
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.joining(", "));

        return diaChiMoi.isBlank() ? diaChi : diaChiMoi;
    }


    @PrePersist
    // Thực hiện xử lý nghiệp vụ của hàm pre persist.
    public void prePersist() {
        if (trangThai == null) {
            trangThai = true;
        }

        if (ngayTao == null) {
            ngayTao = LocalDateTime.now();
        }

        ngayCapNhat = LocalDateTime.now();
    }

    @PreUpdate
    // Thực hiện xử lý nghiệp vụ của hàm pre update.
    public void preUpdate() {
        ngayCapNhat = LocalDateTime.now();
    }
}
