package com.example.sp.model.sanpham;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "kich_co")
@Data
public class KichCo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_kich_co")
    private Integer idKichCo;
    @Column(name = "ma_kich_co", unique = true)
    private String maKichCo;
    @Column(name = "ten_kich_co", columnDefinition = "NVARCHAR(255)")
    private String tenKichCo;
    @Column(name = "trang_thai")
    private Boolean trangThai = true;
}
