package com.example.sp.model.order;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "phuong_thuc_thanh_toan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhuongThucThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pttt")
    private Integer id;

    @Column(name = "ma_pttt")
    private String maPttt;

    @Column(name = "ten_pttt")
    private String tenPttt;

    @Column(name = "trang_thai")
    private Boolean trangThai;
}