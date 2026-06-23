package com.example.sp.model;

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
    @Column(name = "ten_kieu_dang")
    private String tenKieuDang;
    @Column(name = "trang_thai")
    private Boolean trangThai = true;
}
