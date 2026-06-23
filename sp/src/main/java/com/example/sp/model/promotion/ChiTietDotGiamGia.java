package com.example.sp.model.promotion;

import com.example.sp.model.ChiTietSanPham;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "chi_tiet_dot_giam_gia")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChiTietDotGiamGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dot_giam_gia_ct")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dot_giam_gia")
    private DotGiamGia dotGiamGia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_spct")
    private ChiTietSanPham chiTietSanPham;

    @Column(name = "so_luong_ton_kho_khuyen_mai")
    private Integer soLuongTonKhoKhuyenMai;

    @Column(name = "gia_tri_giam_toi_thieu")
    private BigDecimal giaTriGiamToiThieu;

    @Column(name = "trang_thai")
    private Boolean trangThai;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @PrePersist
    public void prePersist() {
        if (trangThai == null) {
            trangThai = true;
        }
        ngayTao = LocalDateTime.now();
    }
}
