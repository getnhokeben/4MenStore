package com.example.sp.model.sanpham;

import jakarta.persistence.*;
import lombok.Data;

@Entity @Table(name = "phong_cach_mac") @Data
public class PhongCachMac {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_phong_cach_mac")
    private Integer idPhongCachMac;
    @Column(name = "ma_phong_cach_mac")
    private String maPhongCachMac;
    @Column(name = "ten_phong_cach", columnDefinition = "NVARCHAR(255)")
    private String tenPhongCach;
    @Column(name = "trang_thai")
    private Boolean trangThai = true;
}
