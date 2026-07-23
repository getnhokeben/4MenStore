package com.example.sp.model.sanpham;

import jakarta.persistence.*;
import lombok.Data;


@Entity
@Table(name = "xuat_xu")
@Data
public class XuatXu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_xuat_xu")
    private Integer idXuatXu;
    @Column(name = "ma_xuat_xu")
    private String maXuatXu;
    @Column(name = "ten_xuat_xu", columnDefinition = "NVARCHAR(255)")
    private String tenXuatXu;
    @Column(name = "trang_thai")
    private Boolean trangThai = true;
}
