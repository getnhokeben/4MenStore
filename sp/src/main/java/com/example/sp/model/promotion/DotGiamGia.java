package com.example.sp.model.promotion;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "dot_giam_gia")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DotGiamGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dot_giam_gia")
    private Integer id;

    @Column(name = "ma_dot_giam_gia", unique = true)
    private String maDotGiamGia;

    @Column(name = "ten_dot_giam_gia")
    private String tenDotGiamGia;

    @Column(name = "loai_giam_gia")
    private String loaiGiamGia;

    @Column(name = "gia_tri_giam_gia")
    private BigDecimal giaTriGiamGia;

    @Column(name = "ngay_bat_dau")
    private LocalDateTime ngayBatDau;

    @Column(name = "ngay_ket_thuc")
    private LocalDateTime ngayKetThuc;

    @Column(name = "trang_thai")
    private Boolean trangThai;

    @Column(name = "so_tien_toi_da")
    private BigDecimal soTienToiDa;

    @PrePersist
    public void prePersist() {
        if (loaiGiamGia == null || loaiGiamGia.isBlank()) {
            loaiGiamGia = "PHAN_TRAM";
        }
        if (trangThai == null) {
            trangThai = true;
        }
    }
}
