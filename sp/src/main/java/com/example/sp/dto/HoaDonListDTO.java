package com.example.sp.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoaDonListDTO {

    private Integer id;
    private String maHoaDon;
    private String tenKhachHang;
    private String tenNhanVien;
    private BigDecimal tongTienThanhToan;
    private String loaiDon;
    private LocalDateTime ngayTao;
    private String trangThai;
}