package com.example.sp.model.promotion;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "phieu_giam_gia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhieuGiamGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pgg")
    private Integer id;

    @Column(name = "ma_pgg", unique = true)
    private String maPgg;

    @Column(name = "ten_pgg")
    private String tenPgg;

    @Column(name = "loai_giam")
    private String loaiGiam;

    @Column(name = "gia_tri")
    private BigDecimal giaTri;

    @Column(name = "gia_tri_toi_da")
    private BigDecimal giaTriToiDa;

    @Column(name = "dieu_kien_don_hang")
    private BigDecimal dieuKienDonHang;

    @Column(name = "ngay_bat_dau")
    private LocalDateTime ngayBatDau;

    @Column(name = "ngay_ket_thuc")
    private LocalDateTime ngayKetThuc;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "so_luong_da_dung")
    private Integer soLuongDaDung;

    @Column(name = "trang_thai")
    private Boolean trangThai;

    @Transient
    private String doiTuong;

    @PrePersist
    public void prePersist() {
        if (soLuongDaDung == null) {
            soLuongDaDung = 0;
        }
        if (trangThai == null) {
            trangThai = true;
        }
    }

    public String getDoiTuong() {
        return Objects.requireNonNullElse(doiTuong, "CONG_KHAI");
    }
}
