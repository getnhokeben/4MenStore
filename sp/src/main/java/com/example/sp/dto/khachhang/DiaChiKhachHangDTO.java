package com.example.sp.dto.khachhang;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaChiKhachHangDTO {

    private Integer id;
    private String maDiaChi;
    private String tenDiaChi;
    private String tenNguoiNhan;
    private String soDienThoai;
    private String thanhPho;
    private String quan;
    private String phuong;
    private String diaChiCuThe;
    private Boolean macDinh;
    private Boolean trangThai;
    private String diaChiDayDu;
}
