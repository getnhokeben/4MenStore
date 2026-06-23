package com.example.sp.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity @Table(name = "hinh_anh_san_pham") @Data
public class HinhAnhSanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_hinh_anh")
    private Integer idHinhAnh;

    @Column(name = "id_san_pham")
    private Integer idSanPham;
    @Column(name = "url_anh")
    private String urlAnh;
}