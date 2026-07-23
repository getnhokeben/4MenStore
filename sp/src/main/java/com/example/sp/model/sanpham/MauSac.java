package com.example.sp.model.sanpham;
import jakarta.persistence.*;
import lombok.Data;
@Entity
@Table(name = "mau_sac")
@Data
public class MauSac {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mau_sac")
    private Integer idMauSac;
    @Column(name = "ma_mau_sac")
    private String maMauSac;
    @Column(name = "ten_mau_sac", columnDefinition = "NVARCHAR(255)")
    private String tenMauSac;
    @Column(name = "trang_thai")
    private Boolean trangThai = true;
}
