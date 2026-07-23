package com.example.sp.model.sanpham;

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
    @Column(name = "ten_loai", columnDefinition = "NVARCHAR(255)")
    private String tenLoai;
    @Column(name = "trang_thai")
    private Boolean trangThai = true;
}
