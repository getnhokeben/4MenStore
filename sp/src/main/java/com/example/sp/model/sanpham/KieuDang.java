package com.example.sp.model.sanpham;

import jakarta.persistence.*;
import lombok.Data;
@Entity
@Table(name = "kieu_dang")
@Data
public class KieuDang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_kieu_dang")
    private Integer idKieuDang;
    @Column(name = "ma_kieu_dang")
    private String maKieuDang;
    @Column(name = "ten_kieu_dang", columnDefinition = "NVARCHAR(255)")
    private String tenKieuDang;
    @Column(name = "trang_thai")
    private Boolean trangThai = true;
}
