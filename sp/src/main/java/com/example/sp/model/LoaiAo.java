package com.example.sp.model;

import jakarta.persistence.*;
import lombok.Data;
@Entity
@Table(name = "loai_ao")
@Data
public class LoaiAo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_loai_ao")
    private Integer idLoaiAo;
    @Column(name = "ma_loai")
    private String maLoai;
    @Column(name = "ten_loai")
    private String tenLoai;
    @Column(name = "trang_thai")
    private Boolean trangThai = true;
}
